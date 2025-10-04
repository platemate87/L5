package l1j.server.server.command.executor;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l1j.server.server.datatables.MapIdsTableEditor;
import l1j.server.server.datatables.MapsTable;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.model.map.L1V1Map;
import l1j.server.server.model.map.L1WorldMap;
import l1j.server.server.serverpackets.S_SystemMessage;
import l1j.server.server.utils.FileUtil;

public class L1MapInfo implements L1CommandExecutor {
        private static final Logger _log = LoggerFactory.getLogger(L1MapInfo.class.getName());
        private static final String MAP_DIRECTORY = "./maps/";
        private static final byte DEFAULT_TILE = 0;

        public static L1CommandExecutor getInstance() {
                return new L1MapInfo();
        }

        @Override
        public void execute(L1PcInstance pc, String cmdName, String arg) {
                if (pc == null) {
                        return;
                }

                String trimmed = arg == null ? "" : arg.trim();
                if (trimmed.isEmpty()) {
                        handleInfo(pc);
                        return;
                }

                String[] args = trimmed.split("\\s+");
                String command = args[0].toLowerCase(Locale.ENGLISH);
                try {
                        switch (command) {
                        case "info":
                                handleInfo(pc);
                                break;
                        case "update":
                                handleUpdate(pc, args);
                                break;
                        case "save":
                                handleSave(pc);
                                break;
                        case "new":
                                handleNew(pc, args);
                                break;
                        case "meta":
                                handleMeta(pc, Arrays.copyOfRange(args, 1, args.length));
                                break;
                        default:
                                sendUsage(pc);
                                break;
                        }
                } catch (Exception ex) {
                        _log.warn(ex.getLocalizedMessage(), ex);
                        sendUsage(pc);
                }
        }

        private void handleInfo(L1PcInstance pc) {
                int currentTile = pc.getMap().getOriginalTile(pc.getX(), pc.getY());
                pc.sendPackets(new S_SystemMessage("Current Tile: " + currentTile));
        }

        private void handleUpdate(L1PcInstance pc, String[] args) {
                if (args.length < 2) {
                                throw new IllegalArgumentException("Missing tile value");
                }
                short newValue = Byte.parseByte(args[1]);
                pc.getMap().setOriginalTile(pc.getX(), pc.getY(), newValue);
                pc.sendPackets(new S_SystemMessage("Tile updated to: " + newValue));
        }

        private void handleSave(L1PcInstance pc) throws IOException {
                pc.sendPackets(new S_SystemMessage("Starting save.. this may take a second..."));
                String currentMapFilename = MAP_DIRECTORY + pc.getMapId() + ".txt";
                String backupMapFilename = currentMapFilename + ".bak";

                File currentMap = new File(currentMapFilename);
                if (currentMap.exists()) {
                        FileUtil.copyFileUsingStream(currentMap, new File(backupMapFilename));
                }

                FileUtil.writeFile(currentMapFilename, pc.getMap().toCsv());

                pc.sendPackets(new S_SystemMessage("Map " + pc.getMapId() + " new values saved."));
                pc.sendPackets(new S_SystemMessage("Backup created in the maps directory as " + backupMapFilename));
        }

        private void handleNew(L1PcInstance pc, String[] args) throws SQLException, IOException {
                if (args.length < 6) {
                        throw new IllegalArgumentException("Usage: .map new <mapId> <width> <height> <startX> <startY> [flag value]...");
                }

                int mapId = Integer.parseInt(args[1]);
                int width = Integer.parseInt(args[2]);
                int height = Integer.parseInt(args[3]);
                int startX = Integer.parseInt(args[4]);
                int startY = Integer.parseInt(args[5]);

                if (width <= 0 || height <= 0) {
                        throw new IllegalArgumentException("Width and height must be positive integers");
                }

                Map<String, Boolean> overrides = parseBooleanFlags(args, 6);
                Map<String, Boolean> flags = MapIdsTableEditor.createDefaultBooleanFlags();
                for (Map.Entry<String, Boolean> entry : overrides.entrySet()) {
                        flags.put(entry.getKey(), entry.getValue());
                }

                int endX = startX + width - 1;
                int endY = startY + height - 1;

                byte[][] tiles = new byte[width][height];
                for (int x = 0; x < width; x++) {
                        Arrays.fill(tiles[x], DEFAULT_TILE);
                }

                L1V1Map newMap = new L1V1Map(mapId, tiles, startX, startY, getFlag(flags, "underwater"),
                                getFlag(flags, "markable"), getFlag(flags, "teleportable"), getFlag(flags, "escapable"),
                                getFlag(flags, "resurrection"), getFlag(flags, "painwand"), getFlag(flags, "penalty"),
                                getFlag(flags, "take_pets"), getFlag(flags, "recall_pets"), getFlag(flags, "usable_item"),
                                getFlag(flags, "usable_skill"));

                String locationName = MapsTable.getInstance().locationname(mapId);
                if (locationName == null || locationName.isEmpty()) {
                        locationName = "Custom Map " + mapId;
                }

                MapIdsTableEditor.upsertMapRecord(mapId, startX, endX, startY, endY, flags, locationName);
                MapsTable.getInstance().reload(mapId);

                File mapFile = new File(MAP_DIRECTORY + mapId + ".txt");
                if (mapFile.exists()) {
                        FileUtil.copyFileUsingStream(mapFile, new File(mapFile.getAbsolutePath() + ".bak"));
                }
                writeMapFile(newMap);

                L1WorldMap.getInstance().reloadMap(mapId);

                pc.sendPackets(new S_SystemMessage("Map " + mapId + " created with size " + width + "x" + height + "."));
        }

