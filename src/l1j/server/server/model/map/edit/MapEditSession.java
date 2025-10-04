package l1j.server.server.model.map.edit;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

import l1j.server.server.model.map.L1Map;
import l1j.server.server.model.map.L1V1Map;
import l1j.server.server.utils.FileUtil;

/**
 * Holds the editing state for a GM that is actively modifying a map. Sessions
 * track brush configuration, pending tile changes, and undo/redo history.
 */
public class MapEditSession {

        private static final int ZONE_MASK = 0x30;
        private static final int IMPASSABLE_BIT = 0x80;
        private static final int[][] FLOOD_DIRECTIONS = new int[][] { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };

        public enum BrushMode {
                SINGLE, RECTANGLE, RADIUS, FILL
        }

        public enum ZoneSetting {
                NONE, NORMAL, SAFETY, COMBAT
        }

        public static class TileChangeSummary {
                private final int x;
                private final int y;
                private final short originalTile;
                private final boolean originalPassable;
                private short currentTile;
                private boolean currentPassable;

                private TileChangeSummary(int x, int y, short originalTile, boolean originalPassable) {
                        this.x = x;
                        this.y = y;
                        this.originalTile = originalTile;
                        this.originalPassable = originalPassable;
                        this.currentTile = originalTile;
                        this.currentPassable = originalPassable;
                }

                private void setCurrent(short tile, boolean passable) {
                        this.currentTile = tile;
                        this.currentPassable = passable;
                }

                public int getX() {
                        return x;
                }

                public int getY() {
                        return y;
                }

                public short getOriginalTile() {
                        return originalTile;
                }

                public boolean isOriginalPassable() {
                        return originalPassable;
                }

                public short getCurrentTile() {
                        return currentTile;
                }

                public boolean isCurrentPassable() {
                        return currentPassable;
                }

                public boolean hasTileChange() {
                        return originalTile != currentTile;
                }

                public boolean hasPassableChange() {
                        return originalPassable != currentPassable;
                }
        }

        private static class TileKey {
                private final int x;
                private final int y;

                private TileKey(int x, int y) {
                        this.x = x;
                        this.y = y;
                }

                @Override
                public int hashCode() {
                        return Objects.hash(x, y);
                }

                @Override
                public boolean equals(Object obj) {
                        if (this == obj) {
                                return true;
                        }
                        if (!(obj instanceof TileKey)) {
                                return false;
                        }
                        TileKey other = (TileKey) obj;
                        return x == other.x && y == other.y;
                }
        }

        private final short mapId;
        private final L1V1Map sourceMap;
        private L1V1Map workingMap;
        private final Map<TileKey, TileChangeSummary> pendingChanges = new HashMap<>();
        private final Deque<MapEditOperation> undoStack = new ArrayDeque<>();
        private final Deque<MapEditOperation> redoStack = new ArrayDeque<>();

        private BrushMode mode = BrushMode.SINGLE;
        private int brushSize = 1;
        private Short brushTileId = null;
        private ZoneSetting zoneSetting = ZoneSetting.NONE;
        private Boolean passableOverride = null;

        public MapEditSession(short mapId, L1V1Map sourceMap) {
                this.mapId = mapId;
                this.sourceMap = sourceMap;
                this.workingMap = new L1V1Map(sourceMap);
        }

        public short getMapId() {
                return mapId;
        }

        public BrushMode getMode() {
                return mode;
        }

        public void setMode(BrushMode mode) {
                this.mode = mode;
        }

        public int getBrushSize() {
                return brushSize;
        }

        public void setBrushSize(int brushSize) {
                this.brushSize = Math.max(1, brushSize);
        }

        public Short getBrushTileId() {
                return brushTileId;
        }

        public void setBrushTileId(Short brushTileId) {
                this.brushTileId = brushTileId;
        }

        public ZoneSetting getZoneSetting() {
                return zoneSetting;
        }

        public void setZoneSetting(ZoneSetting zoneSetting) {
                this.zoneSetting = zoneSetting;
        }

        public Boolean getPassableOverride() {
                return passableOverride;
        }

        public void setPassableOverride(Boolean passableOverride) {
                this.passableOverride = passableOverride;
        }

        public void clearBrushTile() {
                this.brushTileId = null;
        }

        public void clearPassableOverride() {
                this.passableOverride = null;
        }

        public void clearZoneSetting() {
                this.zoneSetting = ZoneSetting.NONE;
        }

