import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) {
        DataSource apiDs = new ApiDataSource();
        apiDs.Write("Mesaj trimis catre API");

        Path storagePath = Paths.get("storage-data.txt");
        DataSource storage = new StorageDataSource(storagePath);
        DataSource secureStorage = new CompressDecorator(new EncryptionDecorator(storage));

        String mesajOriginal = "Date sensibile ce trebuie protejate";
        secureStorage.Write(mesajOriginal);

        String mesajCitit = secureStorage.Read();

        System.out.println("Mesaj original: " + mesajOriginal);
        System.out.println("Mesaj citit dupa compresie/criptare: " + mesajCitit);
        System.out.println("Continut brut din fisier: " + storage.Read());
    }
}