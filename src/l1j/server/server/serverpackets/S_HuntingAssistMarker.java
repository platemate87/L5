package l1j.server.server.serverpackets;

import java.io.IOException;

import l1j.server.server.encryptions.Opcodes;

public class S_HuntingAssistMarker extends ServerBasePacket {

        private static final String S_HUNTING_ASSIST_MARKER = "[S] S_HuntingAssistMarker";
        private byte[] _byte = null;

        public S_HuntingAssistMarker(int objectId, int x, int y, boolean visible) {
                writeC(Opcodes.S_OPCODE_HUNTINGASSISTMARKER);
                writeC(visible ? 1 : 0);
                writeD(objectId);
                writeH(x);
                writeH(y);
                writeC(15);
                writeC(15);
        }

        @Override
        public byte[] getContent() throws IOException {
                if (_byte == null) {
                        _byte = getBytes();
                }
                return _byte;
        }

        @Override
        public String getType() {
                return S_HUNTING_ASSIST_MARKER;
        }
}
