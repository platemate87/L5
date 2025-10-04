package tool.mapeditor.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import tool.mapeditor.model.EditableL1Map;

/**
 * Loads map CSV files using the layout produced by {@code L1V1Map#toCsv()}.
 */
public class CsvMapLoader {
    private final Path mapsDirectory;

    public CsvMapLoader(Path mapsDirectory) {
        this.mapsDirectory = mapsDirectory;
    }

    public EditableL1Map load(int mapId) throws IOException {
        Path file = mapsDirectory.resolve(mapId + ".txt");
        if (!Files.exists(file)) {
            throw new IOException("Map file not found: " + file.toAbsolutePath());
        }
        List<byte[]> rows = new ArrayList<>();
        int width = -1;
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] parts = line.split(",");
                if (width == -1) {
                    width = parts.length;
                }
                byte[] row = new byte[parts.length];
                for (int i = 0; i < parts.length; i++) {
                    row[i] = (byte) Integer.parseInt(parts[i].trim());
                }
                rows.add(row);
            }
        }
        if (rows.isEmpty()) {
            throw new IOException("Map file contains no tile data: " + file.toAbsolutePath());
        }
        int height = rows.size();
        if (width <= 0) {
            throw new IOException("Map file has invalid width: " + file.toAbsolutePath());
        }
        byte[][] tiles = new byte[width][height];
        for (int y = 0; y < height; y++) {
            byte[] row = rows.get(y);
            if (row.length != width) {
                throw new IOException("Inconsistent row width at line " + (y + 1) + " in " + file.toAbsolutePath());
            }
            for (int x = 0; x < width; x++) {
                tiles[x][y] = row[x];
            }
        }
        return new EditableL1Map(mapId, tiles, 0, 0);
    }

    public static CsvMapLoader forProjectRoot() {
        return new CsvMapLoader(Paths.get("maps"));
    }
}
