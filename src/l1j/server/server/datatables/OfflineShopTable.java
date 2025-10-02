package l1j.server.server.datatables;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l1j.server.L1DatabaseFactory;
import l1j.server.server.ActionCodes;
import l1j.server.server.model.L1World;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.serverpackets.S_DoActionShop;
import l1j.server.server.templates.L1PrivateShopBuyList;
import l1j.server.server.templates.L1PrivateShopSellList;
import l1j.server.server.utils.SQLUtil;

public class OfflineShopTable {

        private static final Logger _log = LoggerFactory.getLogger(OfflineShopTable.class);

        private static OfflineShopTable _instance = new OfflineShopTable();

        private final Map<Integer, L1PcInstance> _offlineShops = new ConcurrentHashMap<>();

        private OfflineShopTable() {
        }

        public static OfflineShopTable getInstance() {
                return _instance;
        }

        public void load() {
                Connection con = null;
                PreparedStatement pstm = null;
                ResultSet rs = null;
                int count = 0;

                try {
                        con = L1DatabaseFactory.getInstance().getConnection();
                        pstm = con.prepareStatement("SELECT * FROM character_offline_shop");
                        rs = pstm.executeQuery();

                        while (rs.next()) {
                                String charName = rs.getString("char_name");
                                try {
                                        L1PcInstance pc = L1PcInstance.load(charName);
                                        if (pc == null) {
                                                continue;
                                        }

                                        CharacterTable.getInstance().restoreInventory(pc);

                                        pc.setX(rs.getInt("locx"));
                                        pc.setY(rs.getInt("locy"));
                                        pc.setMap((short) rs.getInt("map_id"));
                                        pc.setHeading(rs.getInt("heading"));
                                        pc.setShopChat(rs.getBytes("shop_chat"));
                                        pc.setLastKnownClientIp(rs.getString("client_ip"));

                                        restoreSellList(pc, rs.getString("sell_list"));
                                        restoreBuyList(pc, rs.getString("buy_list"));

                                        pc.setPrivateShop(true);
                                        pc.setOfflineShop(true);
                                        pc.setTradingInPrivateShop(false);
                                        pc.setNetConnection(null);
                                        pc.setPacketOutput(null);
                                        pc.stopHpRegeneration();
                                        pc.stopMpRegeneration();
                                        pc.stopEtcMonitor();
                                        pc.setOnlineStatus(0);
                                        CharacterTable.updateOnlineStatus(pc);

                                        L1World world = L1World.getInstance();
                                        world.storeObject(pc);
                                        world.addVisibleObject(pc);
                                        byte[] chat = pc.getShopChat();
                                        if (chat == null) {
                                                chat = new byte[0];
                                        }
                                        pc.broadcastPacket(new S_DoActionShop(pc.getId(), ActionCodes.ACTION_Shop, chat));

                                        _offlineShops.put(pc.getId(), pc);
                                        count++;
                                } catch (Exception e) {
                                        _log.error(String.format("Failed to load offline shop for %s", charName), e);
                                }
                        }
                } catch (SQLException e) {
                        _log.error("Failed to load offline shops", e);
                } finally {
                        SQLUtil.close(rs);
                        SQLUtil.close(pstm);
                        SQLUtil.close(con);
                }

                if (count > 0) {
                        _log.info(String.format("Loaded %d offline private shops.", count));
                }
        }

        public void storeOfflineShop(L1PcInstance pc) {
                if (pc == null) {
                        return;
                }

                writeOfflineShop(pc);
                pc.setOfflineShop(true);
                _offlineShops.put(pc.getId(), pc);
        }

        public void updateOfflineShop(L1PcInstance pc) {
                if (pc == null || !_offlineShops.containsKey(pc.getId())) {
                        return;
                }

                writeOfflineShop(pc);
        }

        public void removeOfflineShop(int objId) {
                L1PcInstance shopPc = _offlineShops.remove(objId);
                if (shopPc != null) {
                        shopPc.setOfflineShop(false);
                        shopPc.setPrivateShop(false);
                        L1World world = L1World.getInstance();
                        world.removeVisibleObject(shopPc);
                        world.removeObject(shopPc);
                }

                deleteOfflineShop(objId);
        }

