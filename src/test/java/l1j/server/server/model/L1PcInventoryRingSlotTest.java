package l1j.server.server.model;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import l1j.server.server.model.Instance.L1ItemInstance;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.templates.L1Armor;
import sun.misc.Unsafe;

public class L1PcInventoryRingSlotTest {

        public static void main(String[] args) throws Exception {
                L1PcInventory inventory = allocateInventory();
                L1ItemInstance ringA = createRing(9, 1);
                L1ItemInstance ringB = createRing(9, 3);
                L1ItemInstance ringType11 = createRing(11, 5);

                List<L1ItemInstance> items = inventory.getItems();
                items.add(ringA);
                items.add(ringB);
                items.add(ringType11);

                int bonusWithoutBracelets = inventory.getInventoryChaBonus();
                assertEquals(8, bonusWithoutBracelets, "Expected type-11 rings to share ring slots");

                items.clear();
                items.add(ringA);
                items.add(ringB);
                items.add(ringType11);
                items.addAll(createEquippedBracelets(3));

                int bonusWithBracelets = inventory.getInventoryChaBonus();
                assertEquals(9, bonusWithBracelets, "Bracelet-based ring capacity should remain respected");

                System.out.println("L1PcInventoryRingSlotTest passed");
        }

        private static L1PcInventory allocateInventory() throws Exception {
                Unsafe unsafe = getUnsafe();
                L1PcInventory inventory = (L1PcInventory) unsafe.allocateInstance(L1PcInventory.class);
                L1PcInstance owner = (L1PcInstance) unsafe.allocateInstance(L1PcInstance.class);

                setBoolean(owner, "_gm", true);
                setField(inventory, L1Inventory.class, "_items", new CopyOnWriteArrayList<>());
                setField(inventory, L1PcInventory.class, "_owner", owner);

                return inventory;
        }

        private static List<L1ItemInstance> createEquippedBracelets(int count) {
                List<L1ItemInstance> bracelets = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                        L1Armor braceletTemplate = new L1Armor();
                        braceletTemplate.setType2(2);
                        braceletTemplate.setType(11);
                        L1ItemInstance bracelet = new L1ItemInstance(braceletTemplate, 1);
                        bracelet.setEquipped(true);
                        bracelets.add(bracelet);
                }
                return bracelets;
        }

        private static L1ItemInstance createRing(int type, int chaBonus) {
                L1Armor ringTemplate = new L1Armor();
                ringTemplate.setType2(2);
                ringTemplate.setType(type);
                ringTemplate.set_addcha((byte) chaBonus);
                return new L1ItemInstance(ringTemplate, 1);
        }

        private static void assertEquals(int expected, int actual, String message) {
                if (expected != actual) {
                        throw new AssertionError(message + ": expected=" + expected + ", actual=" + actual);
                }
        }

        private static Unsafe getUnsafe() throws Exception {
                Field f = Unsafe.class.getDeclaredField("theUnsafe");
                f.setAccessible(true);
                return (Unsafe) f.get(null);
        }

        private static void setBoolean(Object target, String fieldName, boolean value) throws Exception {
                Field field = L1PcInstance.class.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.setBoolean(target, value);
        }

        private static void setField(Object target, Class<?> declaringClass, String fieldName, Object value)
                        throws Exception {
                Field field = declaringClass.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
        }
}
