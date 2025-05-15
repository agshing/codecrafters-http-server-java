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
                return HttpResponse.plainText(HttpConstants.RESPONSE_404 + HttpConstants.CRLF);
            }
        }
    }

    private static HttpResponse handleGet(HttpRequest request, String filesFolder) {
        String endpoint = request.endpoint();
        if ("/".equals(endpoint)) {
            return HttpResponse.plainText(HttpConstants.RESPONSE_200 + HttpConstants.CRLF);
        } else if (endpoint.startsWith("/echo/")) {
            String msg = endpoint.substring("/echo/".length());
            String encoding = HttpRequestParser.extractHeader(request.headers(), "Accept-Encoding");
            return buildTextResponse(msg, request.headers(), ContentEncoding.fromHeader(encoding));
        } else if (endpoint.startsWith("/files/")) {
            String fileName = endpoint.substring("/files/".length());
            Path path = Paths.get(filesFolder, fileName);
            if (Files.exists(path)) {
                try {
                    byte[] content = Files.readAllBytes(path);
                    return buildFileResponse(content, request.headers());
                } catch (IOException e) {
                    return HttpResponse.plainText(HttpConstants.RESPONSE_404 + HttpConstants.CRLF);
                }
            } else {
                return HttpResponse.plainText(HttpConstants.RESPONSE_404 + HttpConstants.CRLF);
            }
        } else if ("/user-agent".equals(endpoint)) {
            String userAgent = HttpRequestParser.extractHeader(request.headers(), "User-Agent");
            return buildTextResponse(userAgent != null ? userAgent : "", request.headers(), ContentEncoding.NONE);
        } else {
            return HttpResponse.plainText(HttpConstants.RESPONSE_404 + HttpConstants.CRLF);
        }
    }

    private static HttpResponse handlePost(HttpRequest request, String filesFolder) {
        String endpoint = request.endpoint();
        if (endpoint.startsWith("/files/")) {
            String fileName = endpoint.substring("/files/".length());
            Path path = Paths.get(filesFolder, fileName);
            try {
                Files.write(path, request.body().getBytes(StandardCharsets.UTF_8));
                return HttpResponse.plainText(HttpConstants.RESPONSE_201 + HttpConstants.CRLF);
            } catch (IOException e) {
                return HttpResponse.plainText(HttpConstants.RESPONSE_404 + HttpConstants.CRLF);
            }
        }
        return HttpResponse.plainText(HttpConstants.RESPONSE_404 + HttpConstants.CRLF);
    }

    private static HttpResponse buildTextResponse(String msg, String requestHeaders, ContentEncoding encoding) {
        String headers = "Content-Type: text/plain" + HttpConstants.CRLF;
        byte[] compressedMsg = null;
        int contentLength = msg.length();
        if(encoding == ContentEncoding.GZIP) {
            headers += "Content-Encoding: gzip" + HttpConstants.CRLF;
            compressedMsg = GzipUtil.gzip(msg);
            contentLength = compressedMsg.length;
        }
        headers += buildConnectionHeaderIfApplicable(requestHeaders);
        headers += "Content-Length: " + contentLength + HttpConstants.CRLF + HttpConstants.CRLF;

        if (compressedMsg == null) {
            return HttpResponse.plainText(HttpConstants.RESPONSE_200 + headers + msg);
        } else {
            return HttpResponse.withBinary(HttpConstants.RESPONSE_200 + headers, compressedMsg);
        }
    }

    private static HttpResponse buildFileResponse(byte[] content, String requestHeaders) {
        String headers = "Content-Type: application/octet-stream" + HttpConstants.CRLF +
                "Content-Length: " + content.length + HttpConstants.CRLF;
        headers += buildConnectionHeaderIfApplicable(requestHeaders);
        headers += HttpConstants.CRLF;

        return HttpResponse.withBinary(HttpConstants.RESPONSE_200 + headers, content);
    }

    private static String buildConnectionHeaderIfApplicable(String requestHeaders){
        String connectionHeader = HttpRequestParser.extractHeader(requestHeaders, "Connection");
        if ("close".equalsIgnoreCase(connectionHeader)) {
            return "Connection: close" + HttpConstants.CRLF;
        }

        return "";
    }
}
