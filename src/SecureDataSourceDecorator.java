import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class SecureDataSourceDecorator extends BaseDataSourceDecorator {
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH = 128; // bits

    private final SecretKey secretKey;
    private final SecureRandom secureRandom;

    public SecureDataSourceDecorator(DataSource wrappee) {
        this(wrappee, defaultKey());
    }

    public SecureDataSourceDecorator(DataSource wrappee, SecretKey secretKey) {
        super(wrappee);
        this.secretKey = secretKey;
        this.secureRandom = new SecureRandom();
    }

    @Override
    public void Write(String data) {
        if (data == null || data.isEmpty()) {
            _wrappee.Write(data == null ? "" : data);
            return;
        }

        try {
            byte[] compressed = compress(data.getBytes(StandardCharsets.UTF_8));
            byte[] encrypted = encrypt(compressed);
            String encoded = Base64.getEncoder().encodeToString(encrypted);
            _wrappee.Write(encoded);
        } catch (IOException | GeneralSecurityException e) {
            throw new RuntimeException("Nu s-a putut salva mesajul securizat", e);
        }
    }

    @Override
    public String Read() {
        String encoded = _wrappee.Read();
        if (encoded == null || encoded.isEmpty()) {
            return "";
        }

        try {
            byte[] encrypted = Base64.getDecoder().decode(encoded);
            byte[] compressed = decrypt(encrypted);
            byte[] plain = decompress(compressed);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (IOException | GeneralSecurityException e) {
            throw new RuntimeException("Nu s-a putut citi mesajul securizat", e);
        }
    }

    private byte[] encrypt(byte[] data) throws GeneralSecurityException {
        byte[] iv = new byte[IV_LENGTH_BYTES];
        secureRandom.nextBytes(iv);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
        byte[] cipherBytes = cipher.doFinal(data);

        byte[] payload = new byte[iv.length + cipherBytes.length];
        System.arraycopy(iv, 0, payload, 0, iv.length);
        System.arraycopy(cipherBytes, 0, payload, iv.length, cipherBytes.length);
        return payload;
    }

    private byte[] decrypt(byte[] payload) throws GeneralSecurityException {
        if (payload.length <= IV_LENGTH_BYTES) {
            throw new IllegalStateException("Payload criptat invalid");
        }

        byte[] iv = new byte[IV_LENGTH_BYTES];
        byte[] cipherBytes = new byte[payload.length - IV_LENGTH_BYTES];
        System.arraycopy(payload, 0, iv, 0, IV_LENGTH_BYTES);
        System.arraycopy(payload, IV_LENGTH_BYTES, cipherBytes, 0, cipherBytes.length);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
        return cipher.doFinal(cipherBytes);
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

    private static SecretKey defaultKey() {
        byte[] keyBytes = "0123456789ABCDEF".getBytes(StandardCharsets.UTF_8);
        return new SecretKeySpec(keyBytes, "AES");
    }
}
