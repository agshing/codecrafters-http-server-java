import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

class HttpRequestHandler {
    public static String handle(HttpRequest request, String filesFolder) {
        switch (request.method()) {
            case "GET" -> {
                return handleGet(request, filesFolder);
            }
            case "POST" -> {
                return handlePost(request, filesFolder);
            }
            default -> {
                return HttpConstants.RESPONSE_404 + HttpConstants.CRLF;
            }
        }
    }

    private static String handleGet(HttpRequest request, String filesFolder) {
        String endpoint = request.endpoint();
        if ("/".equals(endpoint)) {
            return HttpConstants.RESPONSE_200 + HttpConstants.CRLF;
        } else if (endpoint.startsWith("/echo/")) {
            String msg = endpoint.substring("/echo/".length());
            String encoding = extractHeader(request.headers(), "Accept-Encoding");
            return buildTextResponse(msg, encoding);
        } else if (endpoint.startsWith("/files/")) {
            String fileName = endpoint.substring("/files/".length());
            Path path = Paths.get(filesFolder, fileName);
            if (Files.exists(path)) {
                try {
                    byte[] content = Files.readAllBytes(path);
                    return buildFileResponse(content);
                } catch (IOException e) {
                    return HttpConstants.RESPONSE_404 + HttpConstants.CRLF;
                }
            } else {
                return HttpConstants.RESPONSE_404 + HttpConstants.CRLF;
            }
        } else if ("/user-agent".equals(endpoint)) {
            String userAgent = extractHeader(request.headers(), "User-Agent");
            return buildTextResponse(userAgent != null ? userAgent : "", null);
        } else {
            return HttpConstants.RESPONSE_404 + HttpConstants.CRLF;
        }
    }

    private static String handlePost(HttpRequest request, String filesFolder) {
        String endpoint = request.endpoint();
        if (endpoint.startsWith("/files/")) {
            String fileName = endpoint.substring("/files/".length());
            Path path = Paths.get(filesFolder, fileName);
            try {
                Files.write(path, request.body().getBytes(StandardCharsets.UTF_8));
                return HttpConstants.RESPONSE_201 + HttpConstants.CRLF;
            } catch (IOException e) {
                return HttpConstants.RESPONSE_404 + HttpConstants.CRLF;
            }
        }
        return HttpConstants.RESPONSE_404 + HttpConstants.CRLF;
    }

    private static String extractHeader(String headers, String key) {
        for (String line : headers.split("\r\n")) {
            if (line.toLowerCase().startsWith(key.toLowerCase() + ":")) {
                return line.split(":", 2)[1].trim();
            }
        }
        return null;
    }

    private static String buildTextResponse(String msg, String encoding) {
        String headers = "Content-Type: text/plain" + HttpConstants.CRLF +
                "Content-Length: " + msg.length() + HttpConstants.CRLF;
        if("gzip".equals(encoding)) {
            headers += "Content-Encoding: gzip" + HttpConstants.CRLF;
        }
        headers += HttpConstants.CRLF;

        return HttpConstants.RESPONSE_200 + headers + msg;
    }

    private static String buildFileResponse(byte[] content) {
        String headers = "Content-Type: application/octet-stream" + HttpConstants.CRLF +
                "Content-Length: " + content.length + HttpConstants.CRLF + HttpConstants.CRLF;
        return HttpConstants.RESPONSE_200 + headers + new String(content, StandardCharsets.UTF_8);
    }
}
