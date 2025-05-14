import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

class HttpRequestHandler {
    public static HttpResponse handle(HttpRequest request, String filesFolder) {
        switch (request.method()) {
            case "GET" -> {
                return handleGet(request, filesFolder);
            }
            case "POST" -> {
                return handlePost(request, filesFolder);
            }
            default -> {
                return new HttpResponse(HttpConstants.RESPONSE_404 + HttpConstants.CRLF, null);
            }
        }
    }

    private static HttpResponse handleGet(HttpRequest request, String filesFolder) {
        String endpoint = request.endpoint();
        if ("/".equals(endpoint)) {
            return new HttpResponse(HttpConstants.RESPONSE_200 + HttpConstants.CRLF, null);
        } else if (endpoint.startsWith("/echo/")) {
            String msg = endpoint.substring("/echo/".length());
            String encoding = extractHeader(request.headers(), "Accept-Encoding");
            return buildTextResponse(msg, ContentEncoding.fromHeader(encoding));
        } else if (endpoint.startsWith("/files/")) {
            String fileName = endpoint.substring("/files/".length());
            Path path = Paths.get(filesFolder, fileName);
            if (Files.exists(path)) {
                try {
                    byte[] content = Files.readAllBytes(path);
                    return buildFileResponse(content);
                } catch (IOException e) {
                    return new HttpResponse(HttpConstants.RESPONSE_404 + HttpConstants.CRLF, null);
                }
            } else {
                return new HttpResponse(HttpConstants.RESPONSE_404 + HttpConstants.CRLF, null);
            }
        } else if ("/user-agent".equals(endpoint)) {
            String userAgent = extractHeader(request.headers(), "User-Agent");
            return buildTextResponse(userAgent != null ? userAgent : "", ContentEncoding.NONE);
        } else {
            return new HttpResponse(HttpConstants.RESPONSE_404 + HttpConstants.CRLF, null);
        }
    }

    private static HttpResponse handlePost(HttpRequest request, String filesFolder) {
        String endpoint = request.endpoint();
        if (endpoint.startsWith("/files/")) {
            String fileName = endpoint.substring("/files/".length());
            Path path = Paths.get(filesFolder, fileName);
            try {
                Files.write(path, request.body().getBytes(StandardCharsets.UTF_8));
                return new HttpResponse(HttpConstants.RESPONSE_201 + HttpConstants.CRLF, null);
            } catch (IOException e) {
                return new HttpResponse(HttpConstants.RESPONSE_404 + HttpConstants.CRLF, null);
            }
        }
        return new HttpResponse(HttpConstants.RESPONSE_404 + HttpConstants.CRLF, null);
    }

    private static String extractHeader(String headers, String key) {
        for (String line : headers.split("\r\n")) {
            if (line.toLowerCase().startsWith(key.toLowerCase() + ":")) {
                return line.split(":", 2)[1].trim();
            }
        }
        return null;
    }

    private static HttpResponse buildTextResponse(String msg, ContentEncoding encoding) {
        String headers = "Content-Type: text/plain" + HttpConstants.CRLF;
        byte[] compressedMsg = null;
        int contentLength = msg.length();
        if(encoding == ContentEncoding.GZIP) {
            headers += "Content-Encoding: gzip" + HttpConstants.CRLF;
            compressedMsg = GzipUtil.gzip(msg);
            contentLength = compressedMsg.length;
        }
        headers += "Content-Length: " + contentLength + HttpConstants.CRLF + HttpConstants.CRLF;

        if (compressedMsg == null) {
            return new HttpResponse(HttpConstants.RESPONSE_200 + headers + msg, null);
        } else {
            return new HttpResponse(HttpConstants.RESPONSE_200 + headers, compressedMsg);
        }
    }

    private static HttpResponse buildFileResponse(byte[] content) {
        String headers = "Content-Type: application/octet-stream" + HttpConstants.CRLF +
                "Content-Length: " + content.length + HttpConstants.CRLF + HttpConstants.CRLF;
        return new HttpResponse(HttpConstants.RESPONSE_200 + headers, content);
    }
}
