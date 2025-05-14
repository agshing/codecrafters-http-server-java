import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class HttpRequestParser {
    private static final Pattern REQUEST_LINE_PATTERN = Pattern.compile("^(GET|POST)\\s+(\\S+)\\s+HTTP/1\\.1", Pattern.MULTILINE);

    public static HttpRequest parse(InputStream inputStream) throws IOException {
        ByteArrayOutputStream headerBuffer = new ByteArrayOutputStream();
        int prev = 0, curr;
        while ((curr = inputStream.read()) != -1) {
            headerBuffer.write(curr);
            if (prev == '\r' && curr == '\n') {
                byte[] temp = headerBuffer.toByteArray();
                if (endsWithCRLFCRLF(temp)) break;
            }
            prev = curr;
        }

        String headers = headerBuffer.toString(StandardCharsets.UTF_8);
        Matcher matcher = REQUEST_LINE_PATTERN.matcher(headers);
        String method = "", endpoint = "";
        if (matcher.find()) {
            method = matcher.group(1);
            endpoint = matcher.group(2);
        }

        int contentLength = 0;
        for (String line : headers.split("\r\n")) {
            if (line.toLowerCase().startsWith("content-length:")) {
                contentLength = Integer.parseInt(line.split(":")[1].trim());
            }
        }

        byte[] bodyBytes = inputStream.readNBytes(contentLength);
        String body = new String(bodyBytes, StandardCharsets.UTF_8);

        return new HttpRequest(method, endpoint, headers, body);
    }

    private static boolean endsWithCRLFCRLF(byte[] data) {
        int len = data.length;
        return len >= 4 &&
                data[len - 4] == '\r' && data[len - 3] == '\n' &&
                data[len - 2] == '\r' && data[len - 1] == '\n';
    }
}