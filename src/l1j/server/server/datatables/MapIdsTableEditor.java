package l1j.server.server.datatables;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l1j.server.L1DatabaseFactory;
import l1j.server.server.utils.SQLUtil;

public final class MapIdsTableEditor {
        private static final Logger _log = LoggerFactory.getLogger(MapIdsTableEditor.class);

        private static final Map<String, String> COLUMN_ALIASES;

        static {
                Map<String, String> aliases = new LinkedHashMap<>();
                aliases.put("underwater", "underwater");
                aliases.put("markable", "markable");
                aliases.put("teleportable", "teleportable");
                aliases.put("escapable", "escapable");
                aliases.put("resurrection", "resurrection");
                aliases.put("painwand", "painwand");
                aliases.put("penalty", "penalty");
                aliases.put("take_pets", "take_pets");
                aliases.put("recall_pets", "recall_pets");
                aliases.put("usable_item", "usable_item");
                aliases.put("usable_skill", "usable_skill");
                COLUMN_ALIASES = Collections.unmodifiableMap(aliases);
        }

        private MapIdsTableEditor() {
        }

        public static Map<String, Boolean> createDefaultBooleanFlags() {
                Map<String, Boolean> defaults = new LinkedHashMap<>();
                for (String column : COLUMN_ALIASES.values()) {
                        defaults.put(column, Boolean.FALSE);
                }
                return defaults;
        }

        public static boolean isBooleanColumn(String column) {
                return COLUMN_ALIASES.containsKey(column.toLowerCase(Locale.ENGLISH));
        }

        public static Set<String> getSupportedBooleanColumns() {
                return COLUMN_ALIASES.keySet();
        }

        public static String normalizeColumnName(String column) {
                if (column == null) {
                        return null;
                }
                return COLUMN_ALIASES.get(column.toLowerCase(Locale.ENGLISH));
        }

        public static void upsertMapRecord(int mapId, int startX, int endX, int startY, int endY,
                        Map<String, Boolean> booleanFlags, String locationName) throws SQLException {
                Map<String, Boolean> flags = createDefaultBooleanFlags();
                if (booleanFlags != null) {
                        for (Map.Entry<String, Boolean> entry : booleanFlags.entrySet()) {
                                String normalized = normalizeColumnName(entry.getKey());
                                if (normalized != null && entry.getValue() != null) {
                                        flags.put(normalized, entry.getValue());
                                }
                        }
                }

                Connection con = null;
                PreparedStatement pstm = null;
                try {
                        con = L1DatabaseFactory.getInstance().getConnection();
                        pstm = con.prepareStatement(
                                        "INSERT INTO mapids (mapid, locationname, startX, endX, startY, endY, monster_amount, drop_rate, unique_rate, underwater, markable, teleportable, escapable, resurrection, painwand, penalty, take_pets, recall_pets, usable_item, usable_skill) "
                                                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
                                                        + "ON DUPLICATE KEY UPDATE locationname = VALUES(locationname), startX = VALUES(startX), endX = VALUES(endX), startY = VALUES(startY), endY = VALUES(endY), monster_amount = VALUES(monster_amount), drop_rate = VALUES(drop_rate), unique_rate = VALUES(unique_rate), underwater = VALUES(underwater), markable = VALUES(markable), teleportable = VALUES(teleportable), escapable = VALUES(escapable), resurrection = VALUES(resurrection), painwand = VALUES(painwand), penalty = VALUES(penalty), take_pets = VALUES(take_pets), recall_pets = VALUES(recall_pets), usable_item = VALUES(usable_item), usable_skill = VALUES(usable_skill)");
                        int idx = 1;
                        pstm.setInt(idx++, mapId);
                        pstm.setString(idx++, locationName);
                        pstm.setInt(idx++, startX);
                        pstm.setInt(idx++, endX);
                        pstm.setInt(idx++, startY);
                        pstm.setInt(idx++, endY);
                        pstm.setDouble(idx++, 1d);
                        pstm.setDouble(idx++, 1d);
                        pstm.setInt(idx++, 1);
                        for (String alias : COLUMN_ALIASES.keySet()) {
                                String column = COLUMN_ALIASES.get(alias);
                                boolean value = Boolean.TRUE.equals(flags.get(column));
                                pstm.setBoolean(idx++, value);
                        }
                        pstm.executeUpdate();
                } finally {
                        SQLUtil.close(pstm);
                        SQLUtil.close(con);
                }
        }

        public static boolean updateBooleanFlag(int mapId, String column, boolean value) {
                String normalized = normalizeColumnName(column);
                if (normalized == null) {
                        return false;
                }
                Connection con = null;
                PreparedStatement pstm = null;
                try {
                        con = L1DatabaseFactory.getInstance().getConnection();
                        pstm = con.prepareStatement("UPDATE mapids SET " + normalized + " = ? WHERE mapid = ?");
                        pstm.setBoolean(1, value);
                        pstm.setInt(2, mapId);
                        return pstm.executeUpdate() > 0;
                } catch (SQLException e) {
                        _log.error(e.getLocalizedMessage(), e);
                        return false;
                } finally {
                        SQLUtil.close(pstm);
                        SQLUtil.close(con);
                }
        }

        public static boolean mapExists(int mapId) {
                Connection con = null;
                PreparedStatement pstm = null;
                ResultSet rs = null;
                try {
                        con = L1DatabaseFactory.getInstance().getConnection();
                        pstm = con.prepareStatement("SELECT 1 FROM mapids WHERE mapid = ?");
                        pstm.setInt(1, mapId);
                        rs = pstm.executeQuery();
                        return rs.next();
                } catch (SQLException e) {
                        _log.error(e.getLocalizedMessage(), e);
                } finally {
                        SQLUtil.close(rs);
                        SQLUtil.close(pstm);
                        SQLUtil.close(con);
                }
                return false;
        }
}
