package tool.mapeditor.model;

import java.io.IOException;
import java.util.Arrays;

import l1j.server.server.model.map.L1Map;
import l1j.server.server.types.Point;

/**
 * Editable in-memory implementation of {@link L1Map} backed by the CSV layout
 * produced by {@code L1V1Map#toCsv()}.
 */
public class EditableL1Map extends L1Map {
    private static final byte BITFLAG_IS_IMPASSABLE = (byte) 0x80;

    private final int mapId;
    private final byte[][] tiles; // [x][y]
    private final int startX;
    private final int startY;

    private boolean underwater;
    private boolean markable;
    private boolean teleportable;
    private boolean escapable;
    private boolean useResurrection;
    private boolean usePainwand;
    private boolean enabledDeathPenalty;
    private boolean takePets;
    private boolean recallPets;
    private boolean usableItem;
    private boolean usableSkill;

    public EditableL1Map(int mapId, byte[][] tiles, int startX, int startY) {
        this(mapId, tiles, startX, startY, new MapAttributes());
    }

    public EditableL1Map(int mapId, byte[][] tiles, int startX, int startY, MapAttributes attributes) {
        this.mapId = mapId;
        this.tiles = tiles;
        this.startX = startX;
        this.startY = startY;
        this.underwater = attributes.isUnderwater();
        this.markable = attributes.isMarkable();
        this.teleportable = attributes.isTeleportable();
        this.escapable = attributes.isEscapable();
        this.useResurrection = attributes.isUseResurrection();
        this.usePainwand = attributes.isUsePainwand();
        this.enabledDeathPenalty = attributes.isEnabledDeathPenalty();
        this.takePets = attributes.isTakePets();
        this.recallPets = attributes.isRecallPets();
        this.usableItem = attributes.isUsableItem();
        this.usableSkill = attributes.isUsableSkill();
    }

    public EditableL1Map copy() {
        byte[][] clone = copyTiles();
        MapAttributes attributes = toAttributes();
        return new EditableL1Map(mapId, clone, startX, startY, attributes);
    }

    public MapAttributes toAttributes() {
        return new MapAttributes()
                .setUnderwater(underwater)
                .setMarkable(markable)
                .setTeleportable(teleportable)
                .setEscapable(escapable)
                .setUseResurrection(useResurrection)
                .setUsePainwand(usePainwand)
                .setEnabledDeathPenalty(enabledDeathPenalty)
                .setTakePets(takePets)
                .setRecallPets(recallPets)
                .setUsableItem(usableItem)
                .setUsableSkill(usableSkill);
    }

    public void overwriteTiles(byte[][] source) {
        for (int x = 0; x < tiles.length; x++) {
            System.arraycopy(source[x], 0, tiles[x], 0, tiles[x].length);
        }
    }

    public byte[][] copyTiles() {
        byte[][] clone = new byte[tiles.length][];
        for (int x = 0; x < tiles.length; x++) {
            clone[x] = Arrays.copyOf(tiles[x], tiles[x].length);
        }
        return clone;
    }

    private int toLocalX(int worldX) {
        return worldX - startX;
    }

    private int toLocalY(int worldY) {
        return worldY - startY;
    }

    private boolean isWithinLocal(int x, int y) {
        return x >= 0 && x < tiles.length && y >= 0 && y < tiles[0].length;
    }

    private int accessTile(int worldX, int worldY) {
        int x = toLocalX(worldX);
        int y = toLocalY(worldY);
        if (!isWithinLocal(x, y)) {
            return 0;
        }
        return tiles[x][y] & 0xFF;
    }

    private int accessOriginalTile(int worldX, int worldY) {
        return accessTile(worldX, worldY) & (~BITFLAG_IS_IMPASSABLE & 0xFF);
    }

    private void setTile(int worldX, int worldY, int value) {
        int x = toLocalX(worldX);
        int y = toLocalY(worldY);
        if (!isWithinLocal(x, y)) {
            return;
        }
        tiles[x][y] = (byte) value;
    }

    public void setZone(int worldX, int worldY, ZoneType zone) {
        int tile = accessOriginalTile(worldX, worldY);
        tile &= ~0x30;
        if (zone == ZoneType.SAFETY) {
            tile |= 0x10;
        } else if (zone == ZoneType.COMBAT) {
            tile |= 0x20;
        }
        setOriginalTile(worldX, worldY, (short) tile);
    }

    @Override
    public int getId() {
        return mapId;
    }

    @Override
    public int getX() {
        return startX;
    }

    @Override
    public int getY() {
        return startY;
    }

    @Override
    public int getWidth() {
        return tiles.length;
    }

    @Override
    public int getHeight() {
        return tiles.length == 0 ? 0 : tiles[0].length;
    }

    @Override
    public int getTile(int x, int y) {
        int tile = accessTile(x, y);
        if ((tile & BITFLAG_IS_IMPASSABLE) == BITFLAG_IS_IMPASSABLE) {
            return 300;
        }
        return accessOriginalTile(x, y);
    }

    @Override
    public int getOriginalTile(int x, int y) {
        return accessOriginalTile(x, y);
    }

