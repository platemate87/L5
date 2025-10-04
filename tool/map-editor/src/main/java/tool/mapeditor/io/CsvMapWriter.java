package tool.mapeditor.io;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import tool.mapeditor.model.EditableL1Map;

public class CsvMapWriter {
    private final Path mapsDirectory;

    public CsvMapWriter(Path mapsDirectory) {
        this.mapsDirectory = mapsDirectory;
    }

    public void write(EditableL1Map map) throws IOException {
        Path file = mapsDirectory.resolve(map.getId() + ".txt");
        Files.createDirectories(file.getParent());
        byte[] data = map.toCsv().getBytes(StandardCharsets.UTF_8);
        Files.write(file, data);
    }

    public static CsvMapWriter forProjectRoot() {
        return new CsvMapWriter(Paths.get("maps"));
    }
}
