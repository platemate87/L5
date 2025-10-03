package l1j.server.server.model.Instance;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.LinkedHashMap;

import l1j.server.Config;
import l1j.server.server.datatables.AccessLevelTable;
import l1j.server.server.datatables.CharacterTable;
import l1j.server.server.model.L1AccessLevel;
import l1j.server.server.model.classes.L1ClassFeature;
import l1j.server.server.model.classes.L1ClassId;
import l1j.server.server.storage.CharacterStorage;

public class L1PcInstanceRecalcTest {
        private static class RecordingCharacterStorage implements CharacterStorage {
                private int storeCalls;

                int getStoreCalls() {
                        return storeCalls;
                }

                @Override
                public void createCharacter(L1PcInstance pc) {
                        throw new UnsupportedOperationException();
                }

                @Override
                public void deleteCharacter(String accountName, String charName) {
                        throw new UnsupportedOperationException();
                }

                @Override
                public void storeCharacter(L1PcInstance pc) {
                        storeCalls++;
                }

                @Override
                public L1PcInstance loadCharacter(String charName) {
                        throw new UnsupportedOperationException();
                }
        }

        public static void main(String[] args) throws Exception {
                installAccessLevelStub();

                Config.KNIGHT_MAX_HP = 32767;
                Config.KNIGHT_MAX_MP = 32767;

                CharacterTable table = CharacterTable.getInstance();
                Field storageField = CharacterTable.class.getDeclaredField("_charStorage");
                storageField.setAccessible(true);
                CharacterStorage originalStorage = (CharacterStorage) storageField.get(table);
                RecordingCharacterStorage recordingStorage = new RecordingCharacterStorage();
                storageField.set(table, recordingStorage);
                try {
                        L1PcInstance pc = new L1PcInstance();
                        pc.setName("Test");
                        pc.setType(1);
                        pc.setClassId(L1ClassId.CLASSID_KNIGHT_MALE);
                        pc.setLevel(60);
                        pc.setBaseCon((byte) 14);
                        pc.setBaseWis((byte) 12);
                        pc.setOriginalCon(14);
                        pc.setOriginalWis(12);

                        int levelsTracked = Math.max(0, pc.getLevel() - 9);
                        String[] hpEntries = new String[levelsTracked];
                        Arrays.fill(hpEntries, "12:14:14");
                        pc.loadHpGainHistory(String.join(",", hpEntries));

                        String[] mpEntries = new String[levelsTracked];
                        Arrays.fill(mpEntries, "2:12:12");
                        pc.loadMpGainHistory(String.join(",", mpEntries));

                        L1ClassFeature features = pc.getClassFeature();
                        int startingHp = features.getStartingHp();
                        int startingMp = features.getStartingMp(pc.getBaseWis());
                        pc.addBaseMaxHp((short) startingHp);
                        pc.addBaseMaxHp((short) (levelsTracked * 12));
                        pc.addBaseMaxMp((short) startingMp);
                        pc.addBaseMaxMp((short) (levelsTracked * 3));

                        int expectedInitialHp = startingHp + levelsTracked * 12;
                        if (pc.getBaseMaxHp() != expectedInitialHp) {
                                throw new AssertionError(
                                                "Initial base HP " + expectedInitialHp + " but was " + pc.getBaseMaxHp());
                        }
                        int expectedInitialMp = startingMp + levelsTracked * 3;
                        if (pc.getBaseMaxMp() != expectedInitialMp) {
                                throw new AssertionError(
                                                "Initial base MP " + expectedInitialMp + " but was " + pc.getBaseMaxMp());
                        }

                        pc.addBaseCon((byte) 4);

                        int expectedHpPerLevel = 18 + 3 + 0;
                        int expectedBaseHp = startingHp + levelsTracked * expectedHpPerLevel;
                        if (pc.getBaseMaxHp() != expectedBaseHp) {
                                throw new AssertionError(
                                                "Expected base HP " + expectedBaseHp + " but was " + pc.getBaseMaxHp());
                        }
                        String hpData = pc.getHpGainHistoryData();
                        if (!hpData.isEmpty()) {
                                String firstEntry = hpData.split(",")[0];
                                if (!"18:18:14".equals(firstEntry)) {
                                        throw new AssertionError("Unexpected HP history entry: " + firstEntry);
                                }
                        }

                        pc.addBaseWis((byte) 6);

                        int expectedMpPerLevel = 3 + 2;
                        int expectedBaseMp = features.getStartingMp(pc.getBaseWis()) + levelsTracked * expectedMpPerLevel;
                        if (pc.getBaseMaxMp() != expectedBaseMp) {
                                throw new AssertionError(
                                                "Expected base MP " + expectedBaseMp + " but was " + pc.getBaseMaxMp());
                        }
                        String mpData = pc.getMpGainHistoryData();
                        if (!mpData.isEmpty()) {
                                String firstEntry = mpData.split(",")[0];
                                if (!"3:18:12".equals(firstEntry)) {
                                        throw new AssertionError("Unexpected MP history entry: " + firstEntry);
                                }
                        }

                        if (recordingStorage.getStoreCalls() < 2) {
                                throw new AssertionError("Expected at least two storeCharacter calls but observed "
                                                + recordingStorage.getStoreCalls());
                        }
                } finally {
                        storageField.set(table, originalStorage);
                }
        }

        private static void installAccessLevelStub() throws Exception {
                Field instanceField = AccessLevelTable.class.getDeclaredField("_instance");
                instanceField.setAccessible(true);

                sun.misc.Unsafe unsafe = getUnsafe();
                AccessLevelTable stub = (AccessLevelTable) unsafe.allocateInstance(AccessLevelTable.class);

                Field mapField = AccessLevelTable.class.getDeclaredField("_accessLevels");
                mapField.setAccessible(true);
                LinkedHashMap<Short, L1AccessLevel> map = new LinkedHashMap<>();
                L1AccessLevel defaultLevel = new L1AccessLevel((short) -1, "Player", (short) 0, null);
                map.put((short) -1, defaultLevel);
                mapField.set(stub, map);
                AccessLevelTable.minAccessLevel = defaultLevel;
                instanceField.set(null, stub);
        }

        private static sun.misc.Unsafe getUnsafe() throws Exception {
                Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
                unsafeField.setAccessible(true);
                return (sun.misc.Unsafe) unsafeField.get(null);
        }
}
