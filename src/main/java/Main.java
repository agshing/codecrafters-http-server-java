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
    private static final String RESPONSE_200 = "HTTP/1.1 200 OK\r\n\r\n";
    private static final String RESPONSE_404 = "HTTP/1.1 404 Not Found\r\n\r\n";
    private static final Pattern ENDPOINT_PATTERN = Pattern.compile("^GET\\s+(\\S+)\\s+HTTP/1\\.1", Pattern.MULTILINE);

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
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        ) {
            // Read the full HTTP request into a StringBuilder
            StringBuilder requestBuilder = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null && !line.isEmpty()) {
                requestBuilder.append(line).append("\r\n");
            }
            String request = requestBuilder.toString();
            System.out.println("Received request:\n" + request);

            String endpoint = extractGetEndpoint(request);
            if ("/".equals(endpoint)) {
                out.print(RESPONSE_200);
            } else {
                out.print(RESPONSE_404);
            }

            out.flush();
            System.out.println("Response sent to client.");

        } catch (IOException e) {
            System.err.println("Client handling exception: " + e.getMessage());
        }

        System.out.println("Client connection closed.");
    }

    private static String extractGetEndpoint(String request) {
        if (request == null || request.isEmpty()) return null;

        Matcher matcher = ENDPOINT_PATTERN.matcher(request);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }
}
