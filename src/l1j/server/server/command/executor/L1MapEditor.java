package l1j.server.server.command.executor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l1j.server.server.datatables.MapIdsTableEditor;
import l1j.server.server.datatables.MapIdsTableEditor.MapMetadata;
import l1j.server.server.datatables.MapsTable;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.model.map.edit.MapEditManager;
import l1j.server.server.model.map.edit.MapEditOperation;
import l1j.server.server.model.map.edit.MapEditSession;
import l1j.server.server.model.map.edit.MapEditSession.BrushMode;
import l1j.server.server.model.map.edit.MapEditSession.TileChangeSummary;
import l1j.server.server.model.map.edit.MapEditSession.ZoneSetting;
import l1j.server.server.model.map.L1WorldMap;
import l1j.server.server.serverpackets.S_SystemMessage;
import l1j.server.server.utils.MapEditorIOUtils;

public class L1MapEditor implements L1CommandExecutor {

        private static final Logger _log = LoggerFactory.getLogger(L1MapEditor.class);

        private static final int PREVIEW_LIMIT = 5;
        private static final String BRUSH_USAGE = "Usage: .map brush [tile <id>|tile none|zone <type>|passable <on|off|clear>"
                        + "|size <value>|info]";

        public static L1CommandExecutor getInstance() {
                return new L1MapEditor();
        }

        private L1MapEditor() {
        }

        @Override
        public void execute(L1PcInstance pc, String cmdName, String arg) {
                if (pc == null) {
                        return;
                }
                String[] parts = arg == null ? new String[0] : arg.trim().split("\\s+");
                if (parts.length == 0 || parts[0].isEmpty()) {
                        sendHelp(pc);
                        return;
                }

                String subCommand = parts[0].toLowerCase();
                MapEditManager manager = MapEditManager.getInstance();

                try {
                        switch (subCommand) {
                        case "start":
                                handleStart(pc, manager);
                                break;
                        case "new":
                                handleNew(pc, manager, parts);
                                break;
                        case "meta":
                                handleMeta(pc, manager, parts);
                                break;
                        case "brush":
                                handleBrush(pc, manager, parts);
                                break;
                        case "mode":
                                handleMode(pc, manager, parts);
                                break;
                        case "apply":
                                handleApply(pc, manager, parts);
                                break;
                        case "undo":
                                handleUndo(pc, manager);
                                break;
                        case "redo":
                                handleRedo(pc, manager);
                                break;
                        case "preview":
                                handlePreview(pc, manager);
                                break;
                        case "commit":
                        case "save":
                                handleSave(pc, manager);
                                break;
                        case "revert":
                                handleRevert(pc, manager, parts);
                                break;
                        case "cancel":
                                handleCancel(pc, manager);
                                break;
                        default:
                                sendHelp(pc);
                                break;
                        }
                } catch (Exception ex) {
                        _log.warn("Map editor command failed", ex);
                        pc.sendPackets(new S_SystemMessage("Map editor error: " + ex.getMessage()));
                }
        }

        private void handleStart(L1PcInstance pc, MapEditManager manager) {
                MapEditSession existing = manager.getSession(pc);
                MapEditSession session = manager.startSession(pc);
                if (existing == null) {
                        pc.sendPackets(new S_SystemMessage("Map editing session active on map " + session.getMapId() + "."));
                } else {
                        pc.sendPackets(new S_SystemMessage("Resuming map editing session on map " + session.getMapId() + "."));
                }
                if (session.hasPendingChanges()) {
                        pc.sendPackets(new S_SystemMessage("Pending edits: " + session.getPendingChangeCount()));
                }
                pc.sendPackets(new S_SystemMessage("Brush: " + session.describeBrush()));
        }

