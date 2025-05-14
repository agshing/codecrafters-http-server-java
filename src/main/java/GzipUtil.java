import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;
import java.nio.charset.StandardCharsets;

public class GzipUtil {
    public static byte[] gzip(String input) {
        if (input == null || input.isEmpty()) {
            return new byte[0];
        }

        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipStream = new GZIPOutputStream(byteStream)) {
            gzipStream.write(input.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return byteStream.toByteArray();
    }
}