import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    private static final int PORT = 4221;
    private static final String CRLF = "\r\n";
    private static final String RESPONSE_200 = "HTTP/1.1 200 OK\r\n";
    private static final String RESPONSE_404 = "HTTP/1.1 404 Not Found\r\n";
    private static final String RESPONSE_201 = "HTTP/1.1 201 Created\r\n";
    private static final Pattern ENDPOINT_PATTERN = Pattern.compile("^(GET|POST)\\s+(\\S+)\\s+HTTP/1\\.1", Pattern.MULTILINE);
    private static final Pattern USER_AGENT_HEADER_PATTERN = Pattern.compile("(?i)^User-Agent:\\s*(.+)$", Pattern.MULTILINE);
    private static final Pattern REQUEST_BODY_PATTERN = Pattern.compile("\\r\\n\\r\\n(.*)", Pattern.DOTALL);

    private static String FILES_FOLDER = "/tmp/";

    private static final ExecutorService threadPool = Executors.newFixedThreadPool(10);

    private record HttpRequestLine(String method, String endpoint) {
    }

    private record HttpRequestData(String headers, String body) {
    }

    public static void main(String[] args) {
        if (args.length > 1) {
            FILES_FOLDER = args[1];
        }

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            // Avoid "Address already in use" errors when restarting
            serverSocket.setReuseAddress(true);
            System.out.println("Server is listening on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());

                threadPool.execute(() -> handleClient(clientSocket));
            }

        } catch (IOException e) {
            System.err.println("Server exception: " + e.getMessage());
        }
    }

    private static void handleClient(Socket socket) {
        System.out.println("Accepted connection from " + socket.getInetAddress());

        try (
                InputStream inputStream = socket.getInputStream();
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        ) {
            HttpRequestData requestData = readHttpRequest(inputStream);
            String response = buildHttpResponse(requestData);
            out.print(response);
            out.flush();
            System.out.println("Response sent to client.");

        } catch (IOException e) {
            System.err.println("Client handling exception: " + e.getMessage());
        }

        System.out.println("Client connection closed.");
    }

    private static HttpRequestData readHttpRequest(InputStream inputStream) throws IOException {
        // Read headers
        ByteArrayOutputStream headerBuffer = new ByteArrayOutputStream();
        int prev = 0, curr;
        while ((curr = inputStream.read()) != -1) {
            headerBuffer.write(curr);
            if (prev == '\r' && curr == '\n') {
                byte[] temp = headerBuffer.toByteArray();
                if (endsWithCRLFCRLF(temp)) {
                    break;
                }
            }
            prev = curr;
        }
        String headersString = headerBuffer.toString(StandardCharsets.UTF_8);
        System.out.println("Headers:\n" + headersString);

        // Parse headers
        String[] headerLines = headersString.split("\r\n");
        String requestLine = headerLines[0];
        System.out.println("Request Line: " + requestLine);

        int contentLength = 0;
        for (int i = 1; i < headerLines.length; i++) {
            String line = headerLines[i];
            if (line.toLowerCase().startsWith("content-length:")) {
                contentLength = Integer.parseInt(line.substring("content-length:".length()).trim());
            }
        }

        // Read body
        byte[] body = new byte[contentLength];
        int bytesRead = 0;
        while (bytesRead < contentLength) {
            int read = inputStream.read(body, bytesRead, contentLength - bytesRead);
            if (read == -1) {
                break;
            }
            bytesRead += read;
        }
        String requestBody = new String(body, StandardCharsets.UTF_8);
        System.out.println("Body:\n" + requestBody);

        return new HttpRequestData(headersString, requestBody);
    }

    private static boolean endsWithCRLFCRLF(byte[] data) {
        int len = data.length;
        if (len < 4) return false;
        return data[len - 4] == '\r' && data[len - 3] == '\n' &&
                data[len - 2] == '\r' && data[len - 1] == '\n';
    }

    private static String buildHttpResponse(HttpRequestData requestData) {
        HttpRequestLine httpRequestLine = extractRequestLine(requestData.headers);
        if(httpRequestLine != null) {
            System.out.println("Endpoint:\n" + httpRequestLine.endpoint);
            System.out.println("Method:\n" + httpRequestLine.method);
        }

        if ("GET".equals(httpRequestLine.method)) {
            String endpoint = httpRequestLine.endpoint;
            if ("/".equals(endpoint)) {
                return RESPONSE_200 + CRLF;
            } else if (endpoint != null && endpoint.startsWith("/echo/")) {
                String msg = endpoint.substring(endpoint.indexOf('/', 1) + 1);
                return buildTextResponse(msg);
            } else if (endpoint != null && endpoint.startsWith("/files/")) {
                String fileName = endpoint.substring(endpoint.indexOf('/', 1) + 1);
                String filePath = FILES_FOLDER + fileName;
                Path path = Paths.get(filePath);

                if (Files.exists(path)) {
                    try {
                        byte[] fileBytes = Files.readAllBytes(path);
                        return buildFileResponse(fileBytes);
                    } catch (IOException e) {
                        System.err.println("Error reading the file: " + e.getMessage());
                    }
                } else {
                    System.out.println("File does not exist: " + filePath);
                }

                return RESPONSE_404 + CRLF;
            } else if ("/user-agent".equals(endpoint)) {
                String userAgent = extractUserAgentHeader(requestData.headers);
                return buildTextResponse(userAgent != null ? userAgent : "");
            } else {
                return RESPONSE_404 + CRLF;
            }
        } else if ("POST".equals(httpRequestLine.method)) {
            String endpoint = httpRequestLine.endpoint;
            if (endpoint != null && endpoint.startsWith("/files/")) {
                String fileName = endpoint.substring(endpoint.indexOf('/', 1) + 1);
                String filePath = FILES_FOLDER + fileName;
                if (requestData.body != null && !requestData.body.isEmpty()) {
                    Path path = Paths.get(filePath);
                    try {
                        Files.write(path, requestData.body.getBytes());
                        System.out.println("Request body written to file successfully.");
                    } catch (IOException e) {
                        System.err.println("Error writing to file: " + e.getMessage());
                    }
                    return RESPONSE_201 + CRLF;
                }
            }
        }

        return RESPONSE_404 + CRLF;
    }

    private static String buildTextResponse(String msg) {
        String headers = String.join(CRLF,
                "Content-Type: text/plain",
                "Content-Length: " + msg.length()
        ) + CRLF + CRLF;
        return RESPONSE_200 + headers + msg;
    }

    private static String buildFileResponse(byte[] fileContent) {
        String headers = String.join(CRLF,
                "Content-Type: application/octet-stream",
                "Content-Length: " + fileContent.length
        ) + CRLF + CRLF;
        return RESPONSE_200 + headers + new String(fileContent);
    }

    private static HttpRequestLine extractRequestLine(String request) {
        if (request == null || request.isEmpty()) return null;

        Matcher matcher = ENDPOINT_PATTERN.matcher(request);
        if (matcher.find()) {
            String method = matcher.group(1);
            String endpoint = matcher.group(2);
            return new HttpRequestLine(method, endpoint);
        }

        return null;
    }

    private static String extractUserAgentHeader(String request) {
        if (request == null || request.isEmpty()) return null;

        Matcher matcher = USER_AGENT_HEADER_PATTERN.matcher(request);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    private static String extractRequestBody(String request) {
        if (request == null || request.isEmpty()) return null;

        Matcher matcher = REQUEST_BODY_PATTERN.matcher(request);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }
}
