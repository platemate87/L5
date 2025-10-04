package l1j.server.server.datatables;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import l1j.server.L1DatabaseFactory;
import l1j.server.server.utils.SQLUtil;

public class MapIdsTableEditor {

        public static class MapMetadata {
                private final int mapId;
                private String locationName;
                private int startX;
                private int endX;
                private int startY;
                private int endY;
                private double monsterAmount = 1.0d;
                private double dropRate = 1.0d;
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

                public MapMetadata(int mapId) {
                        this.mapId = mapId;
                }

                public int getMapId() {
                        return mapId;
                }

                public String getLocationName() {
                        return locationName;
                }

                public void setLocationName(String locationName) {
                        this.locationName = locationName;
                }

                public int getStartX() {
                        return startX;
                }

                public void setStartX(int startX) {
                        this.startX = startX;
                }

                public int getEndX() {
                        return endX;
                }

                public void setEndX(int endX) {
                        this.endX = endX;
                }

                public int getStartY() {
                        return startY;
                }

                public void setStartY(int startY) {
                        this.startY = startY;
                }

                public int getEndY() {
                        return endY;
                }

                public void setEndY(int endY) {
                        this.endY = endY;
                }

                public double getMonsterAmount() {
                        return monsterAmount;
                }

                public void setMonsterAmount(double monsterAmount) {
                        this.monsterAmount = monsterAmount;
                }

                public double getDropRate() {
                        return dropRate;
                }

                public void setDropRate(double dropRate) {
                        this.dropRate = dropRate;
                }

                public boolean isUnderwater() {
                        return underwater;
                }

                public void setUnderwater(boolean underwater) {
                        this.underwater = underwater;
                }

                public boolean isMarkable() {
                        return markable;
                }

                public void setMarkable(boolean markable) {
                        this.markable = markable;
                }

                public boolean isTeleportable() {
                        return teleportable;
                }

                public void setTeleportable(boolean teleportable) {
                        this.teleportable = teleportable;
                }

                public boolean isEscapable() {
                        return escapable;
                }

                public void setEscapable(boolean escapable) {
                        this.escapable = escapable;
                }

                public boolean isUseResurrection() {
                        return useResurrection;
                }

                public void setUseResurrection(boolean useResurrection) {
                        this.useResurrection = useResurrection;
                }

                public boolean isUsePainwand() {
                        return usePainwand;
                }

                public void setUsePainwand(boolean usePainwand) {
                        this.usePainwand = usePainwand;
                }

                public boolean isEnabledDeathPenalty() {
                        return enabledDeathPenalty;
                }

                public void setEnabledDeathPenalty(boolean enabledDeathPenalty) {
                        this.enabledDeathPenalty = enabledDeathPenalty;
                }

                public boolean isTakePets() {
                        return takePets;
                }

                public void setTakePets(boolean takePets) {
                        this.takePets = takePets;
                }

                public boolean isRecallPets() {
                        return recallPets;
                }

                public void setRecallPets(boolean recallPets) {
                        this.recallPets = recallPets;
                }

                public boolean isUsableItem() {
                        return usableItem;
                }

                public void setUsableItem(boolean usableItem) {
                        this.usableItem = usableItem;
                }

                public boolean isUsableSkill() {
                        return usableSkill;
                }

                public void setUsableSkill(boolean usableSkill) {
                        this.usableSkill = usableSkill;
                }
        }

        private static final MapIdsTableEditor INSTANCE = new MapIdsTableEditor();

        private MapIdsTableEditor() {
        }

        public static MapIdsTableEditor getInstance() {
                        return INSTANCE;
        }