        private void handleNew(L1PcInstance pc, MapEditManager manager, String[] parts) {
                if (parts.length < 6) {
                        pc.sendPackets(new S_SystemMessage(
                                        "Usage: .map new <mapId> <startX> <startY> <width> <height> [name=<text>] [monster=<amount>] [drop=<rate>] [+flag|-flag ...] [tile=<id>]"));
                        return;
                }
                short mapId;
                int startX;
                int startY;
                int width;
                int height;
                try {
                        mapId = Short.parseShort(parts[1]);
                        startX = Integer.parseInt(parts[2]);
                        startY = Integer.parseInt(parts[3]);
                        width = Integer.parseInt(parts[4]);
                        height = Integer.parseInt(parts[5]);
                } catch (NumberFormatException ex) {
                        pc.sendPackets(new S_SystemMessage("Invalid map id or dimensions."));
                        return;
                }
                if (width <= 0 || height <= 0) {
                        pc.sendPackets(new S_SystemMessage("Width and height must be positive."));
                        return;
                }

                MapIdsTableEditor editor = MapIdsTableEditor.getInstance();
                MapMetadata existing;
                try {
                        existing = editor.load(mapId);
                } catch (RuntimeException ex) {
                        _log.warn("Unable to check map metadata", ex);
                        pc.sendPackets(new S_SystemMessage("Failed to query existing map metadata: " + ex.getMessage()));
                        return;
                }
                if (existing != null) {
                        pc.sendPackets(new S_SystemMessage("Map " + mapId + " already exists. Use .map meta set to update."));
                        return;
                }

                int endX = startX + width - 1;
                int endY = startY + height - 1;
                Map<String, String> values = parseKeyValueArgs(parts, 6);
                Map<String, Boolean> flags = parseBooleanFlags(parts, 6);

                MapMetadata metadata = new MapMetadata(mapId);
                metadata.setLocationName(values.getOrDefault("name", "Map " + mapId));
                metadata.setStartX(startX);
                metadata.setEndX(endX);
                metadata.setStartY(startY);
                metadata.setEndY(endY);

                String monsterValue = getOption(values, "monster", "monsters", "monster_amount");
                if (monsterValue != null) {
                        try {
                                metadata.setMonsterAmount(Double.parseDouble(monsterValue));
                        } catch (NumberFormatException ex) {
                                pc.sendPackets(new S_SystemMessage("Invalid monster amount: " + monsterValue));
                                return;
                        }
                }

                String dropValue = getOption(values, "drop", "drop_rate", "droprate");
                if (dropValue != null) {
                        try {
                                metadata.setDropRate(Double.parseDouble(dropValue));
                        } catch (NumberFormatException ex) {
                                pc.sendPackets(new S_SystemMessage("Invalid drop rate: " + dropValue));
                                return;
                        }
                }

                metadata.setUnderwater(getFlag(flags, "underwater", false));
                metadata.setMarkable(getFlag(flags, "markable", false));
                metadata.setTeleportable(getFlag(flags, "teleportable", false));
                metadata.setEscapable(getFlag(flags, "escapable", false));
                metadata.setUseResurrection(getFlag(flags, "resurrection", false));
                metadata.setUsePainwand(getFlag(flags, "painwand", false));
                metadata.setEnabledDeathPenalty(getFlag(flags, "penalty", false));
                metadata.setTakePets(getFlag(flags, "take_pets", false));
                metadata.setRecallPets(getFlag(flags, "recall_pets", false));
                metadata.setUsableItem(getFlag(flags, "usable_item", false));
                metadata.setUsableSkill(getFlag(flags, "usable_skill", false));

                try {
                        editor.save(metadata);
                        MapsTable.getInstance().reload();
                } catch (RuntimeException ex) {
                        _log.warn("Unable to save map metadata", ex);
                        pc.sendPackets(new S_SystemMessage("Failed to write map metadata: " + ex.getMessage()));
                        return;
                }

                int defaultTile = 0;
                String tileValue = values.get("tile");
                if (tileValue != null) {
                        try {
                                defaultTile = Integer.parseInt(tileValue);
                                if (defaultTile < 0 || defaultTile > 255) {
                                        pc.sendPackets(new S_SystemMessage("Tile id must be between 0 and 255."));
                                        return;
                                }
                        } catch (NumberFormatException ex) {
                                pc.sendPackets(new S_SystemMessage("Invalid tile id: " + tileValue));
                                return;
                        }
                }

                try {
                        MapEditorIOUtils.writeBlankMap(mapId, width, height, defaultTile);
                } catch (IOException ex) {
                        _log.error("Failed to create map file", ex);
                        pc.sendPackets(new S_SystemMessage("Failed to write map file: " + ex.getMessage()));
                        return;
                }

                refreshLiveMap(mapId);
                pc.sendPackets(new S_SystemMessage(
                                "Created map " + mapId + " with size " + width + "x" + height + ". Metadata saved."));
                MapEditSession session = manager.getSession(pc);
                if (session != null && session.getMapId() == mapId) {
                        manager.cancelSession(pc);
                        pc.sendPackets(new S_SystemMessage(
                                        "Existing editing session reset. Use .map start on the new map to edit tiles."));
                }
        }

