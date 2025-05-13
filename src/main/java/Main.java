import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    private static final int PORT = 4221;
    private static final String CRLF = "\r\n";
    private static final String RESPONSE_200 = "HTTP/1.1 200 OK\r\n";
    private static final String RESPONSE_404 = "HTTP/1.1 404 Not Found\r\n";
    private static final Pattern ENDPOINT_PATTERN = Pattern.compile("^GET\\s+(\\S+)\\s+HTTP/1\\.1", Pattern.MULTILINE);
    private static final Pattern USER_AGENT_HEADER_PATTERN = Pattern.compile("(?i)^User-Agent:\\s*(.+)$", Pattern.MULTILINE);


    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            // Avoid "Address already in use" errors when restarting
            serverSocket.setReuseAddress(true);
            System.out.println("Server is listening on port " + PORT);

            while (true) {
                handleClient(serverSocket.accept());
            }

        } catch (IOException e) {
            System.err.println("Server exception: " + e.getMessage());
        }
    }

    private static void handleClient(Socket clientSocket) {
        System.out.println("Accepted connection from " + clientSocket.getInetAddress());

        try (
                Socket socket = clientSocket;
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {
            String request = readHttpRequest(in);
            System.out.println("Received request:\n" + request);

            String endpoint = extractGetEndpoint(request);
            System.out.println("Endpoint:\n" + endpoint);

            String response = buildHttpResponse(request, endpoint);
            out.print(response);
            out.flush();
            System.out.println("Response sent to client.");

        } catch (IOException e) {
            System.err.println("Client handling exception: " + e.getMessage());
        }

        System.out.println("Client connection closed.");
    }

    private static String readHttpRequest(BufferedReader in) throws IOException {
        StringBuilder requestBuilder = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null && !line.isEmpty()) {
            requestBuilder.append(line).append("\r\n");
        }
        return requestBuilder.toString();
    }

    private static String buildHttpResponse(String request, String endpoint) {
        if ("/".equals(endpoint)) {
            return RESPONSE_200 + CRLF;
        } else if (endpoint != null && endpoint.startsWith("/echo/")) {
            String msg = endpoint.substring(endpoint.indexOf('/', 1) + 1);
            return buildTextResponse(msg);
        } else if ("/user-agent".equals(endpoint)) {
            String userAgent = extractUserAgentHeader(request);
            return buildTextResponse(userAgent != null ? userAgent : "");
        } else {
            return RESPONSE_404 + CRLF;
        }
    }

    private static String buildTextResponse(String msg) {
        String headers = String.join(CRLF,
                "Content-Type: text/plain",
                "Content-Length: " + msg.length()
        ) + CRLF + CRLF;
        return RESPONSE_200 + headers + msg;
    }

    private static String extractGetEndpoint(String request) {
        if (request == null || request.isEmpty()) return null;

        Matcher matcher = ENDPOINT_PATTERN.matcher(request);

        if (matcher.find()) {
            return matcher.group(1);
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
}