        public MapMetadata load(int mapId) {
                Connection con = null;
                PreparedStatement ps = null;
                ResultSet rs = null;
                try {
                        con = L1DatabaseFactory.getInstance().getConnection();
                        ps = con.prepareStatement("SELECT * FROM mapids WHERE mapid=?");
                        ps.setInt(1, mapId);
                        rs = ps.executeQuery();
                        if (!rs.next()) {
                                return null;
                        }
                        MapMetadata metadata = new MapMetadata(mapId);
                        metadata.setLocationName(rs.getString("locationname"));
                        metadata.setStartX(rs.getInt("startX"));
                        metadata.setEndX(rs.getInt("endX"));
                        metadata.setStartY(rs.getInt("startY"));
                        metadata.setEndY(rs.getInt("endY"));
                        metadata.setMonsterAmount(rs.getDouble("monster_amount"));
                        metadata.setDropRate(rs.getDouble("drop_rate"));
                        metadata.setUnderwater(rs.getBoolean("underwater"));
                        metadata.setMarkable(rs.getBoolean("markable"));
                        metadata.setTeleportable(rs.getBoolean("teleportable"));
                        metadata.setEscapable(rs.getBoolean("escapable"));
                        metadata.setUseResurrection(rs.getBoolean("resurrection"));
                        metadata.setUsePainwand(rs.getBoolean("painwand"));
                        metadata.setEnabledDeathPenalty(rs.getBoolean("penalty"));
                        metadata.setTakePets(rs.getBoolean("take_pets"));
                        metadata.setRecallPets(rs.getBoolean("recall_pets"));
                        metadata.setUsableItem(rs.getBoolean("usable_item"));
                        metadata.setUsableSkill(rs.getBoolean("usable_skill"));
                        return metadata;
                } catch (SQLException e) {
                        throw new RuntimeException("Unable to load map metadata for map " + mapId, e);
                } finally {
                        SQLUtil.close(rs);
                        SQLUtil.close(ps);
                        SQLUtil.close(con);
                }
        }

        public void save(MapMetadata metadata) {
                Connection con = null;
                PreparedStatement ps = null;
                try {
                        con = L1DatabaseFactory.getInstance().getConnection();
                        ps = con.prepareStatement(
                                        "UPDATE mapids SET locationname=?, startX=?, endX=?, startY=?, endY=?, monster_amount=?, drop_rate=?, underwater=?, markable=?, teleportable=?, escapable=?, resurrection=?, painwand=?, penalty=?, take_pets=?, recall_pets=?, usable_item=?, usable_skill=? WHERE mapid=?");
                        bindMetadata(ps, metadata, 1);
                        ps.setInt(19, metadata.getMapId());
                        int updated = ps.executeUpdate();
                        SQLUtil.close(ps);
                        if (updated == 0) {
                                ps = con.prepareStatement(
                                                "INSERT INTO mapids (mapid, locationname, startX, endX, startY, endY, monster_amount, drop_rate, underwater, markable, teleportable, escapable, resurrection, painwand, penalty, take_pets, recall_pets, usable_item, usable_skill) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
                                ps.setInt(1, metadata.getMapId());
                                bindMetadata(ps, metadata, 2);
                                ps.executeUpdate();
                        }
                } catch (SQLException e) {
                        throw new RuntimeException("Unable to persist map metadata for map " + metadata.getMapId(), e);
                } finally {
                        SQLUtil.close(ps);
                        SQLUtil.close(con);
                }
        }

        private void bindMetadata(PreparedStatement ps, MapMetadata metadata, int startIndex) throws SQLException {
                int i = startIndex;
                ps.setString(i++, metadata.getLocationName());
                ps.setInt(i++, metadata.getStartX());
                ps.setInt(i++, metadata.getEndX());
                ps.setInt(i++, metadata.getStartY());
                ps.setInt(i++, metadata.getEndY());
                ps.setDouble(i++, metadata.getMonsterAmount());
                ps.setDouble(i++, metadata.getDropRate());
                ps.setBoolean(i++, metadata.isUnderwater());
                ps.setBoolean(i++, metadata.isMarkable());
                ps.setBoolean(i++, metadata.isTeleportable());
                ps.setBoolean(i++, metadata.isEscapable());
                ps.setBoolean(i++, metadata.isUseResurrection());
                ps.setBoolean(i++, metadata.isUsePainwand());
                ps.setBoolean(i++, metadata.isEnabledDeathPenalty());
                ps.setBoolean(i++, metadata.isTakePets());
                ps.setBoolean(i++, metadata.isRecallPets());
                ps.setBoolean(i++, metadata.isUsableItem());
                ps.setBoolean(i++, metadata.isUsableSkill());
        }
}