        private void handleMeta(L1PcInstance pc, MapEditManager manager, String[] parts) {
                if (parts.length < 2) {
                        pc.sendPackets(new S_SystemMessage("Usage: .map meta set <mapId> [options]"));
                        return;
                }
                String sub = parts[1].toLowerCase(Locale.ENGLISH);
                switch (sub) {
                case "set":
                        handleMetaSet(pc, manager, parts);
                        break;
                default:
                        pc.sendPackets(new S_SystemMessage("Usage: .map meta set <mapId> [options]"));
                        break;
                }
        }

        private void handleMetaSet(L1PcInstance pc, MapEditManager manager, String[] parts) {
                if (parts.length < 3) {
                        pc.sendPackets(new S_SystemMessage(
                                        "Usage: .map meta set <mapId> [name=<text>] [startX=<value>] [startY=<value>] [endX=<value>] [endY=<value>] [width=<value>] [height=<value>] [monster=<amount>] [drop=<rate>] [+flag|-flag ...]"));
                        return;
                }
                int mapId;
                try {
                        mapId = Integer.parseInt(parts[2]);
                } catch (NumberFormatException ex) {
                        pc.sendPackets(new S_SystemMessage("Invalid map id."));
                        return;
                }

                MapIdsTableEditor editor = MapIdsTableEditor.getInstance();
                MapMetadata metadata;
                try {
                        metadata = editor.load(mapId);
                } catch (RuntimeException ex) {
                        _log.warn("Unable to load map metadata", ex);
                        pc.sendPackets(new S_SystemMessage("Failed to load map metadata: " + ex.getMessage()));
                        return;
                }
                if (metadata == null) {
                        pc.sendPackets(new S_SystemMessage("Map " + mapId + " does not exist. Use .map new to create it."));
                        return;
                }

                Map<String, String> values = parseKeyValueArgs(parts, 3);
                Map<String, Boolean> flags = parseBooleanFlags(parts, 3);
                boolean changed = false;

                String name = values.get("name");
                if (name != null && !name.equals(metadata.getLocationName())) {
                        metadata.setLocationName(name);
                        changed = true;
                }

                try {
                        changed |= updateCoordinate(values, metadata, "startx");
                        changed |= updateCoordinate(values, metadata, "starty");
                        changed |= updateCoordinate(values, metadata, "endx");
                        changed |= updateCoordinate(values, metadata, "endy");
                } catch (IllegalArgumentException ex) {
                        pc.sendPackets(new S_SystemMessage(ex.getMessage()));
                        return;
                }

                String widthValue = values.get("width");
                if (widthValue != null) {
                        try {
                                int width = Integer.parseInt(widthValue);
                                if (width <= 0) {
                                        throw new NumberFormatException();
                                }
                                int endX = metadata.getStartX() + width - 1;
                                if (endX != metadata.getEndX()) {
                                        metadata.setEndX(endX);
                                        changed = true;
                                }
                        } catch (NumberFormatException ex) {
                                pc.sendPackets(new S_SystemMessage("Invalid width: " + widthValue));
                                return;
                        }
                }

                String heightValue = values.get("height");
                if (heightValue != null) {
                        try {
                                int height = Integer.parseInt(heightValue);
                                if (height <= 0) {
                                        throw new NumberFormatException();
                                }
                                int endY = metadata.getStartY() + height - 1;
                                if (endY != metadata.getEndY()) {
                                        metadata.setEndY(endY);
                                        changed = true;
                                }
                        } catch (NumberFormatException ex) {
                                pc.sendPackets(new S_SystemMessage("Invalid height: " + heightValue));
                                return;
                        }
                }

                String monsterValue = getOption(values, "monster", "monsters", "monster_amount");
                if (monsterValue != null) {
                        try {
                                double monsterAmount = Double.parseDouble(monsterValue);
                                if (monsterAmount != metadata.getMonsterAmount()) {
                                        metadata.setMonsterAmount(monsterAmount);
                                        changed = true;
                                }
                        } catch (NumberFormatException ex) {
                                pc.sendPackets(new S_SystemMessage("Invalid monster amount: " + monsterValue));
                                return;
                        }
                }

                String dropValue = getOption(values, "drop", "drop_rate", "droprate");
                if (dropValue != null) {
                        try {
                                double dropRate = Double.parseDouble(dropValue);
                                if (dropRate != metadata.getDropRate()) {
                                        metadata.setDropRate(dropRate);
                                        changed = true;
                                }
                        } catch (NumberFormatException ex) {
                                pc.sendPackets(new S_SystemMessage("Invalid drop rate: " + dropValue));
                                return;
                        }
                }

                changed |= applyFlag(metadata::setUnderwater, metadata.isUnderwater(), flags, "underwater");
                changed |= applyFlag(metadata::setMarkable, metadata.isMarkable(), flags, "markable");
                changed |= applyFlag(metadata::setTeleportable, metadata.isTeleportable(), flags, "teleportable");
                changed |= applyFlag(metadata::setEscapable, metadata.isEscapable(), flags, "escapable");
                changed |= applyFlag(metadata::setUseResurrection, metadata.isUseResurrection(), flags, "resurrection");
                changed |= applyFlag(metadata::setUsePainwand, metadata.isUsePainwand(), flags, "painwand");
                changed |= applyFlag(metadata::setEnabledDeathPenalty, metadata.isEnabledDeathPenalty(), flags, "penalty");
                changed |= applyFlag(metadata::setTakePets, metadata.isTakePets(), flags, "take_pets");
                changed |= applyFlag(metadata::setRecallPets, metadata.isRecallPets(), flags, "recall_pets");
                changed |= applyFlag(metadata::setUsableItem, metadata.isUsableItem(), flags, "usable_item");
                changed |= applyFlag(metadata::setUsableSkill, metadata.isUsableSkill(), flags, "usable_skill");

                if (!changed) {
                        pc.sendPackets(new S_SystemMessage("No metadata changes detected."));
                        return;
                }

                if (metadata.getEndX() < metadata.getStartX() || metadata.getEndY() < metadata.getStartY()) {
                        pc.sendPackets(new S_SystemMessage("Invalid coordinates: end must be greater than or equal to start."));
                        return;
                }

                try {
                        editor.save(metadata);
                        MapsTable.getInstance().reload();
                } catch (RuntimeException ex) {
                        _log.warn("Unable to save map metadata", ex);
                        pc.sendPackets(new S_SystemMessage("Failed to save metadata: " + ex.getMessage()));
                        return;
                }

                refreshLiveMap((short) mapId);
                MapEditSession session = manager.getSession(pc);
                if (session != null && session.getMapId() == mapId) {
                        manager.cancelSession(pc);
                        pc.sendPackets(new S_SystemMessage(
                                        "Active editing session reset to pick up new metadata. Use .map start to resume."));
                }
                pc.sendPackets(new S_SystemMessage("Updated metadata for map " + mapId + "."));
        }