    @Override
    public void setOriginalTile(int x, int y, short value) {
        int original = value & 0xFF;
        boolean wasImpassable = isImpassable(x, y);
        setTile(x, y, original);
        setPassable(x, y, !wasImpassable);
    }

    @Override
    public boolean isInMap(Point pt) {
        return isInMap(pt.getX(), pt.getY());
    }

    @Override
    public boolean isInMap(int x, int y) {
        int localX = toLocalX(x);
        int localY = toLocalY(y);
        return isWithinLocal(localX, localY);
    }

    @Override
    public boolean isPassable(Point pt) {
        return isPassable(pt.getX(), pt.getY());
    }

    @Override
    public boolean isPassable(int x, int y) {
        return isPassable(x, y - 1, 4) || isPassable(x + 1, y, 6) || isPassable(x, y + 1, 0)
                || isPassable(x - 1, y, 2);
    }

    @Override
    public boolean isPassable(Point pt, int heading) {
        return isPassable(pt.getX(), pt.getY(), heading);
    }

    @Override
    public boolean isPassable(int x, int y, int heading) {
        int tile1 = accessTile(x, y);
        int tile2;

        if (heading == 0) {
            tile2 = accessTile(x, y - 1);
        } else if (heading == 1) {
            tile2 = accessTile(x + 1, y - 1);
        } else if (heading == 2) {
            tile2 = accessTile(x + 1, y);
        } else if (heading == 3) {
            tile2 = accessTile(x + 1, y + 1);
        } else if (heading == 4) {
            tile2 = accessTile(x, y + 1);
        } else if (heading == 5) {
            tile2 = accessTile(x - 1, y + 1);
        } else if (heading == 6) {
            tile2 = accessTile(x - 1, y);
        } else if (heading == 7) {
            tile2 = accessTile(x - 1, y - 1);
        } else {
            return false;
        }

        if ((tile2 & BITFLAG_IS_IMPASSABLE) == BITFLAG_IS_IMPASSABLE) {
            return false;
        }

        if (heading == 0) {
            return (tile1 & 0x02) == 0x02;
        } else if (heading == 1) {
            int tile3 = accessTile(x, y - 1);
            int tile4 = accessTile(x + 1, y);
            return (tile3 & 0x01) == 0x01 || (tile4 & 0x02) == 0x02;
        } else if (heading == 2) {
            return (tile1 & 0x01) == 0x01;
        } else if (heading == 3) {
            int tile3 = accessTile(x, y + 1);
            return (tile3 & 0x01) == 0x01;
        } else if (heading == 4) {
            return (tile2 & 0x02) == 0x02;
        } else if (heading == 5) {
            return (tile2 & 0x01) == 0x01 || (tile2 & 0x02) == 0x02;
        } else if (heading == 6) {
            return (tile2 & 0x01) == 0x01;
        } else if (heading == 7) {
            int tile3 = accessTile(x - 1, y);
            return (tile3 & 0x02) == 0x02;
        }

        return false;
    }

    @Override
    public void setPassable(Point pt, boolean isPassable) {
        setPassable(pt.getX(), pt.getY(), isPassable);
    }

    @Override
    public void setPassable(int x, int y, boolean isPassable) {
        int tile = accessTile(x, y);
        if (isPassable) {
            tile &= ~BITFLAG_IS_IMPASSABLE;
        } else {
            tile |= BITFLAG_IS_IMPASSABLE;
        }
        setTile(x, y, tile);
    }

    @Override
    public boolean isSafetyZone(Point pt) {
        return isSafetyZone(pt.getX(), pt.getY());
    }

    @Override
    public boolean isSafetyZone(int x, int y) {
        int tile = accessOriginalTile(x, y);
        return (tile & 0x30) == 0x10;
    }

    @Override
    public boolean isCombatZone(Point pt) {
        return isCombatZone(pt.getX(), pt.getY());
    }

    @Override
    public boolean isCombatZone(int x, int y) {
        int tile = accessOriginalTile(x, y);
        return (tile & 0x30) == 0x20;
    }

    @Override
    public boolean isNormalZone(Point pt) {
        return isNormalZone(pt.getX(), pt.getY());
    }

    @Override
    public boolean isNormalZone(int x, int y) {
        int tile = accessOriginalTile(x, y);
        return (tile & 0x30) == 0x00;
    }

    @Override
    public boolean isArrowPassable(Point pt) {
        return isArrowPassable(pt.getX(), pt.getY());
    }

    @Override
    public boolean isArrowPassable(int x, int y) {
        return isArrowPassable(x, y - 1, 4) || isArrowPassable(x + 1, y, 6) || isArrowPassable(x, y + 1, 0)
                || isArrowPassable(x - 1, y, 2);
    }

    @Override
    public boolean isArrowPassable(Point pt, int heading) {
        return isArrowPassable(pt.getX(), pt.getY(), heading);
    }

