import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Main {

    private static final int PORT = 4221;
    private static final String FILES_FOLDER_DEFAULT = "/tmp/";
    private static String filesFolder = FILES_FOLDER_DEFAULT;

    public static void main(String[] args) {
        if (args.length > 1) {
            filesFolder = args[1];
        }

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            serverSocket.setReuseAddress(true);
            System.out.println("Server is listening on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                Thread.startVirtualThread(() -> handleClient(clientSocket));
            }

        } catch (IOException e) {
            System.err.println("Server exception: " + e.getMessage());
        }
    }

    private static void handleClient(Socket socket) {
        try (
                InputStream inputStream = socket.getInputStream();
                OutputStream outputStream = socket.getOutputStream()
        ) {
            HttpRequest request = HttpRequestParser.parse(inputStream);
            HttpResponse response = HttpRequestHandler.handle(request, filesFolder);
            outputStream.write(response.txtBody().getBytes(StandardCharsets.UTF_8));
            if(response.byteBody() != null) {
                outputStream.write(response.byteBody());
            }
            outputStream.flush();

        } catch (IOException e) {
            System.err.println("Client handling exception: " + e.getMessage());
        }
    }
}