        private void handleBrush(L1PcInstance pc, MapEditManager manager, String[] parts) {
                MapEditSession session = requireSession(pc, manager);
                if (session == null) {
                        return;
                }
                if (parts.length < 2) {
                        pc.sendPackets(new S_SystemMessage(BRUSH_USAGE));
                        return;
                }
                String brushCommand = parts[1].toLowerCase();
                switch (brushCommand) {
                case "tile":
                        if (parts.length < 3) {
                                pc.sendPackets(new S_SystemMessage("Usage: .map brush tile <id|none>"));
                                return;
                        }
                        if ("none".equalsIgnoreCase(parts[2])) {
                                session.clearBrushTile();
                                pc.sendPackets(new S_SystemMessage("Brush tile cleared."));
                                break;
                        }
                        try {
                                short tile = Short.parseShort(parts[2]);
                                session.setBrushTileId(tile);
                                pc.sendPackets(new S_SystemMessage("Brush tile set to " + tile + "."));
                        } catch (NumberFormatException ex) {
                                pc.sendPackets(new S_SystemMessage("Invalid tile id."));
                        }
                        break;
                case "zone":
                        if (parts.length < 3) {
                                pc.sendPackets(new S_SystemMessage("Usage: .map brush zone <none|normal|safety|combat>"));
                                return;
                        }
                        ZoneSetting zone = parseZone(parts[2]);
                        session.setZoneSetting(zone);
                        pc.sendPackets(new S_SystemMessage("Zone setting set to " + zone.name().toLowerCase() + "."));
                        break;
                case "passable":
                        if (parts.length < 3) {
                                pc.sendPackets(new S_SystemMessage("Usage: .map brush passable <on|off|clear>"));
                                return;
                        }
                        String passableArg = parts[2].toLowerCase();
                        if ("on".equals(passableArg) || "true".equals(passableArg)) {
                                session.setPassableOverride(Boolean.TRUE);
                                pc.sendPackets(new S_SystemMessage("Brush passable override set to passable."));
                        } else if ("off".equals(passableArg) || "false".equals(passableArg)) {
                                session.setPassableOverride(Boolean.FALSE);
                                pc.sendPackets(new S_SystemMessage("Brush passable override set to blocked."));
                        } else if ("clear".equals(passableArg)) {
                                session.clearPassableOverride();
                                pc.sendPackets(new S_SystemMessage("Brush passable override cleared."));
                        } else {
                                pc.sendPackets(new S_SystemMessage("Usage: .map brush passable <on|off|clear>"));
                        }
                        break;
                case "size":
                        if (parts.length < 3) {
                                pc.sendPackets(new S_SystemMessage("Usage: .map brush size <value>"));
                                return;
                        }
                        try {
                                int size = Integer.parseInt(parts[2]);
                                session.setBrushSize(size);
                                pc.sendPackets(new S_SystemMessage("Brush size set to " + session.getBrushSize() + "."));
                        } catch (NumberFormatException ex) {
                                pc.sendPackets(new S_SystemMessage("Invalid brush size."));
                        }
                        break;
                case "info":
                        pc.sendPackets(new S_SystemMessage("Brush: " + session.describeBrush()));
                        break;
                default:
                        pc.sendPackets(new S_SystemMessage(BRUSH_USAGE));
                        break;
                }
                pc.sendPackets(new S_SystemMessage("Brush: " + session.describeBrush()));
        }

