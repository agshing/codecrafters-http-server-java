import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {

    private static final int PORT = 4221;
    private static final String RESPONSE = "HTTP/1.1 200 OK\r\n\r\n";

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

        try (Socket socket = clientSocket; PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            out.print(RESPONSE);
            out.flush();
            System.out.println("Response sent to client.");

        } catch (IOException e) {
            System.err.println("Client handling exception: " + e.getMessage());
        }

        System.out.println("Client connection closed.");
    }
}
