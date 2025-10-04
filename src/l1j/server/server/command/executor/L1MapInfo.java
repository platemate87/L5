package l1j.server.server.command.executor;

import l1j.server.server.model.Instance.L1PcInstance;

public class L1MapInfo implements L1CommandExecutor {

        private L1MapInfo() {
        }

        public static L1CommandExecutor getInstance() {
                return L1MapEditor.getInstance();
        }

        @Override
        public void execute(L1PcInstance pc, String cmdName, String arg) {
                L1MapEditor.getInstance().execute(pc, cmdName, arg);
        }
}
