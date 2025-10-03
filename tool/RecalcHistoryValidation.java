import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import l1j.server.Config;
import l1j.server.server.datatables.CharacterTable;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.model.classes.L1ClassId;
import l1j.server.server.storage.CharacterStorage;
import sun.misc.Unsafe;

public class RecalcHistoryValidation {
        private static final int TARGET_LEVEL = 20;
        private static final int EXPECTED_HISTORY_ENTRIES = TARGET_LEVEL - 9;
        private static final Set<Integer> GAP_INDEXES = new HashSet<>();

        static {
                GAP_INDEXES.add(1);
                GAP_INDEXES.add(4);
                GAP_INDEXES.add(6);
                GAP_INDEXES.add(8);
                GAP_INDEXES.add(10);
        }

        public static void main(String[] args) throws Exception {
                installCharacterTableStub();

                Config.KNIGHT_MAX_HP = 5000;
                Config.KNIGHT_MAX_MP = 1000;

                ScenarioResult baseline = validateScenario(true, null);
                validateScenario(false, baseline);

                System.out.println("Regression validation succeeded.");
        }

        private static void assertEqual(int expected, int actual, String label) {
                if (expected != actual) {
                        throw new AssertionError(label + " mismatch: expected " + expected + " but found " + actual);
                }
        }

        private static ScenarioResult validateScenario(boolean backfillBeforeBoost, ScenarioResult expected)
                        throws Exception {
                String scenarioLabel = backfillBeforeBoost ? "boost-after-backfill" : "boost-during-backfill";

                L1PcInstance cleanHistory = createKnight(false);
                L1PcInstance gappyHistory = createKnight(true);

                if (backfillBeforeBoost || expected == null) {
                        runRecalc(cleanHistory);
                }
                if (backfillBeforeBoost) {
                        runRecalc(gappyHistory);
                }

                short cleanHpBefore = cleanHistory.getBaseMaxHp();
                short gappyHpBefore = gappyHistory.getBaseMaxHp();
                short cleanMpBefore = cleanHistory.getBaseMaxMp();
                short gappyMpBefore = gappyHistory.getBaseMaxMp();

                assertEqual(cleanHpBefore, gappyHpBefore, scenarioLabel + " initial base HP");
                assertEqual(cleanMpBefore, gappyMpBefore, scenarioLabel + " initial base MP");

                boostStats(cleanHistory);
                boostStats(gappyHistory);

                runRecalc(cleanHistory);
                runRecalc(gappyHistory);

                short cleanHpAfter = cleanHistory.getBaseMaxHp();
                short gappyHpAfter = gappyHistory.getBaseMaxHp();
                short cleanMpAfter = cleanHistory.getBaseMaxMp();
                short gappyMpAfter = gappyHistory.getBaseMaxMp();

                if (expected == null) {
                        assertEqual(cleanHpAfter, gappyHpAfter, scenarioLabel + " post-boost base HP");
                        assertEqual(cleanMpAfter, gappyMpAfter, scenarioLabel + " post-boost base MP");
                } else {
                        assertEqual(expected.hp, cleanHpAfter, scenarioLabel + " post-boost clean HP");
                        assertEqual(expected.mp, cleanMpAfter, scenarioLabel + " post-boost clean MP");
                        if (gappyHpAfter > expected.hp) {
                                throw new AssertionError(
                                                scenarioLabel + " post-boost gappy HP exceeded clean baseline: " + gappyHpAfter
                                                                + " vs " + expected.hp);
                        }
                        if (gappyMpAfter > expected.mp) {
                                throw new AssertionError(
                                                scenarioLabel + " post-boost gappy MP exceeded clean baseline: " + gappyMpAfter
                                                                + " vs " + expected.mp);
                        }
                }

                System.out.println("Scenario " + scenarioLabel + ": HP before boost " + cleanHpBefore + " vs " + gappyHpBefore);
                System.out.println("Scenario " + scenarioLabel + ": MP before boost " + cleanMpBefore + " vs " + gappyMpBefore);
                System.out.println("Scenario " + scenarioLabel + ": HP after boost " + cleanHpAfter + " vs " + gappyHpAfter);
                System.out.println("Scenario " + scenarioLabel + ": MP after boost " + cleanMpAfter + " vs " + gappyMpAfter);
                if (expected == null) {
                        return new ScenarioResult(cleanHpAfter, cleanMpAfter);
                }
                return expected;
        }

        private static void boostStats(L1PcInstance pc) {
                pc.setBaseCon((byte) (pc.getBaseCon() + 1));
                pc.setBaseWis((byte) (pc.getBaseWis() + 1));
        }

        private static final class ScenarioResult {
                private final short hp;
                private final short mp;

                ScenarioResult(short hp, short mp) {
                        this.hp = hp;
                        this.mp = mp;
                }
        }

        private static short hpRandomComponent(int index) {
                return (short) ((index % 2 == 0) ? 6 : 5);
        }

        private static short mpRandomComponent(int index) {
                return (short) ((index % 2 == 0) ? 3 : 2);
        }

