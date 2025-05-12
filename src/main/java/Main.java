import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(4221);

            // Since the tester restarts your program quite often, setting SO_REUSEADDR
            // ensures that we don't run into 'Address already in use' errors
            serverSocket.setReuseAddress(true);

            Socket clientSocket = serverSocket.accept();
            System.out.println("Accepted new connection");
            System.out.println("Client connected: " + clientSocket.getInetAddress());
            // BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            String response = "HTTP/1.1 200 OK\\r\\n\\r\\n\n";
            out.println(response);

            // Close connection
            clientSocket.close();
            System.out.println("Client connection closed.");

        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }
}
