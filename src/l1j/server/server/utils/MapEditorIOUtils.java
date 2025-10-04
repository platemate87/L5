package l1j.server.server.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Helper functions used by the in-game map editor for persisting map files
 * and managing their backups/caches.
 */
public final class MapEditorIOUtils {

        private static final String MAP_DIR = "./maps/";
        private static final String MAP_CACHE_DIR = "./data/mapcache/";

        private MapEditorIOUtils() {
        }

        public static File getMapFile(short mapId) {
                return new File(MAP_DIR + mapId + ".txt");
        }

        public static File getBackupFile(short mapId) {
                return new File(MAP_DIR + mapId + ".txt.bak");
        }

        public static void writeMapFile(short mapId, String csvContent, boolean createBackup) throws IOException {
                File mapFile = getMapFile(mapId);
                ensureParentDirectory(mapFile.toPath());
                if (createBackup && mapFile.exists()) {
                        FileUtil.copyFileUsingStream(mapFile, getBackupFile(mapId));
                }
                FileUtil.writeFile(mapFile.getPath(), csvContent);
                deleteMapCache(mapId);
        }

        public static void writeBlankMap(short mapId, int width, int height, int tileValue) throws IOException {
                if (width <= 0 || height <= 0) {
                        throw new IllegalArgumentException("width/height must be positive");
                }
                byte[][] tiles = new byte[width][height];
                byte tile = (byte) tileValue;
                for (int x = 0; x < width; x++) {
                        Arrays.fill(tiles[x], tile);
                }
                writeTiles(mapId, tiles, false);
        }

        public static void writeTiles(short mapId, byte[][] tiles, boolean createBackup) throws IOException {
                if (tiles == null || tiles.length == 0 || tiles[0].length == 0) {
                        throw new IllegalArgumentException("tiles");
                }
                StringBuilder builder = new StringBuilder();
                int width = tiles.length;
                int height = tiles[0].length;
                for (int y = 0; y < height; y++) {
                        if (y > 0) {
                                builder.append(System.lineSeparator());
                        }
                        for (int x = 0; x < width; x++) {
                                if (x > 0) {
                                        builder.append(',');
                                }
                                builder.append(tiles[x][y]);
                        }
                }
                writeMapFile(mapId, builder.toString(), createBackup);
        }

        public static boolean restoreBackup(short mapId) throws IOException {
                File backup = getBackupFile(mapId);
                if (!backup.exists()) {
                        return false;
                }
                File mapFile = getMapFile(mapId);
                ensureParentDirectory(mapFile.toPath());
                FileUtil.copyFileUsingStream(backup, mapFile);
                deleteMapCache(mapId);
                return true;
        }

        public static void deleteMapCache(short mapId) {
                File cache = new File(MAP_CACHE_DIR + mapId + ".map");
                if (cache.exists() && !cache.delete()) {
                        cache.deleteOnExit();
                }
        }

        private static void ensureParentDirectory(Path path) throws IOException {
                Path parent = path.getParent();
                if (parent == null) {
                        return;
                }
                if (Files.exists(parent)) {
                        return;
                }
                Files.createDirectories(parent);
        }
}