        public String describeBrush() {
                StringBuilder sb = new StringBuilder();
                sb.append("Mode=").append(mode.name().toLowerCase());
                sb.append(", Size=").append(brushSize);
                sb.append(", Tile=").append(brushTileId == null ? "unchanged" : brushTileId);
                sb.append(", Zone=").append(zoneSetting.name().toLowerCase());
                sb.append(", Passable=");
                if (passableOverride == null) {
                        sb.append("unchanged");
                } else {
                        sb.append(passableOverride ? "passable" : "blocked");
                }
                return sb.toString();
        }

        public MapEditOperation applyBrush(int baseX, int baseY) {
                MapEditOperation operation = new MapEditOperation(mode);
                switch (mode) {
                case SINGLE:
                        applySingle(baseX, baseY, operation);
                        break;
                case RECTANGLE:
                        applyRectangle(baseX, baseY, operation);
                        break;
                case RADIUS:
                        applyRadius(baseX, baseY, operation);
                        break;
                case FILL:
                        applyFloodFill(baseX, baseY, operation);
                        break;
                }

                if (operation.isEmpty()) {
                        return null;
                }

                undoStack.push(operation);
                redoStack.clear();
                return operation;
        }

        private void applySingle(int x, int y, MapEditOperation operation) {
                MapEditOperation.MapTileChange change = applyTile(x, y);
                if (change != null) {
                        operation.addChange(change);
                }
        }

        private void applyRectangle(int centerX, int centerY, MapEditOperation operation) {
                int radius = Math.max(0, brushSize - 1);
                for (int dx = -radius; dx <= radius; dx++) {
                        for (int dy = -radius; dy <= radius; dy++) {
                                MapEditOperation.MapTileChange change = applyTile(centerX + dx, centerY + dy);
                                if (change != null) {
                                        operation.addChange(change);
                                }
                        }
                }
        }

        private void applyRadius(int centerX, int centerY, MapEditOperation operation) {
                int radius = Math.max(0, brushSize - 1);
                int radiusSquared = radius * radius;
                for (int dx = -radius; dx <= radius; dx++) {
                        for (int dy = -radius; dy <= radius; dy++) {
                                if (dx * dx + dy * dy > radiusSquared) {
                                        continue;
                                }
                                MapEditOperation.MapTileChange change = applyTile(centerX + dx, centerY + dy);
                                if (change != null) {
                                        operation.addChange(change);
                                }
                        }
                }
        }

        private void applyFloodFill(int startX, int startY, MapEditOperation operation) {
                if (!workingMap.isInMap(startX, startY)) {
                        return;
                }

                short targetTile = (short) workingMap.getOriginalTile(startX, startY);
                int radius = Math.max(0, brushSize - 1);

                Queue<TileKey> queue = new ArrayDeque<>();
                Set<TileKey> visited = new HashSet<>();
                TileKey start = new TileKey(startX, startY);
                queue.add(start);
                visited.add(start);

                while (!queue.isEmpty()) {
                        TileKey key = queue.poll();
                        short currentTile = (short) workingMap.getOriginalTile(key.x, key.y);
                        if (currentTile != targetTile) {
                                continue;
                        }
                        MapEditOperation.MapTileChange change = applyTile(key.x, key.y);
                        if (change != null) {
                                operation.addChange(change);
                        }

                        for (int[] direction : FLOOD_DIRECTIONS) {
                                int nextX = key.x + direction[0];
                                int nextY = key.y + direction[1];
                                TileKey nextKey = new TileKey(nextX, nextY);
                                if (visited.contains(nextKey)) {
                                        continue;
                                }
                                if (!workingMap.isInMap(nextX, nextY)) {
                                        continue;
                                }
                                if (radius > 0 && (Math.abs(nextX - startX) > radius || Math.abs(nextY - startY) > radius)) {
                                        continue;
                                }
                                if ((short) workingMap.getOriginalTile(nextX, nextY) != targetTile) {
                                        continue;
                                }
                                visited.add(nextKey);
                                queue.add(nextKey);
                        }
                }
        }

        private MapEditOperation.MapTileChange applyTile(int x, int y) {
                if (!workingMap.isInMap(x, y)) {
                        return null;
                }
                int localX = x - workingMap.getX();
                int localY = y - workingMap.getY();
                byte[][] tiles = workingMap.getRawTiles();
                if (localX < 0 || localX >= tiles.length) {
                        return null;
                }
                if (localY < 0 || localY >= tiles[localX].length) {
                        return null;
                }

                short beforeTile = (short) workingMap.getOriginalTile(x, y);
                boolean beforePassable = (tiles[localX][localY] & IMPASSABLE_BIT) == 0;

                short newTile = beforeTile;
                if (brushTileId != null) {
                        newTile = brushTileId;
                }
                newTile = applyZoneSetting(newTile);
                boolean newPassable = passableOverride != null ? passableOverride.booleanValue() : beforePassable;

                boolean tileChanged = newTile != beforeTile;
                boolean passableChanged = newPassable != beforePassable;

                if (!tileChanged && !passableChanged) {
                        return null;
                }

                workingMap.setOriginalTile(x, y, newTile);
                workingMap.setPassable(x, y, newPassable);

                MapEditOperation.MapTileChange change = new MapEditOperation.MapTileChange(x, y, beforeTile, newTile,
                                beforePassable, newPassable);
                updatePendingChange(x, y, change.getAfterTile(), change.getAfterPassable());
                return change;
        }