        private static void runRecalc(L1PcInstance pc) {
                pc.recalcBaseHpMpFromStats();
        }

        private static L1PcInstance createKnight(boolean withGaps) throws Exception {
                Unsafe unsafe = getUnsafe();
                L1PcInstance pc = (L1PcInstance) unsafe.allocateInstance(L1PcInstance.class);

                Field hpHistoryField = L1PcInstance.class.getDeclaredField("_hpGainHistory");
                hpHistoryField.setAccessible(true);
                hpHistoryField.set(pc, new ArrayList<>());
                Field mpHistoryField = L1PcInstance.class.getDeclaredField("_mpGainHistory");
                mpHistoryField.setAccessible(true);
                mpHistoryField.set(pc, new ArrayList<>());

                pc.setLevel(TARGET_LEVEL);
                pc.setClassId(L1ClassId.CLASSID_KNIGHT_MALE);
                pc.setType(1);
                pc.setBaseCon((byte) 18);
                pc.setOriginalCon(18);
                pc.setBaseWis((byte) 12);
                pc.setOriginalWis(12);

                Field baseHpField = L1PcInstance.class.getDeclaredField("_baseMaxHp");
                baseHpField.setAccessible(true);
                baseHpField.setShort(pc, (short) 215);
                Field baseMpField = L1PcInstance.class.getDeclaredField("_baseMaxMp");
                baseMpField.setAccessible(true);
                baseMpField.setShort(pc, (short) 51);

                pc.setMaxHp(215);
                pc.setCurrentHp(215);
                pc.setMaxMp(51);
                pc.setCurrentMp(51);

                populateHistories(pc, withGaps);

                return pc;
        }

        private static void populateHistories(L1PcInstance pc, boolean withGaps) throws Exception {
                List<Object> hpHistory = getHistory(pc, "_hpGainHistory");
                List<Object> mpHistory = getHistory(pc, "_mpGainHistory");

                ensureSize(hpHistory, EXPECTED_HISTORY_ENTRIES);
                ensureSize(mpHistory, EXPECTED_HISTORY_ENTRIES);

                Constructor<?> hpCtor = Class
                                .forName("l1j.server.server.model.Instance.L1PcInstance$HpGainEntry")
                                .getDeclaredConstructor(short.class, int.class, int.class);
                hpCtor.setAccessible(true);
                Constructor<?> mpCtor = Class
                                .forName("l1j.server.server.model.Instance.L1PcInstance$MpGainEntry")
                                .getDeclaredConstructor(short.class, int.class, int.class);
                mpCtor.setAccessible(true);

                for (int i = 0; i < EXPECTED_HISTORY_ENTRIES; i++) {
                        boolean skip = withGaps && GAP_INDEXES.contains(i);
                        if (!skip) {
                                Object hpEntry = hpCtor.newInstance(hpRandomComponent(i), 18, 18);
                                hpHistory.set(i, hpEntry);
                                Object mpEntry = mpCtor.newInstance(mpRandomComponent(i), 12, 12);
                                mpHistory.set(i, mpEntry);
                        }
                }

                if (!withGaps) {
                        return;
                }

                // Ensure recorded entries mimic the live totals we expect by partially clearing
                // the gappy history so that the missing slots are truly absent.
                for (int index : GAP_INDEXES) {
                        if (index < hpHistory.size()) {
                                hpHistory.set(index, null);
                        }
                        if (index < mpHistory.size()) {
                                mpHistory.set(index, null);
                        }
                }
        }

        @SuppressWarnings("unchecked")
        private static List<Object> getHistory(L1PcInstance pc, String fieldName) throws Exception {
                Field field = L1PcInstance.class.getDeclaredField(fieldName);
                field.setAccessible(true);
                return (List<Object>) field.get(pc);
        }

        private static void ensureSize(List<Object> list, int size) {
                while (list.size() < size) {
                        list.add(null);
                }
        }

        private static void installCharacterTableStub() throws Exception {
                Unsafe unsafe = getUnsafe();
                Field instanceField = CharacterTable.class.getDeclaredField("_instance");
                instanceField.setAccessible(true);
                Object current = instanceField.get(null);
                CharacterTable table;
                if (current == null) {
                        table = (CharacterTable) unsafe.allocateInstance(CharacterTable.class);
                } else {
                        table = (CharacterTable) current;
                }
                Field storageField = CharacterTable.class.getDeclaredField("_charStorage");
                storageField.setAccessible(true);
                storageField.set(table, new NoOpCharacterStorage());
                instanceField.set(null, table);
        }

        private static Unsafe getUnsafe() throws Exception {
                Field f = Unsafe.class.getDeclaredField("theUnsafe");
                f.setAccessible(true);
                return (Unsafe) f.get(null);
        }

        private static final class NoOpCharacterStorage implements CharacterStorage {
                @Override
                public void createCharacter(L1PcInstance pc) {
                }

                @Override
                public void deleteCharacter(String accountName, String charName) {
                }

                @Override
                public void storeCharacter(L1PcInstance pc) {
                }

                @Override
                public L1PcInstance loadCharacter(String charName) {
                        return null;
                }
        }
}