        private void writeOfflineShop(L1PcInstance pc) {
                Connection con = null;
                PreparedStatement pstm = null;

                try {
                        con = L1DatabaseFactory.getInstance().getConnection();
                        pstm = con.prepareStatement(
                                        "REPLACE INTO character_offline_shop (char_id, char_name, map_id, locx, locy, heading, shop_chat, sell_list, buy_list, client_ip) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                        pstm.setInt(1, pc.getId());
                        pstm.setString(2, pc.getName());
                        pstm.setInt(3, pc.getMapId());
                        pstm.setInt(4, pc.getX());
                        pstm.setInt(5, pc.getY());
                        pstm.setInt(6, pc.getHeading());
                        pstm.setBytes(7, pc.getShopChat());
                        pstm.setString(8, serializeSellList(pc.getSellList()));
                        pstm.setString(9, serializeBuyList(pc.getBuyList()));
                        pstm.setString(10, pc.getIpForLogging());
                        pstm.execute();
                } catch (SQLException e) {
                        _log.error(String.format("Failed to store offline shop for %s", pc.getName()), e);
                } finally {
                        SQLUtil.close(pstm);
                        SQLUtil.close(con);
                }
        }

        private void deleteOfflineShop(int objId) {
                Connection con = null;
                PreparedStatement pstm = null;

                try {
                        con = L1DatabaseFactory.getInstance().getConnection();
                        pstm = con.prepareStatement("DELETE FROM character_offline_shop WHERE char_id=?");
                        pstm.setInt(1, objId);
                        pstm.execute();
                } catch (SQLException e) {
                        _log.error(String.format("Failed to delete offline shop for objId %d", objId), e);
                } finally {
                        SQLUtil.close(pstm);
                        SQLUtil.close(con);
                }
        }

        private String serializeSellList(List<L1PrivateShopSellList> list) {
                if (list == null || list.isEmpty()) {
                        return null;
                }

                StringBuilder sb = new StringBuilder();
                for (L1PrivateShopSellList entry : list) {
                        if (sb.length() > 0) {
                                sb.append(';');
                        }
                        sb.append(entry.getItemObjectId()).append(',');
                        sb.append(entry.getSellTotalCount()).append(',');
                        sb.append(entry.getSellPrice()).append(',');
                        sb.append(entry.getSellCount());
                }
                return sb.toString();
        }

        private String serializeBuyList(List<L1PrivateShopBuyList> list) {
                if (list == null || list.isEmpty()) {
                        return null;
                }

                StringBuilder sb = new StringBuilder();
                for (L1PrivateShopBuyList entry : list) {
                        if (sb.length() > 0) {
                                sb.append(';');
                        }
                        sb.append(entry.getItemObjectId()).append(',');
                        sb.append(entry.getItemId()).append(',');
                        sb.append(entry.getItemEnchantLevel()).append(',');
                        sb.append(entry.getBuyTotalCount()).append(',');
                        sb.append(entry.getBuyPrice()).append(',');
                        sb.append(entry.getBuyCount());
                }
                return sb.toString();
        }

        private void restoreSellList(L1PcInstance pc, String data) {
                List<L1PrivateShopSellList> list = pc.getSellList();
                list.clear();
                if (data == null || data.isEmpty()) {
                        return;
                }

                String[] entries = data.split(";");
                for (String entry : entries) {
                        if (entry.isEmpty()) {
                                continue;
                        }
                        String[] parts = entry.split(",");
                        if (parts.length < 4) {
                                continue;
                        }
                        try {
                                L1PrivateShopSellList sell = new L1PrivateShopSellList();
                                sell.setItemObjectId(Integer.parseInt(parts[0]));
                                sell.setSellTotalCount(Integer.parseInt(parts[1]));
                                sell.setSellPrice(Integer.parseInt(parts[2]));
                                sell.setSellCount(Integer.parseInt(parts[3]));
                                list.add(sell);
                        } catch (NumberFormatException ignored) {
                                _log.warn(String.format("Invalid sell list data for %s: %s", pc.getName(), entry));
                        }
                }
        }

        private void restoreBuyList(L1PcInstance pc, String data) {
                List<L1PrivateShopBuyList> list = pc.getBuyList();
                list.clear();
                if (data == null || data.isEmpty()) {
                        return;
                }

                String[] entries = data.split(";");
                for (String entry : entries) {
                        if (entry.isEmpty()) {
                                continue;
                        }
                        String[] parts = entry.split(",");
                        if (parts.length < 6) {
                                continue;
                        }
                        try {
                                L1PrivateShopBuyList buy = new L1PrivateShopBuyList();
                                buy.setItemObjectId(Integer.parseInt(parts[0]));
                                buy.setItemId(Integer.parseInt(parts[1]));
                                buy.setItemEnchantLevel(Integer.parseInt(parts[2]));
                                buy.setBuyTotalCount(Integer.parseInt(parts[3]));
                                buy.setBuyPrice(Integer.parseInt(parts[4]));
                                buy.setBuyCount(Integer.parseInt(parts[5]));
                                list.add(buy);
                        } catch (NumberFormatException ignored) {
                                _log.warn(String.format("Invalid buy list data for %s: %s", pc.getName(), entry));
                        }
                }
        }

        public Map<Integer, L1PcInstance> getOfflineShops() {
                return _offlineShops;
        }
}
