package l1j.server.server.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import l1j.server.server.model.Instance.L1PcInstance;

/**
 * Utility methods for Tower of Insolence charm handling.
 */
public final class ToiCharmUtil {

    private static final Set<Integer> DEFAULT_TELEPORTABLE_FLOORS;
    private static final List<CharmRequirement> CHARM_REQUIREMENTS;

    static {
        DEFAULT_TELEPORTABLE_FLOORS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(1, 2, 3, 4, 5, 7, 8, 9)));

        List<CharmRequirement> requirements = new ArrayList<>();
        requirements.add(new CharmRequirement(40289, 11, 19, 16));
        requirements.add(new CharmRequirement(40290, 21, 29, 26));
        requirements.add(new CharmRequirement(40291, 31, 39, 36));
        requirements.add(new CharmRequirement(40292, 41, 49, 46));
        requirements.add(new CharmRequirement(40293, 51, 59, 56));
        requirements.add(new CharmRequirement(40294, 61, 69, 66));
        requirements.add(new CharmRequirement(40295, 71, 79, 76));
        requirements.add(new CharmRequirement(40296, 81, 89, 87));
        requirements.add(new CharmRequirement(40297, 91, 99, 97));
        CHARM_REQUIREMENTS = Collections.unmodifiableList(requirements);
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
        if (floor < 1 || floor > 99) {
            return false;
        }

        if (DEFAULT_TELEPORTABLE_FLOORS.contains(floor)) {
            return true;
        }

        for (CharmRequirement requirement : CHARM_REQUIREMENTS) {
            if (requirement.covers(floor)) {
                return pc.getInventory().checkItem(requirement.getItemId(), 1);
            }
        }

        return false;
    }

    private static boolean isTowerOfInsolenceFloor(int mapId) {
        return mapId >= 101 && mapId <= 200;
    }

    private static final class CharmRequirement {
        private final int itemId;
        private final int minFloor;
        private final int maxFloor;
        private final Set<Integer> excludedFloors;

        CharmRequirement(int itemId, int minFloor, int maxFloor, int... excludedFloors) {
            this.itemId = itemId;
            this.minFloor = minFloor;
            this.maxFloor = maxFloor;
            Set<Integer> exclusions = new HashSet<>();
            for (int excludedFloor : excludedFloors) {
                exclusions.add(excludedFloor);
            }
            this.excludedFloors = Collections.unmodifiableSet(exclusions);
        }

        int getItemId() {
            return itemId;
        }

        boolean covers(int floor) {
            return floor >= minFloor && floor <= maxFloor && !excludedFloors.contains(floor);
        }
    }
}
