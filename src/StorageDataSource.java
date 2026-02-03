import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class StorageDataSource implements DataSource {
    private final Path filePath;

    public StorageDataSource() {
        this(Paths.get("storage-data.txt"));
    }

    public StorageDataSource(Path filePath) {
        this.filePath = filePath;
    }

    @Override
    public void Write(String data) {
        try {
            if (filePath.getParent() != null) {
                Files.createDirectories(filePath.getParent());
            }
            Files.writeString(
                    filePath,
                    data,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Nu s-a putut scrie in fisierul de stocare", e);
        }
    }

    @Override
    public String Read() {
        try {
            if (!Files.exists(filePath)) {
                return "";
            }
            return Files.readString(filePath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Nu s-a putut citi din fisierul de stocare", e);
        }
    }
}
