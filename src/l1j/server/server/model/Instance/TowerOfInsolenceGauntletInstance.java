package l1j.server.server.model.Instance;

import static l1j.server.server.model.skill.L1SkillId.FOG_OF_SLEEPING;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import l1j.server.server.ActionCodes;
import l1j.server.server.GeneralThreadPool;
import l1j.server.server.datatables.DropTable;
import l1j.server.server.model.L1Character;
import l1j.server.server.model.L1Object;
import l1j.server.server.model.L1World;
import l1j.server.server.serverpackets.S_DoActionGFX;
import l1j.server.server.serverpackets.S_NPCPack;
import l1j.server.server.serverpackets.S_RemoveObject;
import l1j.server.server.serverpackets.ServerBasePacket;
import l1j.server.server.templates.L1Npc;
import l1j.server.server.templates.L1NpcTransformationStage;

public class TowerOfInsolenceGauntletInstance extends L1MonsterInstance {

        private static final long serialVersionUID = 1L;

        private final List<L1NpcTransformationStage> _transformationPlan;
        private int _nextStageIndex;
        private final int _initialMaxHp;

        public TowerOfInsolenceGauntletInstance(L1Npc template) {
                super(template);
                _transformationPlan = new ArrayList<>(template.getTransformationStages());
                _initialMaxHp = getMaxHp();
        }

        @Override
        public void receiveDamage(L1Character attacker, int damage) {
                if (_transformationPlan.isEmpty()) {
                        super.receiveDamage(attacker, damage);
                        return;
                }

                if (attacker instanceof L1PcInstance) {
                        ((L1PcInstance) attacker).setLastAggressiveAct();
                }

                if (getCurrentHp() > 0 && !isDead()) {
                        if (getHiddenStatus() == HIDDEN_STATUS_SINK || getHiddenStatus() == HIDDEN_STATUS_FLY) {
                                return;
                        }
                        if (damage >= 0) {
                                if (attacker instanceof L1EffectInstance) {
                                        setHate(((L1EffectInstance) attacker).getUser(), damage);
                                } else {
                                        setHate(attacker, damage);
                                }
                        }
                        if (damage > 0) {
                                removeSkillEffect(FOG_OF_SLEEPING);
                        }

                        onNpcAI();

                        L1Npc template = getNpcTemplate();
                        if (attacker instanceof L1PcInstance) {
                                serchLink((L1PcInstance) attacker, template.get_family());
                        }

                        int id = getNpcId();
                        if (attacker instanceof L1PcInstance && damage > 0) {
                                L1PcInstance player = (L1PcInstance) attacker;
                                player.setPetTarget(this);

                                if (id >= 45681 && id <= 45684) {
                                        recall(player);
                                }
                        }

                        int newHp = getCurrentHp() - damage;
                        newHp = applyStageTransitions(newHp);

                        if (newHp <= 0 && !isDead()) {
                                setCurrentHpDirect(0);
                                setDead(true);
                                setStatus(ActionCodes.ACTION_Die);
                                openDoorWhenNpcDied(this);
                                Death death = new Death(attacker);
                                GeneralThreadPool.getInstance().execute(death);
                        }
                        if (newHp > 0) {
                                setCurrentHp(newHp);
                                hide();
                        }
                } else if (!isDead()) {
                        setDead(true);
                        setStatus(ActionCodes.ACTION_Die);
                        Death death = new Death(attacker);
                        GeneralThreadPool.getInstance().execute(death);
                }
        }

        private int applyStageTransitions(int prospectiveHp) {
                int newHp = prospectiveHp;
                while (_nextStageIndex < _transformationPlan.size()) {
                        L1NpcTransformationStage stage = _transformationPlan.get(_nextStageIndex);
                        int thresholdHp = calculateThresholdHp(stage.getHpPercent());
                        if (newHp <= thresholdHp) {
                                _nextStageIndex++;
                                transform(stage.getTransformNpcId());
                                newHp = Math.min(Math.max(newHp, 1), getMaxHp());
                                setCurrentHpDirect(newHp);
                        } else {
                                break;
                        }
                }
                return newHp;
        }

        private int calculateThresholdHp(int hpPercent) {
                if (hpPercent <= 0) {
                        return 0;
                }
                return (_initialMaxHp * hpPercent + 99) / 100;
        }

        private void hide() {
                switch (getNpcTemplate().get_npcId()) {
                case 45061:
                case 45161:
                case 45181:
                case 45455:
                        hideHelper(3, 10, 13, HIDDEN_STATUS_SINK, new S_DoActionGFX(getId(), ActionCodes.ACTION_Hide));
                        break;
                case 45682:
                        hideHelper(3, 50, 20, HIDDEN_STATUS_SINK,
                                        new S_DoActionGFX(getId(), ActionCodes.ACTION_AntharasHide));
                        break;
                case 45067:
                case 45264:
                case 45452:
                case 45090:
                case 4532:
                case 45445:
                        hideHelper(3, 10, 4, HIDDEN_STATUS_FLY, new S_DoActionGFX(getId(), ActionCodes.ACTION_Moveup));
                        break;
                case 45681:
                        hideHelper(3, 50, 11, HIDDEN_STATUS_FLY, new S_DoActionGFX(getId(), ActionCodes.ACTION_Moveup));
                        break;
                case 46107:
                case 46108:
                        hideHelper(4, 10, 13, HIDDEN_STATUS_SINK, new S_DoActionGFX(getId(), ActionCodes.ACTION_Hide));
                        break;
                default:
                }
        }

        private void hideHelper(int hpFraction, int range, int status, int hiddenStatus, final ServerBasePacket action) {
                if (getMaxHp() / hpFraction > getCurrentHp() && 1 > ThreadLocalRandom.current().nextInt(range)) {
                        allTargetClear();
                        setStatusAndHiddenStatus(status, hiddenStatus);
                        broadcastPacket(action);
                        broadcastPacket(new S_NPCPack(this));
                }
        }

        private void setStatusAndHiddenStatus(int status, int hiddenStatus) {
                setHiddenStatus(hiddenStatus);
                setStatus(status);
        }

        private static void openDoorWhenNpcDied(L1NpcInstance npc) {
                int[] npcId = { 46143, 46144, 46145, 46146, 46147, 46148, 46149, 46150, 46151, 46152 };
                int[] doorId = { 5001, 5002, 5003, 5004, 5005, 5006, 5007, 5008, 5009, 5010 };

                for (int i = 0; i < npcId.length; i++) {
                        if (npc.getNpcTemplate().get_npcId() == npcId[i]) {
                                openDoorInCrystalCave(doorId[i]);
                        }
                }
        }

        private static void openDoorInCrystalCave(int doorId) {
                for (L1Object object : L1World.getInstance().getObject()) {
                        if (object instanceof L1DoorInstance) {
                                L1DoorInstance door = (L1DoorInstance) object;
                                if (door.getDoorId() == doorId) {
                                        door.open();
                                }
                        }
                }
        }

        private void recall(L1PcInstance pc) {
                if (getMapId() != pc.getMapId()) {
                        return;
                }

                if (getLocation().getTileLineDistance(pc.getLocation()) > 4) {
                        return;
                }

                DropTable.getInstance().setDrop(this, getInventory());
                pc.sendPackets(new S_RemoveObject(this));
                pc.removeKnownObject(this);
                deleteMe();
        }
}
