package l1j.server.server.command.executor;

import java.util.ArrayList;
import java.util.List;

import l1j.server.server.datatables.OfflineShopTable;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.serverpackets.S_SystemMessage;

public class L1OfflineShopDisconnect implements L1CommandExecutor {

        private L1OfflineShopDisconnect() {
        }

        public static L1CommandExecutor getInstance() {
                return new L1OfflineShopDisconnect();
        }

        @Override
        public void execute(L1PcInstance pc, String cmdName, String arg) {
                if (pc == null) {
                        return;
                }

                OfflineShopTable offlineShopTable = OfflineShopTable.getInstance();
                List<Integer> shopsToRemove = new ArrayList<>(offlineShopTable.getOfflineShops().keySet());

                int disconnected = 0;
                for (Integer objId : shopsToRemove) {
                        offlineShopTable.removeOfflineShop(objId);
                        disconnected++;
                }

                String message;
                if (disconnected == 0) {
                        message = "There are no offline private shops to disconnect.";
                } else if (disconnected == 1) {
                        message = "Disconnected 1 offline private shop.";
                } else {
                        message = String.format("Disconnected %d offline private shops.", disconnected);
                }
                pc.sendPackets(new S_SystemMessage(message));
        }
}
