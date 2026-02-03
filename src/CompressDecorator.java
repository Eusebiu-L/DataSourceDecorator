import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class CompressDecorator extends BaseDataSourceDecorator {
    public CompressDecorator(DataSource wrappee) {
        super(wrappee);
    }

    @Override
    public void Write(String data) {
        try {
            byte[] compressed = compress(data.getBytes(StandardCharsets.UTF_8));
            String encoded = Base64.getEncoder().encodeToString(compressed);
            _wrappee.Write(encoded);
        } catch (IOException e) {
            throw new RuntimeException("Nu s-a putut comprima mesajul", e);
        }
    }

    @Override
    public String Read() {
        String encoded = _wrappee.Read();
        if (encoded == null || encoded.isEmpty()) {
            return "";
        }
        try {
            byte[] compressed = Base64.getDecoder().decode(encoded);
            byte[] decompressed = decompress(compressed);
            return new String(decompressed, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Nu s-a putut decomprima mesajul", e);
        }
    }

    private byte[] compress(byte[] data) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
            gzip.write(data);
        }
        return baos.toByteArray();
    }

    private byte[] decompress(byte[] data) throws IOException {
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(data));
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[512];
            int read;
            while ((read = gzip.read(buffer)) != -1) {
                baos.write(buffer, 0, read);
            }
            return baos.toByteArray();
        }
    }
}