        private void handleMode(L1PcInstance pc, MapEditManager manager, String[] parts) {
                MapEditSession session = requireSession(pc, manager);
                if (session == null) {
                        return;
                }
                if (parts.length < 2) {
                        pc.sendPackets(new S_SystemMessage("Usage: .map mode <single|rectangle|radius|fill>"));
                        return;
                }
                BrushMode mode = parseMode(parts[1]);
                session.setMode(mode);
                pc.sendPackets(new S_SystemMessage("Brush mode set to " + mode.name().toLowerCase() + "."));
        }

        private void handleApply(L1PcInstance pc, MapEditManager manager, String[] parts) {
                MapEditSession session = requireSession(pc, manager);
                if (session == null) {
                        return;
                }
                int x = pc.getX();
                int y = pc.getY();
                if (parts.length >= 3) {
                        try {
                                x = Integer.parseInt(parts[1]);
                                y = Integer.parseInt(parts[2]);
                        } catch (NumberFormatException ex) {
                                pc.sendPackets(new S_SystemMessage("Usage: .map apply [x y]"));
                                return;
                        }
                }
                MapEditOperation operation = session.applyBrush(x, y);
                if (operation == null) {
                        pc.sendPackets(new S_SystemMessage("Brush apply made no changes."));
                        return;
                }
                pc.sendPackets(new S_SystemMessage("Applied " + operation.getChangeCount() + " tile(s)."));
                pc.sendPackets(new S_SystemMessage("Pending edits: " + session.getPendingChangeCount()));
        }