        private short applyZoneSetting(short tile) {
                switch (zoneSetting) {
                case NORMAL:
                        return (short) (tile & ~ZONE_MASK);
                case SAFETY:
                        return (short) ((tile & ~ZONE_MASK) | 0x10);
                case COMBAT:
                        return (short) ((tile & ~ZONE_MASK) | 0x20);
                case NONE:
                default:
                        return tile;
                }
        }

        public MapEditOperation undo() {
                if (undoStack.isEmpty()) {
                        return null;
                }
                MapEditOperation operation = undoStack.pop();
                operation.revert(workingMap);
                for (MapEditOperation.MapTileChange change : operation.getChanges()) {
                        updatePendingChange(change.getX(), change.getY(), change.getBeforeTile(), change.getBeforePassable());
                }
                redoStack.push(operation);
                return operation;
        }

        public MapEditOperation redo() {
                if (redoStack.isEmpty()) {
                        return null;
                }
                MapEditOperation operation = redoStack.pop();
                operation.applyTo(workingMap);
                for (MapEditOperation.MapTileChange change : operation.getChanges()) {
                        updatePendingChange(change.getX(), change.getY(), change.getAfterTile(), change.getAfterPassable());
                }
                undoStack.push(operation);
                return operation;
        }

        public void resetWorkingCopy() {
                this.workingMap = new L1V1Map(sourceMap);
                pendingChanges.clear();
                undoStack.clear();
                redoStack.clear();
        }

        public boolean hasPendingChanges() {
                return !pendingChanges.isEmpty();
        }

        public int getPendingChangeCount() {
                return pendingChanges.size();
        }

        public Collection<TileChangeSummary> getPendingChanges() {
                return Collections.unmodifiableCollection(pendingChanges.values());
        }

        public int commit() throws IOException {
                if (pendingChanges.isEmpty()) {
                        return 0;
                }
                int changedTiles = pendingChanges.size();
                for (TileChangeSummary summary : new ArrayList<>(pendingChanges.values())) {
                        sourceMap.setOriginalTile(summary.getX(), summary.getY(), summary.getCurrentTile());
                        sourceMap.setPassable(summary.getX(), summary.getY(), summary.isCurrentPassable());
                }

                String mapFilename = "./maps/" + mapId + ".txt";
                File mapFile = new File(mapFilename);
                if (mapFile.exists()) {
                        File backupFile = new File(mapFilename + ".bak");
                        FileUtil.copyFileUsingStream(mapFile, backupFile);
                }
                FileUtil.writeFile(mapFilename, sourceMap.toCsv());

                resetWorkingCopy();
                return changedTiles;
        }

        private void updatePendingChange(int x, int y, short currentTile, boolean currentPassable) {
                TileKey key = new TileKey(x, y);
                short originalTile = (short) sourceMap.getOriginalTile(x, y);
                boolean originalPassable = isPassable(sourceMap, x, y);
                if (currentTile == originalTile && currentPassable == originalPassable) {
                        pendingChanges.remove(key);
                        return;
                }
                TileChangeSummary summary = pendingChanges.get(key);
                if (summary == null) {
                        summary = new TileChangeSummary(x, y, originalTile, originalPassable);
                        pendingChanges.put(key, summary);
                }
                summary.setCurrent(currentTile, currentPassable);
        }

        private boolean isPassable(L1Map map, int x, int y) {
                if (!(map instanceof L1V1Map)) {
                        return map.isPassable(x, y);
                }
                L1V1Map v1Map = (L1V1Map) map;
                if (!v1Map.isInMap(x, y)) {
                        return false;
                }
                int localX = x - v1Map.getX();
                int localY = y - v1Map.getY();
                byte[][] tiles = v1Map.getRawTiles();
                if (localX < 0 || localX >= tiles.length) {
                        return false;
                }
                if (localY < 0 || localY >= tiles[localX].length) {
                        return false;
                }
                return (tiles[localX][localY] & IMPASSABLE_BIT) == 0;
        }
}
