package l1j.server.server.model.map.edit;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.model.map.L1Map;
import l1j.server.server.model.map.L1V1Map;
import l1j.server.server.model.map.L1WorldMap;

/**
 * Manages active map editing sessions for GMs. Each session is scoped to a
 * specific player and map.
 */
public class MapEditManager {

        private static final MapEditManager INSTANCE = new MapEditManager();

        private final Map<Integer, MapEditSession> sessions = new ConcurrentHashMap<>();

        private MapEditManager() {
        }

        public static MapEditManager getInstance() {
                return INSTANCE;
        }

        public MapEditSession startSession(L1PcInstance pc) {
                return sessions.compute(pc.getId(), (id, existing) -> {
                        if (existing != null) {
                                return existing;
                        }
                        L1Map map = L1WorldMap.getInstance().getMap((short) pc.getMapId());
                        if (!(map instanceof L1V1Map)) {
                                throw new IllegalStateException("Map " + pc.getMapId() + " is not editable");
                        }
                        return new MapEditSession((short) pc.getMapId(), (L1V1Map) map);
                });
        }

        public MapEditSession getSession(L1PcInstance pc) {
                return sessions.get(pc.getId());
        }

        public void endSession(L1PcInstance pc) {
                sessions.remove(pc.getId());
        }

        public void cancelSession(L1PcInstance pc) {
                MapEditSession session = sessions.remove(pc.getId());
                if (session != null) {
                        session.resetWorkingCopy();
                }
        }
}