        private void handleUndo(L1PcInstance pc, MapEditManager manager) {
                MapEditSession session = requireSession(pc, manager);
                if (session == null) {
                        return;
                }
                MapEditOperation operation = session.undo();
                if (operation == null) {
                        pc.sendPackets(new S_SystemMessage("Nothing to undo."));
                        return;
                }
                pc.sendPackets(new S_SystemMessage("Undid " + operation.getChangeCount() + " tile(s)."));
                pc.sendPackets(new S_SystemMessage("Pending edits: " + session.getPendingChangeCount()));
        }

        private void handleRedo(L1PcInstance pc, MapEditManager manager) {
                MapEditSession session = requireSession(pc, manager);
                if (session == null) {
                        return;
                }
                MapEditOperation operation = session.redo();
                if (operation == null) {
                        pc.sendPackets(new S_SystemMessage("Nothing to redo."));
                        return;
                }
                pc.sendPackets(new S_SystemMessage("Redid " + operation.getChangeCount() + " tile(s)."));
                pc.sendPackets(new S_SystemMessage("Pending edits: " + session.getPendingChangeCount()));
        }

        private void handlePreview(L1PcInstance pc, MapEditManager manager) {
                MapEditSession session = requireSession(pc, manager);
                if (session == null) {
                        return;
                }
                if (!session.hasPendingChanges()) {
                        pc.sendPackets(new S_SystemMessage("No pending edits."));
                        return;
                }
                Collection<TileChangeSummary> summaries = session.getPendingChanges();
                pc.sendPackets(new S_SystemMessage(
                                "Pending edits: " + summaries.size() + " tile(s). Showing up to " + PREVIEW_LIMIT + "."));
                List<TileChangeSummary> preview = new ArrayList<>(summaries);
                preview.sort(Comparator.comparingInt(TileChangeSummary::getX).thenComparingInt(TileChangeSummary::getY));
                int count = 0;
                for (TileChangeSummary summary : preview) {
                        if (count++ >= PREVIEW_LIMIT) {
                                break;
                        }
                        StringBuilder sb = new StringBuilder();
                        sb.append("Tile (").append(summary.getX()).append(',').append(summary.getY()).append(") ");
                        if (summary.hasTileChange()) {
                                sb.append("tile ").append(summary.getOriginalTile()).append(" -> ")
                                                .append(summary.getCurrentTile());
                        }
                        if (summary.hasPassableChange()) {
                                if (summary.hasTileChange()) {
                                        sb.append(", ");
                                }
                                sb.append("passable ").append(summary.isOriginalPassable() ? "yes" : "no").append(" -> ")
                                                .append(summary.isCurrentPassable() ? "yes" : "no");
                        }
                        pc.sendPackets(new S_SystemMessage(sb.toString()));
                }
        }

