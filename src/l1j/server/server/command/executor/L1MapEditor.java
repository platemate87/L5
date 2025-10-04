package l1j.server.server.command.executor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.model.map.edit.MapEditManager;
import l1j.server.server.model.map.edit.MapEditOperation;
import l1j.server.server.model.map.edit.MapEditSession;
import l1j.server.server.model.map.edit.MapEditSession.BrushMode;
import l1j.server.server.model.map.edit.MapEditSession.TileChangeSummary;
import l1j.server.server.model.map.edit.MapEditSession.ZoneSetting;
import l1j.server.server.serverpackets.S_SystemMessage;

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
                                handleCommit(pc, manager);
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

        private void handleCommit(L1PcInstance pc, MapEditManager manager) {
                MapEditSession session = requireSession(pc, manager);
                if (session == null) {
                        return;
                }
                if (!session.hasPendingChanges()) {
                        pc.sendPackets(new S_SystemMessage("No pending edits to commit."));
                        return;
                }
                try {
                        int committed = session.commit();
                        pc.sendPackets(new S_SystemMessage("Committed " + committed + " tile(s) to map " + session.getMapId() + "."));
                } catch (IOException e) {
                        _log.error("Failed to commit map edits", e);
                        pc.sendPackets(new S_SystemMessage("Failed to commit edits: " + e.getMessage()));
                }
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
                                ".map <start|brush|mode|apply|undo|redo|preview|commit|cancel>"));
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
}
