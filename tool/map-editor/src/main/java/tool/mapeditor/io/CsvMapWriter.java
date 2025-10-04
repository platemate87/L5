package tool.mapeditor.io;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import tool.mapeditor.model.EditableL1Map;

public class CsvMapWriter {
    private final Path mapsDirectory;

    public CsvMapWriter(Path mapsDirectory) {
        this.mapsDirectory = mapsDirectory;
    }

    public void write(EditableL1Map map) throws IOException {
        Path file = mapsDirectory.resolve(map.getId() + ".txt");
        Files.createDirectories(file.getParent());
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            writer.write(map.toCsv());
        }
    }

    public static CsvMapWriter forProjectRoot() {
        return new CsvMapWriter(Paths.get("maps"));
    }
}