        private void handleSave(L1PcInstance pc, MapEditManager manager) {
                MapEditSession session = requireSession(pc, manager);
                if (session == null) {
                        return;
                }
                if (!session.hasPendingChanges()) {
                        pc.sendPackets(new S_SystemMessage("No pending edits to save."));
                        return;
                }
                try {
                        int committed = session.commit();
                        refreshLiveMap(session.getMapId());
                        pc.sendPackets(new S_SystemMessage("Saved " + committed + " tile(s) to map " + session.getMapId() + "."));
                } catch (IOException e) {
                        _log.error("Failed to save map edits", e);
                        pc.sendPackets(new S_SystemMessage("Failed to save edits: " + e.getMessage()));
                }
        }

        private void handleRevert(L1PcInstance pc, MapEditManager manager, String[] parts) {
                MapEditSession session = manager.getSession(pc);
                short mapId;
                if (parts.length >= 2) {
                        try {
                                mapId = Short.parseShort(parts[1]);
                        } catch (NumberFormatException ex) {
                                pc.sendPackets(new S_SystemMessage("Invalid map id."));
                                return;
                        }
                } else if (session != null) {
                        mapId = session.getMapId();
                } else {
                        pc.sendPackets(new S_SystemMessage("Specify a map id or start an editing session first."));
                        return;
                }

                try {
                        if (!MapEditorIOUtils.restoreBackup(mapId)) {
                                pc.sendPackets(new S_SystemMessage("No backup exists for map " + mapId + "."));
                                return;
                        }
                } catch (IOException e) {
                        _log.error("Failed to restore map backup", e);
                        pc.sendPackets(new S_SystemMessage("Failed to restore backup: " + e.getMessage()));
                        return;
                }

                if (session != null && session.getMapId() == mapId) {
                        manager.cancelSession(pc);
                        pc.sendPackets(
                                        new S_SystemMessage("Editing session reset to match restored map state."));
                }
                refreshLiveMap(mapId);
                pc.sendPackets(new S_SystemMessage("Restored map " + mapId + " from backup."));
        }

        private void handleCancel(L1PcInstance pc, MapEditManager manager) {
                MapEditSession session = manager.getSession(pc);
                if (session == null) {
                        pc.sendPackets(new S_SystemMessage("No active map edit session."));
                        return;
                }
                manager.cancelSession(pc);
                pc.sendPackets(new S_SystemMessage("Map editing session cancelled."));
        }

        private MapEditSession requireSession(L1PcInstance pc, MapEditManager manager) {
                MapEditSession session = manager.getSession(pc);
                if (session == null) {
                        pc.sendPackets(new S_SystemMessage("No active map edit session. Use .map start first."));
                        return null;
                }
                return session;
        }

        private void sendHelp(L1PcInstance pc) {
                pc.sendPackets(new S_SystemMessage(
                                ".map <start|new|meta|brush|mode|apply|undo|redo|preview|save|commit|revert|cancel>"));
        }

        private ZoneSetting parseZone(String value) {
                if (value == null) {
                        return ZoneSetting.NONE;
                }
                switch (value.toLowerCase()) {
                case "normal":
                        return ZoneSetting.NORMAL;
                case "safety":
                case "safe":
                        return ZoneSetting.SAFETY;
                case "combat":
                case "war":
                        return ZoneSetting.COMBAT;
                case "none":
                default:
                        return ZoneSetting.NONE;
                }
        }

