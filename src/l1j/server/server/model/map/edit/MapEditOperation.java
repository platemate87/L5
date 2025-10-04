package l1j.server.server.model.map.edit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import l1j.server.server.model.map.L1V1Map;

/**
 * Represents a single batch of tile modifications performed by the map editor.
 * Each operation contains one or more tile changes which can be undone or
 * redone atomically.
 */
public class MapEditOperation {

        private final MapEditSession.BrushMode mode;
        private final List<MapTileChange> changes = new ArrayList<>();

        public MapEditOperation(MapEditSession.BrushMode mode) {
                this.mode = mode;
        }

        public MapEditSession.BrushMode getMode() {
                return mode;
        }

        public void addChange(MapTileChange change) {
                if (change != null) {
                        changes.add(change);
                }
        }

        public boolean isEmpty() {
                return changes.isEmpty();
        }

        public int getChangeCount() {
                return changes.size();
        }

        public List<MapTileChange> getChanges() {
                return Collections.unmodifiableList(changes);
        }

        public void applyTo(L1V1Map map) {
                for (MapTileChange change : changes) {
                        map.setOriginalTile(change.getX(), change.getY(), change.getAfterTile());
                        map.setPassable(change.getX(), change.getY(), change.getAfterPassable());
                }
        }

        public void revert(L1V1Map map) {
                for (MapTileChange change : changes) {
                        map.setOriginalTile(change.getX(), change.getY(), change.getBeforeTile());
                        map.setPassable(change.getX(), change.getY(), change.getBeforePassable());
                }
        }

        /**
         * A single tile modification that stores the before and after state so
         * changes can be reverted or re-applied.
         */
        public static class MapTileChange {
                private final int x;
                private final int y;
                private final short beforeTile;
                private final short afterTile;
                private final boolean beforePassable;
                private final boolean afterPassable;

                public MapTileChange(int x, int y, short beforeTile, short afterTile, boolean beforePassable,
                                boolean afterPassable) {
                        this.x = x;
                        this.y = y;
                        this.beforeTile = beforeTile;
                        this.afterTile = afterTile;
                        this.beforePassable = beforePassable;
                        this.afterPassable = afterPassable;
                }

                public int getX() {
                        return x;
                }

                public int getY() {
                        return y;
                }

                public short getBeforeTile() {
                        return beforeTile;
                }

                public short getAfterTile() {
                        return afterTile;
                }

                public boolean getBeforePassable() {
                        return beforePassable;
                }

                public boolean getAfterPassable() {
                        return afterPassable;
                }

                public boolean isTileChanged() {
                        return beforeTile != afterTile;
                }

                public boolean isPassableChanged() {
                        return beforePassable != afterPassable;
                }
        }
}
