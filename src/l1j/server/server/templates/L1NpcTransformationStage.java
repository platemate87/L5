package l1j.server.server.templates;

public class L1NpcTransformationStage {

        private final int _hpPercent;
        private final int _transformNpcId;

        public L1NpcTransformationStage(int hpPercent, int transformNpcId) {
                _hpPercent = hpPercent;
                _transformNpcId = transformNpcId;
        }

        public int getHpPercent() {
                return _hpPercent;
        }

        public int getTransformNpcId() {
                return _transformNpcId;
        }
}