        private BrushMode parseMode(String value) {
                if (value == null) {
                        return BrushMode.SINGLE;
                }
                switch (value.toLowerCase()) {
                case "rectangle":
                case "square":
                        return BrushMode.RECTANGLE;
                case "radius":
                case "circle":
                        return BrushMode.RADIUS;
                case "fill":
                case "flood":
                        return BrushMode.FILL;
                case "single":
                default:
                        return BrushMode.SINGLE;
                }
        }

        private Map<String, String> parseKeyValueArgs(String[] parts, int startIndex) {
                Map<String, String> values = new HashMap<>();
                for (int i = startIndex; i < parts.length; i++) {
                        String token = parts[i];
                        if (token == null || token.isEmpty()) {
                                continue;
                        }
                        if (token.startsWith("+") || token.startsWith("-")) {
                                continue;
                        }
                        int idx = token.indexOf('=');
                        if (idx <= 0 || idx >= token.length() - 1) {
                                continue;
                        }
                        String key = token.substring(0, idx).toLowerCase(Locale.ENGLISH);
                        String value = token.substring(idx + 1);
                        values.put(key, value);
                }
                return values;
        }

        private Map<String, Boolean> parseBooleanFlags(String[] parts, int startIndex) {
                Map<String, Boolean> flags = new HashMap<>();
                for (int i = startIndex; i < parts.length; i++) {
                        String token = parts[i];
                        if (token == null || token.length() < 2) {
                                continue;
                        }
                        char prefix = token.charAt(0);
                        if (prefix != '+' && prefix != '-') {
                                continue;
                        }
                        String flagName = token.substring(1).toLowerCase(Locale.ENGLISH);
                        if (flagName.isEmpty()) {
                                continue;
                        }
                        flags.put(flagName, prefix == '+');
                }
                return flags;
        }

        private boolean getFlag(Map<String, Boolean> flags, String key, boolean defaultValue) {
                Boolean value = flags.get(key);
                return value != null ? value.booleanValue() : defaultValue;
        }

        private String getOption(Map<String, String> values, String... keys) {
                for (String key : keys) {
                        String value = values.get(key);
                        if (value != null) {
                                return value;
                        }
                }
                return null;
        }

        private boolean updateCoordinate(Map<String, String> values, MapMetadata metadata, String key) {
                String value = values.get(key);
                if (value == null) {
                        return false;
                }
                try {
                        int coordinate = Integer.parseInt(value);
                        switch (key) {
                        case "startx":
                                if (coordinate != metadata.getStartX()) {
                                        metadata.setStartX(coordinate);
                                        return true;
                                }
                                break;
                        case "endx":
                                if (coordinate != metadata.getEndX()) {
                                        metadata.setEndX(coordinate);
                                        return true;
                                }
                                break;
                        case "starty":
                                if (coordinate != metadata.getStartY()) {
                                        metadata.setStartY(coordinate);
                                        return true;
                                }
                                break;
                        case "endy":
                                if (coordinate != metadata.getEndY()) {
                                        metadata.setEndY(coordinate);
                                        return true;
                                }
                                break;
                        default:
                                break;
                        }
                        return false;
                } catch (NumberFormatException ex) {
                        String label;
                        switch (key) {
                        case "startx":
                                label = "startX";
                                break;
                        case "endx":
                                label = "endX";
                                break;
                        case "starty":
                                label = "startY";
                                break;
                        case "endy":
                                label = "endY";
                                break;
                        default:
                                label = key;
                                break;
                        }
                        throw new IllegalArgumentException("Invalid " + label + ": " + value, ex);
                }
        }

        private boolean applyFlag(Consumer<Boolean> setter, boolean currentValue, Map<String, Boolean> flags, String key) {
                Boolean value = flags.get(key);
                if (value == null) {
                        return false;
                }
                if (value.booleanValue() != currentValue) {
                        setter.accept(value);
                        return true;
                }
                return false;
        }

        private void refreshLiveMap(short mapId) {
                L1WorldMap.getInstance().reloadMap(mapId);
        }
}