    @Override
    public boolean isArrowPassable(int x, int y, int heading) {
        int tile1 = accessTile(x, y);
        int tile2;

        if (heading == 0) {
            tile2 = accessTile(x, y - 1);
        } else if (heading == 1) {
            tile2 = accessTile(x + 1, y - 1);
        } else if (heading == 2) {
            tile2 = accessTile(x + 1, y);
        } else if (heading == 3) {
            tile2 = accessTile(x + 1, y + 1);
        } else if (heading == 4) {
            tile2 = accessTile(x, y + 1);
        } else if (heading == 5) {
            tile2 = accessTile(x - 1, y + 1);
        } else if (heading == 6) {
            tile2 = accessTile(x - 1, y);
        } else if (heading == 7) {
            tile2 = accessTile(x - 1, y - 1);
        } else {
            return false;
        }

        if ((tile2 & BITFLAG_IS_IMPASSABLE) == BITFLAG_IS_IMPASSABLE) {
            return false;
        }

        if (heading == 0) {
            return (tile1 & 0x02) == 0x02;
        } else if (heading == 1) {
            int tile3 = accessTile(x, y - 1);
            int tile4 = accessTile(x + 1, y);
            return (tile3 & 0x01) == 0x01 || (tile4 & 0x02) == 0x02;
        } else if (heading == 2) {
            return (tile1 & 0x01) == 0x01;
        } else if (heading == 3) {
            int tile3 = accessTile(x, y + 1);
            return (tile3 & 0x01) == 0x01;
        } else if (heading == 4) {
            return (tile2 & 0x02) == 0x02;
        } else if (heading == 5) {
            return (tile2 & 0x01) == 0x01 || (tile2 & 0x02) == 0x02;
        } else if (heading == 6) {
            return (tile2 & 0x01) == 0x01;
        } else if (heading == 7) {
            int tile3 = accessTile(x - 1, y);
            return (tile3 & 0x02) == 0x02;
        }

        return false;
    }

    @Override
    public boolean isUnderwater() {
        return underwater;
    }

    public void setUnderwater(boolean underwater) {
        this.underwater = underwater;
    }

    @Override
    public boolean isMarkable() {
        return markable;
    }

    public void setMarkable(boolean markable) {
        this.markable = markable;
    }

    @Override
    public boolean isTeleportable() {
        return teleportable;
    }

    public void setTeleportable(boolean teleportable) {
        this.teleportable = teleportable;
    }

    @Override
    public boolean isEscapable() {
        return escapable;
    }

    public void setEscapable(boolean escapable) {
        this.escapable = escapable;
    }

    @Override
    public boolean isUseResurrection() {
        return useResurrection;
    }

    public void setUseResurrection(boolean useResurrection) {
        this.useResurrection = useResurrection;
    }

    @Override
    public boolean isUsePainwand() {
        return usePainwand;
    }

    public void setUsePainwand(boolean usePainwand) {
        this.usePainwand = usePainwand;
    }

    @Override
    public boolean isEnabledDeathPenalty() {
        return enabledDeathPenalty;
    }

    public void setEnabledDeathPenalty(boolean enabledDeathPenalty) {
        this.enabledDeathPenalty = enabledDeathPenalty;
    }

    @Override
    public boolean isTakePets() {
        return takePets;
    }

    public void setTakePets(boolean takePets) {
        this.takePets = takePets;
    }

    @Override
    public boolean isRecallPets() {
        return recallPets;
    }

    public void setRecallPets(boolean recallPets) {
        this.recallPets = recallPets;
    }

    @Override
    public boolean isUsableItem() {
        return usableItem;
    }

    public void setUsableItem(boolean usableItem) {
        this.usableItem = usableItem;
    }

    @Override
    public boolean isUsableSkill() {
        return usableSkill;
    }

    public void setUsableSkill(boolean usableSkill) {
        this.usableSkill = usableSkill;
    }

    @Override
    public boolean isFishingZone(int x, int y) {
        return accessOriginalTile(x, y) == 16;
    }

    @Override
    public String toCsv() throws IOException {
        StringBuilder builder = new StringBuilder();
        int width = getWidth();
        int height = getHeight();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                builder.append(tiles[x][y] & 0xFF);
                if (x < width - 1) {
                    builder.append(',');
                }
            }
            builder.append('\n');
        }
        return builder.toString();
    }

    @Override
    public String toString(Point pt) {
        int x = pt.getX();
        int y = pt.getY();
        StringBuilder sb = new StringBuilder();
        sb.append("(x=").append(x).append(", y=").append(y).append(") ");
        sb.append("tile=").append(getOriginalTile(x, y));
        sb.append(", passable=").append(isPassable(x, y));
        if (isSafetyZone(x, y)) {
            sb.append(", safety zone");
        } else if (isCombatZone(x, y)) {
            sb.append(", combat zone");
        } else {
            sb.append(", normal zone");
        }
        return sb.toString();
    }

    public byte getRawTile(int worldX, int worldY) {
        return tiles[toLocalX(worldX)][toLocalY(worldY)];
    }

    public boolean isImpassable(int worldX, int worldY) {
        int tile = accessTile(worldX, worldY);
        return (tile & BITFLAG_IS_IMPASSABLE) == BITFLAG_IS_IMPASSABLE;
    }
}
