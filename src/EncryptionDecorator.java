import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

public class EncryptionDecorator extends BaseDataSourceDecorator {
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128; // bits
    private static final int IV_LENGTH_BYTES = 12;

    private final SecretKey secretKey;
    private final SecureRandom secureRandom;

    public EncryptionDecorator(DataSource wrappee) {
        this(wrappee, defaultKey());
    }

    public EncryptionDecorator(DataSource wrappee, SecretKey secretKey) {
        super(wrappee);
        this.secretKey = secretKey;
        this.secureRandom = new SecureRandom();
    }

    @Override
    public void Write(String data) {
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] cipherBytes = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[iv.length + cipherBytes.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(cipherBytes, 0, payload, iv.length, cipherBytes.length);

            String encoded = Base64.getEncoder().encodeToString(payload);
            _wrappee.Write(encoded);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Nu s-a putut cripta mesajul", e);
        }
    }

    @Override
    public String Read() {
        String encodedPayload = _wrappee.Read();
        if (encodedPayload == null || encodedPayload.isEmpty()) {
            return "";
        }

        try {
            byte[] payload = Base64.getDecoder().decode(encodedPayload);
            if (payload.length <= IV_LENGTH_BYTES) {
                throw new IllegalStateException("Payload criptat invalid");
            }

            byte[] iv = new byte[IV_LENGTH_BYTES];
            byte[] cipherBytes = new byte[payload.length - IV_LENGTH_BYTES];
            System.arraycopy(payload, 0, iv, 0, IV_LENGTH_BYTES);
            System.arraycopy(payload, IV_LENGTH_BYTES, cipherBytes, 0, cipherBytes.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] plainBytes = cipher.doFinal(cipherBytes);

            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Nu s-a putut decripta mesajul", e);
        }
    }

    private static SecretKey defaultKey() {
        byte[] keyBytes = "0123456789ABCDEF".getBytes(StandardCharsets.UTF_8);
        return new SecretKeySpec(keyBytes, "AES");
    }
}