        private void handleMeta(L1PcInstance pc, String[] args) {
                if (args.length == 0) {
                        throw new IllegalArgumentException("Usage: .map meta set [mapId] <flag> <true|false>");
                }

                String subCommand = args[0].toLowerCase(Locale.ENGLISH);
                switch (subCommand) {
                case "set":
                        handleMetaSet(pc, Arrays.copyOfRange(args, 1, args.length));
                        break;
                default:
                        throw new IllegalArgumentException("Unknown meta command: " + subCommand);
                }
        }

        private void handleMetaSet(L1PcInstance pc, String[] args) {
                if (args.length < 2) {
                        throw new IllegalArgumentException("Usage: .map meta set [mapId] <flag> <true|false>");
                }

                int argIndex = 0;
                int mapId;
                if (isInteger(args[argIndex])) {
                        mapId = Integer.parseInt(args[argIndex]);
                        argIndex++;
                } else {
                        mapId = pc.getMapId();
                }

                if (argIndex >= args.length - 1) {
                        throw new IllegalArgumentException("Usage: .map meta set [mapId] <flag> <true|false>");
                }

                String columnToken = args[argIndex++];
                String valueToken = args[argIndex];

                String normalizedColumn = MapIdsTableEditor.normalizeColumnName(columnToken);
                if (normalizedColumn == null) {
                        throw new IllegalArgumentException("Unknown map flag: " + columnToken);
                }

                boolean newValue = parseBoolean(valueToken);
                if (!MapIdsTableEditor.updateBooleanFlag(mapId, normalizedColumn, newValue)) {
                        throw new IllegalStateException("Unable to update flag " + normalizedColumn + " for map " + mapId);
                }

                MapsTable.getInstance().reload(mapId);
                L1WorldMap.getInstance().reloadMap(mapId);

                pc.sendPackets(new S_SystemMessage(
                                "Updated map " + mapId + " flag " + normalizedColumn + " to " + Boolean.toString(newValue)));
        }

        private Map<String, Boolean> parseBooleanFlags(String[] args, int startIndex) {
                Map<String, Boolean> values = new LinkedHashMap<>();
                int index = startIndex;
                while (index < args.length) {
                        String token = args[index];
                        String key;
                        String value;
                        if (token.contains("=")) {
                                String[] parts = token.split("=", 2);
                                key = parts[0];
                                value = parts.length > 1 ? parts[1] : "true";
                                index++;
                        } else {
                                key = token;
                                if (index + 1 >= args.length) {
                                        throw new IllegalArgumentException("Missing value for flag " + key);
                                }
                                value = args[index + 1];
                                index += 2;
                        }
                        String normalized = MapIdsTableEditor.normalizeColumnName(key);
                        if (normalized == null) {
                                throw new IllegalArgumentException("Unknown map flag: " + key);
                        }
                        values.put(normalized, parseBoolean(value));
                }
                return values;
        }

        private boolean getFlag(Map<String, Boolean> flags, String key) {
                Boolean value = flags.get(key);
                return value != null && value;
        }

        private boolean parseBoolean(String value) {
                String normalized = value.toLowerCase(Locale.ENGLISH);
                switch (normalized) {
                case "true":
                case "1":
                case "yes":
                case "on":
                        return true;
                case "false":
                case "0":
                case "no":
                case "off":
                        return false;
                default:
                        throw new IllegalArgumentException("Unknown boolean value: " + value);
                }
        }

        private boolean isInteger(String value) {
                try {
                        Integer.parseInt(value);
                        return true;
                } catch (NumberFormatException ex) {
                        return false;
                }
        }

        private void writeMapFile(L1V1Map map) throws IOException {
                byte[][] tiles = map.getRawTiles();
                int width = map.getWidth();
                int height = map.getHeight();
                StringBuilder data = new StringBuilder();
                for (int y = 0; y < height; y++) {
                        for (int x = 0; x < width; x++) {
                                data.append(Byte.toUnsignedInt(tiles[x][y]));
                                if (x < width - 1) {
                                        data.append(',');
                                }
                        }
                        data.append('\n');
                }
                FileUtil.writeFile(MAP_DIRECTORY + map.getId() + ".txt", data.toString());
        }

        private void sendUsage(L1PcInstance pc) {
                pc.sendPackets(new S_SystemMessage(
                                ".map [info|update|save|new|meta set] (see .map meta set [mapId] <flag> <true|false>)"));
        }
}
