package l1j.server.server.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import l1j.server.server.model.Instance.L1PcInstance;

/**
 * Utility methods for Tower of Insolence charm handling.
 */
public final class ToiCharmUtil {

    private static final Map<Integer, Integer> REQUIRED_CHARM_BY_BASE_FLOOR;

    static {
        Map<Integer, Integer> map = new HashMap<>();
        map.put(11, 40289); // Tower of Insolence 11F Charm
        map.put(21, 40290); // Tower of Insolence 21F Charm
        map.put(31, 40291); // Tower of Insolence 31F Charm
        map.put(41, 40292); // Tower of Insolence 41F Charm
        map.put(51, 40293); // Tower of Insolence 51F Charm
        map.put(61, 40294); // Tower of Insolence 61F Charm
        map.put(71, 40295); // Tower of Insolence 71F Charm
        map.put(81, 40296); // Tower of Insolence 81F Charm
        map.put(91, 40297); // Tower of Insolence 91F Charm
        REQUIRED_CHARM_BY_BASE_FLOOR = Collections.unmodifiableMap(map);
    }

    private ToiCharmUtil() {
    }

    public static boolean hasRequiredCharmForCurrentFloor(L1PcInstance pc) {
        if (pc == null) {
            return false;
        }
        return hasRequiredCharmForFloor(pc, pc.getMapId());
    }

    public static boolean hasRequiredCharmForFloor(L1PcInstance pc, int mapId) {
        if (pc == null) {
            return false;
        }

        if (!isTowerOfInsolenceFloor(mapId)) {
            return false;
        }

        int floor = mapId - 100;
        if (floor < 11 || floor > 99) {
            return false;
        }

        int baseFloor = ((floor - 1) / 10) * 10 + 1;
        Integer charmId = REQUIRED_CHARM_BY_BASE_FLOOR.get(baseFloor);
        if (charmId == null) {
            return false;
        }

        return pc.getInventory().checkItem(charmId, 1);
    }

    private static boolean isTowerOfInsolenceFloor(int mapId) {
        return mapId >= 101 && mapId <= 200;
    }
}
