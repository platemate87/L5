package l1j.server.server.model.Instance;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import l1j.server.server.model.L1Character;
import l1j.server.server.templates.L1Npc;
import l1j.server.server.templates.L1NpcTransformationStage;

public class TowerOfInsolenceGauntletInstanceTest {

        private static final int BASE_NPC_ID = 920000;
        private static final int FINAL_STAGE_ID = 91334;

        public static void main(String[] args) {
                Map<Integer, L1Npc> templates = new HashMap<>();
                List<L1NpcTransformationStage> stages = new ArrayList<>();
                for (int percent = 90; percent >= 10; percent -= 10) {
                        int stageNpcId = percent == 10 ? FINAL_STAGE_ID : BASE_NPC_ID + (100 - percent) / 10;
                        stages.add(new L1NpcTransformationStage(percent, stageNpcId));
                }

                L1Npc base = createTemplate(BASE_NPC_ID, 1000);
                base.setTransformationStages(stages);
                templates.put(BASE_NPC_ID, base);

                for (L1NpcTransformationStage stage : stages) {
                        L1Npc template = createTemplate(stage.getTransformNpcId(), 1000);
                        templates.put(stage.getTransformNpcId(), template);
                }

                TestBoss boss = new TestBoss(base, templates);
                L1Character attacker = boss;

                int expectedHp = 1000;
                for (int percent = 90; percent >= 10; percent -= 10) {
                        boss.receiveDamage(attacker, 100);
                        expectedHp -= 100;
                        if (boss.isDead()) {
                                throw new AssertionError("Boss died prematurely at " + percent + "% stage");
                        }
                        if (boss.getCurrentHp() != expectedHp) {
                                throw new AssertionError("Unexpected HP after damage: " + boss.getCurrentHp());
                        }
                }

                List<Integer> transitions = boss.getTransitions();
                if (transitions.size() != 9) {
                        throw new AssertionError("Expected 9 transitions but got " + transitions.size());
                }

                int expectedStageId = BASE_NPC_ID + 1;
                for (int i = 0; i < transitions.size(); i++) {
                        int stageId = transitions.get(i);
                        if (i == transitions.size() - 1) {
                                expectedStageId = FINAL_STAGE_ID;
                        }
                        if (stageId != expectedStageId) {
                                throw new AssertionError(
                                                "Unexpected stage at index " + i + ": expected " + expectedStageId + " but was "
                                                                + stageId);
                        }
                        if (expectedStageId != FINAL_STAGE_ID) {
                                expectedStageId++;
                        }
                }

                if (boss.getNpcTemplate().get_npcId() != FINAL_STAGE_ID) {
                        throw new AssertionError("Final stage was not Grim Reaper");
                }
        }

        private static L1Npc createTemplate(int npcId, int hp) {
                L1Npc npc = new L1Npc();
                npc.set_npcId(npcId);
                npc.set_name("Test NPC " + npcId);
                npc.set_nameid("Test NPC " + npcId);
                npc.setImpl("TowerOfInsolenceGauntlet");
                npc.set_gfxid(0);
                npc.set_level(1);
                npc.set_hp(hp);
                npc.set_mp(0);
                npc.set_ac(0);
                npc.set_str((byte) 10);
                npc.set_con((byte) 10);
                npc.set_dex((byte) 10);
                npc.set_wis((byte) 10);
                npc.set_int((byte) 10);
                npc.set_mr(0);
                npc.set_exp(0);
                npc.set_lawful(0);
                npc.set_size("medium");
                npc.set_randomlevel(0);
                npc.set_randomhp(0);
                npc.set_randommp(0);
                npc.set_randomac(0);
                npc.set_randomexp(0);
                npc.set_randomlawful(0);
                npc.set_hprinterval(0);
                npc.set_hpr(0);
                npc.set_mprinterval(0);
                npc.set_mpr(0);
                npc.set_weakAttr(0);
                npc.set_ranged(0);
                npc.setTamable(false);
                npc.set_passispeed(0);
                npc.set_atkspeed(0);
                npc.set_agro(false);
                npc.set_agrososc(false);
                npc.set_agrocoi(false);
                npc.set_family(0);
                npc.set_agrofamily(0);
                npc.set_agrogfxid1(0);
                npc.set_agrogfxid2(0);
                npc.set_picupitem(false);
                npc.set_digestitem(0);
                npc.set_bravespeed(false);
                npc.set_teleport(false);
                npc.set_damagereduction(0);
                npc.set_hard(false);
                npc.set_doppel(false);
                npc.set_IsTU(false);
                npc.set_IsErase(false);
                npc.setBowActId(0);
                npc.setKarma(0);
                npc.setTransformId(-1);
                npc.setTransformGfxId(0);
                npc.setLightSize(0);
                npc.setAmountFixed(false);
                npc.setChangeHead(false);
                npc.setCantResurrect(false);
                return npc;
        }

        private static class TestBoss extends TowerOfInsolenceGauntletInstance {

                private final Map<Integer, L1Npc> _templates;
                private final List<Integer> _transitions = new ArrayList<>();

                TestBoss(L1Npc template, Map<Integer, L1Npc> templates) {
                        super(template);
                        _templates = templates;
                }

                @Override
                protected void transform(int transformId) {
                        _transitions.add(transformId);
                        L1Npc template = _templates.get(transformId);
                        if (template == null) {
                                throw new IllegalStateException("Missing template for transform " + transformId);
                        }
                        setting_template(template);
                }

                @Override
                public void onNpcAI() {
                        // Suppress data-table initialization in tests.
                }

                @Override
                public void setting_template(L1Npc template) {
                        try {
                                Field field = L1NpcInstance.class.getDeclaredField("_npcTemplate");
                                field.setAccessible(true);
                                field.set(this, template);
                        } catch (Exception e) {
                                throw new RuntimeException(e);
                        }

                        setName(template.get_name());
                        setNameId(template.get_nameid());
                        setLevel(template.get_level());
                        setMaxHp(template.get_hp());
                        setCurrentHpDirect(template.get_hp());
                        setMaxMp(template.get_mp());
                        setCurrentMpDirect(template.get_mp());
                        setAc(template.get_ac());
                        setStr(template.get_str());
                        setCon(template.get_con());
                        setDex(template.get_dex());
                        setInt(template.get_int());
                        setWis(template.get_wis());
                        setMr(template.get_mr());
                        setPassispeed(template.get_passispeed());
                        setAtkspeed(template.get_atkspeed());
                        setAgro(template.is_agro());
                        setAgrocoi(template.is_agrocoi());
                        setAgrososc(template.is_agrososc());
                        setTempCharGfx(template.get_gfxid());
                        setGfxId(template.get_gfxid());
                        setExp(template.get_exp());
                        setLawful(template.get_lawful());
                        setTempLawful(template.get_lawful());
                }

                List<Integer> getTransitions() {
                        return _transitions;
                }
        }
}
