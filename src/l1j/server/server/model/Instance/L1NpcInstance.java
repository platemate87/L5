/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * http://www.gnu.org/copyleft/gpl.html
 */
package l1j.server.server.model.Instance;

import static l1j.server.server.model.item.L1ItemId.B_POTION_OF_GREATER_HASTE_SELF;
import static l1j.server.server.model.item.L1ItemId.B_POTION_OF_HASTE_SELF;
import static l1j.server.server.model.item.L1ItemId.POTION_OF_EXTRA_HEALING;
import static l1j.server.server.model.item.L1ItemId.POTION_OF_GREATER_HASTE_SELF;
import static l1j.server.server.model.item.L1ItemId.POTION_OF_GREATER_HEALING;
import static l1j.server.server.model.item.L1ItemId.POTION_OF_HASTE_SELF;
import static l1j.server.server.model.item.L1ItemId.POTION_OF_HEALING;
import static l1j.server.server.model.skill.L1SkillId.CANCELLATION;
import static l1j.server.server.model.skill.L1SkillId.COUNTER_BARRIER;
import static l1j.server.server.model.skill.L1SkillId.POLLUTE_WATER;
import static l1j.server.server.model.skill.L1SkillId.STATUS_HASTE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l1j.server.Config;
import l1j.server.server.ActionCodes;
import l1j.server.server.GeneralThreadPool;
import l1j.server.server.datatables.NpcChatTable;
import l1j.server.server.datatables.NpcSpawnTable;
import l1j.server.server.datatables.NpcTable;
import l1j.server.server.model.L1Attack;
import l1j.server.server.model.L1Character;
import l1j.server.server.model.L1GroundInventory;
import l1j.server.server.model.L1HateList;
import l1j.server.server.model.L1Inventory;
import l1j.server.server.model.L1Magic;
import l1j.server.server.model.L1MobGroupInfo;
import l1j.server.server.model.L1MobSkillUse;
import l1j.server.server.model.L1NpcChatTimer;
import l1j.server.server.model.L1Object;
import l1j.server.server.model.L1Spawn;
import l1j.server.server.model.L1World;
import l1j.server.server.model.map.L1Map;
import l1j.server.server.model.map.L1WorldMap;
import l1j.server.server.model.skill.L1SkillId;
import l1j.server.server.model.skill.L1SkillUse;
import l1j.server.server.serverpackets.S_ChangeShape;
import l1j.server.server.serverpackets.S_DoActionGFX;
import l1j.server.server.serverpackets.S_Light;
import l1j.server.server.serverpackets.S_MoveCharPacket;
import l1j.server.server.serverpackets.S_NPCPack;
import l1j.server.server.serverpackets.S_RemoveObject;
import l1j.server.server.serverpackets.S_SkillHaste;
import l1j.server.server.serverpackets.S_SkillSound;
import l1j.server.server.templates.L1Npc;
import l1j.server.server.templates.L1NpcChat;
import l1j.server.server.types.Point;

public class L1NpcInstance extends L1Character {
	private static final long serialVersionUID = 1L;

	public static final int MOVE_SPEED = 0;
	public static final int ATTACK_SPEED = 1;
	public static final int MAGIC_SPEED = 2;

	public static final int HIDDEN_STATUS_NONE = 0;
	public static final int HIDDEN_STATUS_SINK = 1;
	public static final int HIDDEN_STATUS_FLY = 2;
	public static final int HIDDEN_STATUS_ICE = 3;

	public static final int CHAT_TIMING_APPEARANCE = 0;
	public static final int CHAT_TIMING_DEAD = 1;
	public static final int CHAT_TIMING_HIDE = 2;
	public static final int CHAT_TIMING_GAME_TIME = 3;

	static Logger _log = LoggerFactory.getLogger(L1NpcInstance.class.getName());
	private L1Npc _npcTemplate;

	private L1Spawn _spawn;
	private int _spawnNumber;

	private int _petcost;
	public L1Inventory _inventory = new L1Inventory();
	private L1MobSkillUse mobSkill;
	private boolean firstFound = true;
	private boolean _awoken = false;
	private int _drainedMana = 0;
	private boolean _rest = false;
	private boolean _truedead = false;

	private int _randomMoveDistance = 0;
	private int _randomMoveDirection = 0;

	private L1NpcChatTimer _chatTask;
	private ScheduledFuture<?> _chatTaskFuture;
	private NpcAIThreadImpl _aiThread = null;

	private L1NpcInstance _servantMaster = null;

	// keeps track of the # of summons the mob has spawned
	private int _summons = 0;

	public void setServantMaster(L1NpcInstance npcInstance) {
		this._servantMaster = npcInstance;
	}

	public L1NpcInstance getServantMaster() {
		return this._servantMaster;
	}

	public int getServantSummonCount() {
		return this._summons;
	}

	public void addServantSummon() {
		this._summons++;
	}

	public void addServantSummon(int amount) {
		this._summons += amount;
	}

	public void removeServantSummon() {
		this._summons--;

		// just in case
		if (this._summons < 0) {
			this._summons = 0;
		}
	}

	interface NpcAI {
		public void start();
	}

	protected void startAI() {
		if (_aiThread == null)
			_aiThread = new NpcAIThreadImpl();

		if (Config.NPCAI_IMPLTYPE == 1) {
			new NpcAITimerImpl().start();
		} else if (Config.NPCAI_IMPLTYPE == 2) {
			_aiThread.start();
		} else {
			new NpcAITimerImpl().start();
		}
	}

	protected L1NpcInstance getCallingClass() {
		return this;
	}

	class NpcAITimerImpl implements Runnable, NpcAI {
		private class DeathSyncTimer implements Runnable {
			private void schedule(int delay) {
				GeneralThreadPool.getInstance().schedule(new DeathSyncTimer(), delay);
			}

			@Override
			public void run() {
				try {
					if (isDeathProcessing()) {
						schedule(getSleepTime());
						return;
					}
					allTargetClear();
					setAiRunning(false);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					_log.error("", e);
				}
			}
		}

		@Override
		public void start() {
			setAiRunning(true);
			GeneralThreadPool.getInstance().schedule(NpcAITimerImpl.this, 0);
		}

		private void stop() {
			mobSkill.resetAllSkillUseCount();
			GeneralThreadPool.getInstance().schedule(new DeathSyncTimer(), 0);
		}

		private void schedule(int delay) {
			GeneralThreadPool.getInstance().schedule(new NpcAITimerImpl(), delay);
		}

		@Override
		public void run() {
			try {
				if (notContinued()) {
					stop();
					return;
				}

				if (0 < _paralysisTime) {
					schedule(_paralysisTime);
					_paralysisTime = 0;
					setParalyzed(false);
					return;
				} else if (isParalyzed() || isSleeped()) {
					schedule(200);
					return;
				}

				if (!AIProcess()) {
					schedule(getSleepTime());
					return;
				}
				stop();
			} catch (Exception e) {
				_log.warn("NpcAI", e);
			}
		}

		private boolean notContinued() {
			return _destroyed || isDead() || getCurrentHp() <= 0 || getHiddenStatus() != HIDDEN_STATUS_NONE;
		}
	}

	class NpcAIThreadImpl implements Runnable, NpcAI {
		@Override
		public void start() {
			// need to set it running before the thread is running
			// to fix a race condition.
			// shouldn't be a problem as long as the GeneralThreadPool doesn't crap out
			getCallingClass().setAiRunning(true);
			GeneralThreadPool.getInstance().execute(NpcAIThreadImpl.this);
		}

		@Override
		public void run() {
			Thread.currentThread().setName("NpcAIThreadImpl-" + getNpcId());
			try {
				while (!_destroyed && !isDead() && getCurrentHp() > 0 && getHiddenStatus() == HIDDEN_STATUS_NONE
						&& isAiRunning()) {

					while (isParalyzed() || isSleeped()) {
						try {
							Thread.sleep(200);
						} catch (InterruptedException e) {
							setParalyzed(false);
						}
					}

					if (AIProcess()) {
						break;
					}
					try {
						Thread.sleep(getSleepTime());
					} catch (Exception e) {
						break;
					}
				}
				mobSkill.resetAllSkillUseCount();
				do {
					try {
						Thread.sleep(getSleepTime());
					} catch (Exception e) {
						break;
					}
				} while (isDeathProcessing());
				allTargetClear();
			} catch (Exception e) {
				_log.warn("NpcAI", e);
			} finally {
				setAiRunning(false);
			}
		}
	}

	private boolean AIProcess() {
		setSleepTime(300);

		checkTarget();
		if (_target == null && _master == null) {
			searchTarget();
		}

		onItemUse();

		if (_target == null) {
			checkTargetItem();
			if (isPickupItem() && _targetItem == null) {
				searchTargetItem();
			}

			if (_targetItem == null) {
				if (noTarget(1)) {
					return true;
				}
			} else {
				L1Inventory groundInventory = L1World.getInstance().getInventory(_targetItem.getX(), _targetItem.getY(),
						_targetItem.getMapId());
				if (groundInventory.checkItem(_targetItem.getItemId())) {
					onTargetItem();
				} else {
					_targetItemList.remove(_targetItem);
					_targetItem = null;
					setSleepTime(1000);
					return false;
				}
			}
		} else {
			if (getHiddenStatus() == HIDDEN_STATUS_NONE) {
				onTarget();
			} else {
				return true;
			}
		}

		return false;
	}

	public void onItemUse() {
	}

	public void searchTarget() {
	}

	public void checkTarget() {
		if (_target == null || _target.getMapId() != getMapId() || _target.getCurrentHp() <= 0 || _target.isDead()
				|| (_target.isInvisble() && !getNpcTemplate().is_agrocoi() && !_hateList.containsKey(_target))) {
			if (_target != null) {
				targetClear();
			}
			if (!_hateList.isEmpty()) {
				_target = _hateList.getMaxHateCharacter();
				checkTarget();
			}
		}
	}

	public void setHate(L1Character cha, int hate) {
		if (cha != null && cha.getId() != getId()) {
			if (!isFirstAttack() && hate != 0) {

				hate += getMaxHp() / 10;
				setFirstAttack(true);
			}

			_hateList.add(cha, hate);
			_dropHateList.add(cha, hate);
			_target = _hateList.getMaxHateCharacter();
			checkTarget();
		}
	}

	public void setLink(L1Character cha) {
	}

	public void serchLink(L1PcInstance targetPlayer, int family) {
		List<L1Object> targetKnownObjects = targetPlayer.getKnownObjects();
		for (Object knownObject : targetKnownObjects) {
			if (knownObject instanceof L1NpcInstance) {
				L1NpcInstance npc = (L1NpcInstance) knownObject;
				if (npc.getNpcTemplate().get_agrofamily() > 0) {
					if (npc.getNpcTemplate().get_agrofamily() == 1) {
						if (npc.getNpcTemplate().get_family() == family) {
							npc.setLink(targetPlayer);
						}
					} else {
						npc.setLink(targetPlayer);
					}
				}
				L1MobGroupInfo mobGroupInfo = getMobGroupInfo();
				if (mobGroupInfo != null) {
					if (getMobGroupId() != 0 && getMobGroupId() == npc.getMobGroupId()) {
						npc.setLink(targetPlayer);
					}
				}
			}
		}
	}
	
	public void setTarget(L1Character target) {
	    if (target == null) {
	        _log.warn("Trying to set null target.");
	        _target = null;
	        return;
	    }
	    if ((target instanceof L1PetInstance && ((L1PetInstance)target).getMaster() == null) ||
	        (target instanceof L1SummonInstance && ((L1SummonInstance)target).getMaster() == null)) {
	        _log.warn("Trying to set pet/summon target without master.");
	        _target = null;
	        return;
	    }
	    _target = target;
	}

	// TODO: Test hack?
	public void onTarget() {
		setActived(true);
		_targetItemList.clear();
		_targetItem = null;
		L1Character target = _target;

		if (target == null) {
		    _log.warn("L1NpcInstance::onTarget(): null target.");
		    return;
		}
		if (target instanceof L1PetInstance) {
		    L1PetInstance pet = (L1PetInstance) target;
		    if (pet.getMaster() == null) {
		        _log.warn("L1NpcInstance::onTarget(): pet target missing master.");
		        return;
		    }
		}
		if (target instanceof L1SummonInstance) {
		    L1SummonInstance summon = (L1SummonInstance) target;
		    if (summon.getMaster() == null) {
		        _log.warn("L1NpcInstance::onTarget(): summon target missing master.");
		        return;
		    }
		}

		if (getAtkspeed() == 0) {
			if (getPassispeed() > 0) {
				int escapeDistance = 15;
				if (hasSkillEffect(40)) {
					escapeDistance = 1;
				}
				if (getLocation().getTileLineDistance(target.getLocation()) > escapeDistance) {
					targetClear();
				} else {
					int dir = targetReverseDirection(target.getX(), target.getY());
					dir = checkObject(getX(), getY(), getMapId(), dir);
					setDirectionMove(dir);
					setSleepTime(calcSleepTime(getPassispeed(), MOVE_SPEED));
				}
			}
		} else {
			if (mobSkill.skillUse(target)) {
				setSleepTime(calcSleepTime(mobSkill.getSleepTime(), MAGIC_SPEED));
				return;
			}

			if (isAttackPosition(target.getX(), target.getY(), getNpcTemplate().get_ranged())) {
				setHeading(targetDirection(target.getX(), target.getY()));
				attackTarget(target);
			} else {
				if (getPassispeed() > 0) {
					if (getMapId() != target.getMapId()) {
						return;
					}
					int distance = getLocation().getTileDistance(target.getLocation());
					if (firstFound && getNpcTemplate().is_teleport() && distance > 3 && distance < 15) {
						if (nearTeleport(target.getX(), target.getY())) {
							firstFound = false;
							return;
						}
					}

					if (getNpcTemplate().is_teleport() && 20 > ThreadLocalRandom.current().nextInt(100)
							&& getCurrentMp() >= 10 && distance > 6 && distance < 15) {
						if (nearTeleport(target.getX(), target.getY())) {
							return;
						}
					}
					int dir = moveDirection(target.getX(), target.getY());
					if (dir == -1) {
						targetClear();
					} else {
						setDirectionMove(dir);
						setSleepTime(calcSleepTime(getPassispeed(), MOVE_SPEED));
					}
				} else {
					targetClear();
				}
			}
		}
	}

	public void attackTarget(L1Character target) {
		// if the pet/summon/pc is being attacked by another pet in a safety zone, then
		// kill the attack
		if (this instanceof L1PetInstance && (target instanceof L1PetInstance || target instanceof L1PcInstance
				|| target instanceof L1SummonInstance)) {
			if (target.getMap().isSafetyZone(this.getLocation())) {
				return;
			}
		}

		if (target instanceof L1PcInstance) {
			L1PcInstance player = (L1PcInstance) target;
			if (player.isTeleport()) {
				return;
			}
		} else if (target instanceof L1PetInstance) {
			L1PetInstance pet = (L1PetInstance) target;
			L1Character cha = pet.getMaster();
			if (cha instanceof L1PcInstance) {
				L1PcInstance player = (L1PcInstance) cha;
				if (player.isTeleport()) {
					return;
				}
			}
		} else if (target instanceof L1SummonInstance) {
			L1SummonInstance summon = (L1SummonInstance) target;
			L1Character cha = summon.getMaster();
			if (cha instanceof L1PcInstance) {
				L1PcInstance player = (L1PcInstance) cha;
				if (player.isTeleport()) {
					return;
				}
			}
		}
		if (this instanceof L1PetInstance) {
			L1PetInstance pet = (L1PetInstance) this;
			L1Character cha = pet.getMaster();
			if (cha instanceof L1PcInstance) {
				L1PcInstance player = (L1PcInstance) cha;
				if (player.isTeleport()) {
					return;
				}
			}
		} else if (this instanceof L1SummonInstance) {
			L1SummonInstance summon = (L1SummonInstance) this;
			L1Character cha = summon.getMaster();
			if (cha instanceof L1PcInstance) {
				L1PcInstance player = (L1PcInstance) cha;
				if (player.isTeleport()) {
					return;
				}
			}
		}

		if (target instanceof L1NpcInstance) {
			L1NpcInstance npc = (L1NpcInstance) target;
			if (npc.getHiddenStatus() != HIDDEN_STATUS_NONE) {
				allTargetClear();
				return;
			}
		}

		L1Attack attack = new L1Attack(this, target);

		if (target.hasSkillEffect(COUNTER_BARRIER)) {
			L1Magic magic = new L1Magic(target, this);
			if (magic.calcProbabilityMagic(COUNTER_BARRIER) && attack.isShortDistance()) {
				attack.actionCounterBarrier();
				attack.commitCounterBarrier();
				setSleepTime(calcSleepTime(getAtkspeed(), ATTACK_SPEED));
				return;
			}
		}

		if (attack.calcHit()) {
			attack.calcDamage();
			attack.addPcPoisonAttack(target, this);
		}

		attack.action();
		attack.commit();

		setSleepTime(calcSleepTime(getAtkspeed(), ATTACK_SPEED));
	}

	public void searchTargetItem() {
		ArrayList<L1GroundInventory> gInventorys = new ArrayList<>();

		for (L1Object obj : L1World.getInstance().getVisibleObjects(this)) {
			if (obj != null && obj instanceof L1GroundInventory) {
				gInventorys.add((L1GroundInventory) obj);
			}
		}
		if (gInventorys.size() == 0) {
			return;
		}

		int pickupIndex = (int) (Math.random() * gInventorys.size());
		L1GroundInventory inventory = gInventorys.get(pickupIndex);
		for (L1ItemInstance item : inventory.getItems()) {
			if (getInventory().checkAddItem(item, item.getCount()) == L1Inventory.OK) {
				_targetItem = item;
				_targetItemList.add(_targetItem);
			}
		}
	}

	public void searchItemFromAir() {
		ArrayList<L1GroundInventory> gInventorys = new ArrayList<>();

		for (L1Object obj : L1World.getInstance().getVisibleObjects(this)) {
			if (obj != null && obj instanceof L1GroundInventory) {
				gInventorys.add((L1GroundInventory) obj);
			}
		}
		if (gInventorys.size() == 0) {
			return;
		}

		int pickupIndex = (int) (Math.random() * gInventorys.size());
		L1GroundInventory inventory = gInventorys.get(pickupIndex);
		for (L1ItemInstance item : inventory.getItems()) {
			if (item.getItem().getType() == 6 // potion
					|| item.getItem().getType() == 7) { // food
				if (getInventory().checkAddItem(item, item.getCount()) == L1Inventory.OK) {
					if (getHiddenStatus() == HIDDEN_STATUS_FLY) {
						setHiddenStatus(HIDDEN_STATUS_NONE);
						broadcastPacket(new S_DoActionGFX(getId(), ActionCodes.ACTION_Movedown));
						setStatus(0);
						broadcastPacket(new S_NPCPack(this));
						onNpcAI();
						startChat(CHAT_TIMING_HIDE);
						_targetItem = item;
						_targetItemList.add(_targetItem);
					}
				}
			}
		}
	}

	public static void shuffle(L1Object[] arr) {
		for (int i = arr.length - 1; i > 0; i--) {
			int t = (int) (Math.random() * i);

			L1Object tmp = arr[i];
			arr[i] = arr[t];
			arr[t] = tmp;
		}
	}

	public void checkTargetItem() {
		if (_targetItem == null || _targetItem.getMapId() != getMapId()
				|| getLocation().getTileDistance(_targetItem.getLocation()) > 15) {
			if (!_targetItemList.isEmpty()) {
				_targetItem = _targetItemList.get(0);
				_targetItemList.remove(0);
				checkTargetItem();
			} else {
				_targetItem = null;
			}
		}
	}

	public void onTargetItem() {
		if (getLocation().getTileLineDistance(_targetItem.getLocation()) == 0) {
			pickupTargetItem(_targetItem);
		} else {
			int dir = moveDirection(_targetItem.getX(), _targetItem.getY());
			if (dir == -1) {
				_targetItemList.remove(_targetItem);
				_targetItem = null;
			} else {
				setDirectionMove(dir);
				setSleepTime(calcSleepTime(getPassispeed(), MOVE_SPEED));
			}
		}
	}

	public void pickupTargetItem(L1ItemInstance targetItem) {
		L1Inventory groundInventory = L1World.getInstance().getInventory(targetItem.getX(), targetItem.getY(),
				targetItem.getMapId());
		L1ItemInstance item = groundInventory.tradeItem(targetItem, targetItem.getCount(), getInventory());
		_targetItemList.remove(_targetItem);
		_targetItem = null;
		if (item == null)
			return;
		turnOnOffLight();
		int count = targetItem.getCount();
		onGetItem(item, count);
		setSleepTime(1000);
	}

	public boolean noTarget(int depth) {
		if (_master != null && _master.getMapId() == getMapId()
				&& getLocation().getTileLineDistance(_master.getLocation()) > 2) {
			int dir = moveDirection(_master.getX(), _master.getY());
			if (dir != -1) {
				setDirectionMove(dir);
				setSleepTime(calcSleepTime(getPassispeed(), MOVE_SPEED));
			} else {
				return true;
			}
		} else {
			if (Config.NPC_ACTIVE_RANGE == -1) {
				if (L1World.getInstance().getRecognizePlayer(this, true).size() == 0)
					return true;
			} else {
				// Once a player enters its screen make it start using a wider check to stay
				// active.
				// If they wider check fails to locate any players revert to the screen check.
				// It's more realistic when non-agro mobs can walk back on your screen a well as
				// off.
				if (!_awoken) {
					if (L1World.getInstance().getRecognizePlayer(this, true).size() == 0)
						return true;
					_awoken = true;
				} else if (L1World.getInstance().getVisiblePlayer(this, Config.NPC_ACTIVE_RANGE, true).size() == 0) {
					_awoken = false;
					return true;
				}
			}
			if (_master == null && getPassispeed() > 0 && !isRest()) {
				//
				L1MobGroupInfo mobGroupInfo = getMobGroupInfo();
				if (mobGroupInfo == null || mobGroupInfo != null && mobGroupInfo.isLeader(this)) {
					//
					//
					if (_randomMoveDistance == 0) {
						_randomMoveDistance = ThreadLocalRandom.current().nextInt(5) + 1;
						_randomMoveDirection = ThreadLocalRandom.current().nextInt(20);
						//
						if (getHomeX() != 0 && getHomeY() != 0 && _randomMoveDirection < 8
								&& ThreadLocalRandom.current().nextInt(3) == 0) {
							_randomMoveDirection = moveDirection(getHomeX(), getHomeY());
						}
					} else {
						_randomMoveDistance--;
					}
					int dir = checkObject(getX(), getY(), getMapId(), _randomMoveDirection);
					if (dir != -1) {
						setDirectionMove(dir);
						setSleepTime(calcSleepTime(getPassispeed(), MOVE_SPEED));
					}
				} else {
					L1NpcInstance leader = mobGroupInfo.getLeader();
					if (getLocation().getTileLineDistance(leader.getLocation()) > 2) {
						int dir = moveDirection(leader.getX(), leader.getY());
						if (dir == -1) {
							return true;
						} else {
							setDirectionMove(dir);
							setSleepTime(calcSleepTime(getPassispeed(), MOVE_SPEED));
						}
					}
				}
			}
		}
		return false;
	}

	public void onFinalAction(L1PcInstance pc, String s) {
	}

	public void targetClear() {
		if (_target == null) {
			return;
		}
		_hateList.remove(_target);
		_target = null;
	}

	public void removeTarget(L1Character target) {
		_hateList.remove(target);
		if (_target != null && _target.equals(target)) {
			_target = null;
		}
	}

	public void allTargetClear() {
		_hateList.clear();
		_dropHateList.clear();
		_target = null;
		_targetItemList.clear();
		_targetItem = null;
	}

	public void setMaster(L1Character cha) {
		_master = cha;
	}

	public L1Character getMaster() {
		return _master;
	}

	public void onNpcAI() {
	}

	private void consumeAndCreate(int[] ingredients, int[] ingredientCounts, int result, L1Inventory inventory) {
		if (!inventory.checkItem(ingredients, ingredientCounts))
			return;

		for (int i = 0; i < ingredients.length; i++)
			inventory.consumeItem(ingredients[i], ingredientCounts[i]);
		inventory.storeItem(result, 1);
	}

	private void makeIfAbsent(int[] ingredients, int[] ingredientCounts, int result, L1Inventory inventory) {
		if (inventory.checkItem(result))
			return;
		consumeAndCreate(ingredients, ingredientCounts, result, inventory);
	}

	private void refineItem() {
		if (getExp() == 0)
			return;

		if (getNpcId() == 45032) {
			makeIfAbsent(new int[] { 40508, 40521, 40045 }, new int[] { 150, 3, 3 }, 20, _inventory);

			makeIfAbsent(new int[] { 40494, 40521 }, new int[] { 150, 3 }, 19, _inventory);

			makeIfAbsent(new int[] { 40494, 40521 }, new int[] { 50, 1 }, 3, _inventory);

			makeIfAbsent(new int[] { 88, 40508, 40045 }, new int[] { 4, 80, 3 }, 100, _inventory);

			makeIfAbsent(new int[] { 88, 40494 }, new int[] { 2, 80 }, 89, _inventory);
			L1ItemInstance item = _inventory.findItemId(89);
			if (item != null && getNpcTemplate().get_digestitem() > 0)
				setDigestItem(item);

		} else if (getNpcId() == 81069) {
			makeIfAbsent(new int[] { 40032 }, new int[] { 1 }, 40542, _inventory);
		} else if (getNpcId() == 45166 || getNpcId() == 45167) {
			makeIfAbsent(new int[] { 40725 }, new int[] { 1 }, 40726, _inventory);
		}
	}

	private boolean _aiRunning = false;
	private boolean _actived = false;
	private boolean _firstAttack = false;
	private int _sleep_time;
	protected L1HateList _hateList = new L1HateList();
	protected L1HateList _dropHateList = new L1HateList();

	protected List<L1ItemInstance> _targetItemList = new ArrayList<>(0);
	protected L1Character _target = null;
	protected L1ItemInstance _targetItem = null;
	protected L1Character _master = null;
	private boolean _deathProcessing = false;
	ScheduledFuture<?> _mprTimerFuture;
	ScheduledFuture<?> _hprTimerFuture;
	private int _paralysisTime = 0; // Paralysis RestTime

	public void setParalysisTime(int ptime) {
		_paralysisTime = ptime;
	}

	public L1HateList getHateList() {
		return _hateList;
	}

	public int getParalysisTime() {
		return _paralysisTime;
	}

	// HP Regeneration
	public final void startHpRegeneration() {
		int hprInterval = getNpcTemplate().get_hprinterval();
		int hpr = getNpcTemplate().get_hpr();
		if (!_hprRunning && hprInterval > 0 && hpr > 0) {
			_hprTimer = new HprTimer(hpr);
			_hprTimerFuture = GeneralThreadPool.getInstance().scheduleAtFixedRate(_hprTimer, hprInterval, hprInterval);
			_hprRunning = true;
		}
	}

	public final void stopHpRegeneration() {
		if (_hprRunning) {
			_hprTimerFuture.cancel(true);
			_hprRunning = false;
		}
	}

	// MP Regeneration
	public final void startMpRegeneration() {
		int mprInterval = getNpcTemplate().get_mprinterval();
		int mpr = getNpcTemplate().get_mpr();

		if (!_mprRunning && mprInterval > 0 && mpr > 0) {
			_mprTimer = new MprTimer(mpr);
			_mprTimerFuture = GeneralThreadPool.getInstance().scheduleAtFixedRate(_mprTimer, mprInterval, mprInterval);
			_mprRunning = true;
		}
	}

	public final void stopMpRegeneration() {
		if (_mprRunning) {
			_mprTimerFuture.cancel(true);
			_mprRunning = false;
		}
	}

	private boolean _hprRunning = false;

	private HprTimer _hprTimer;

	class HprTimer implements Runnable {
		private String originalThreadName;

		@Override
		public void run() {
			originalThreadName = Thread.currentThread().getName();
			Thread.currentThread().setName("L1NpcInstance-HprTimer");
			try {
				if ((!_destroyed && !isDead()) && (getCurrentHp() > 0 && getCurrentHp() < getMaxHp())) {
					setCurrentHp(getCurrentHp() + _point);
				} else {
					stopHpRegeneration();
				}
			} catch (Exception e) {
				_log.error(e.getLocalizedMessage(), e);
			} finally {
				Thread.currentThread().setName(originalThreadName);
			}
		}

		public HprTimer(int point) {
			if (point < 1) {
				point = 1;
			}
			_point = point;
		}

		private final int _point;
	}

	private boolean _mprRunning = false;

	private MprTimer _mprTimer;

	class MprTimer implements Runnable {
		private String originalThreadName;

		@Override
		public void run() {
			originalThreadName = Thread.currentThread().getName();
			Thread.currentThread().setName("L1NpcInstance-MprTimer");
			try {
				if ((!_destroyed && !isDead()) && (getCurrentHp() > 0 && getCurrentMp() < getMaxMp())) {
					setCurrentMp(getCurrentMp() + _point);
				} else {
					stopMpRegeneration();
				}
			} catch (Exception e) {
				_log.error(e.getLocalizedMessage(), e);
			} finally {
				Thread.currentThread().setName(originalThreadName);
			}
		}

		public MprTimer(int point) {
			if (point < 1) {
				point = 1;
			}
			_point = point;
		}

		private final int _point;
	}

	private Map<Integer, Integer> _digestItems;
	public boolean _digestItemRunning = false;

	class DigestItemTimer implements Runnable {
		private String originalThreadName;

		@Override
		public void run() {
			try {
				originalThreadName = Thread.currentThread().getName();
				Thread.currentThread().setName("L1NpcInstance-DigestTimer");
				_digestItemRunning = true;
				while (!_destroyed && _digestItems.size() > 0) {
					try {
						Thread.sleep(1000);
					} catch (Exception exception) {
						break;
					}

					Object[] keys = _digestItems.keySet().toArray();
					for (Object key2 : keys) {
						Integer key = (Integer) key2;
						Integer digestCounter = _digestItems.get(key);
						digestCounter -= 1;
						if (digestCounter <= 0) {
							_digestItems.remove(key);
							L1ItemInstance digestItem = getInventory().getItem(key);
							if (digestItem != null) {
								getInventory().removeItem(digestItem, digestItem.getCount());
							}
						} else {
							_digestItems.put(key, digestCounter);
						}
					}
				}
				_digestItemRunning = false;
			} catch (Exception e) {
				_log.error("", e);
			} finally {
				Thread.currentThread().setName(originalThreadName);
			}
		}
	}

	public L1NpcInstance(L1Npc template) {
		setStatus(0);
		setMoveSpeed(0);
		setDead(false);
		setStatus(0);
		setreSpawn(false);

		if (template != null) {
			setting_template(template);
		}
	}

	public void setting_template(L1Npc template) {
		_npcTemplate = template;
		int randomlevel = 0;
		double rate = 0;
		double diff = 0;
		setName(template.get_name());
		setNameId(template.get_nameid());

		if (template.get_randomlevel() == 0) {
			setLevel(template.get_level());
		} else {
			randomlevel = ThreadLocalRandom.current().nextInt(template.get_randomlevel() - template.get_level() + 1);
			diff = template.get_randomlevel() - template.get_level();
			rate = randomlevel / diff;
			randomlevel += template.get_level();
			setLevel(randomlevel);
		}
		if (template.get_randomhp() == 0) {
			setMaxHp(template.get_hp());
			setCurrentHpDirect(template.get_hp());
		} else {
			double randomhp = rate * (template.get_randomhp() - template.get_hp());
			int hp = (int) (template.get_hp() + randomhp);
			setMaxHp(hp);
			setCurrentHpDirect(hp);
		}
		if (template.get_randommp() == 0) {
			setMaxMp(template.get_mp());
			setCurrentMpDirect(template.get_mp());
		} else {
			double randommp = rate * (template.get_randommp() - template.get_mp());
			int mp = (int) (template.get_mp() + randommp);
			setMaxMp(mp);
			setCurrentMpDirect(mp);
		}
		if (template.get_randomac() == 0) {
			setAc(template.get_ac());
		} else {
			double randomac = rate * (template.get_randomac() - template.get_ac());
			int ac = (int) (template.get_ac() + randomac);
			setAc(ac);
		}
		if (template.get_randomlevel() == 0) {
			setStr(template.get_str());
			setCon(template.get_con());
			setDex(template.get_dex());
			setInt(template.get_int());
			setWis(template.get_wis());
			setMr(template.get_mr());
		} else {
			setStr((byte) Math.min(template.get_str() + diff, 127));
			setCon((byte) Math.min(template.get_con() + diff, 127));
			setDex((byte) Math.min(template.get_dex() + diff, 127));
			setInt((byte) Math.min(template.get_int() + diff, 127));
			setWis((byte) Math.min(template.get_wis() + diff, 127));
			setMr((byte) Math.min(template.get_mr() + diff, 127));

			addHitup((int) diff * 2);
			addDmgup((int) diff * 2);
		}
		setPassispeed(template.get_passispeed());
		setAtkspeed(template.get_atkspeed());
		setAgro(template.is_agro());
		setAgrocoi(template.is_agrocoi());
		setAgrososc(template.is_agrososc());
		setTempCharGfx(template.get_gfxid());
		setGfxId(template.get_gfxid());
		if (template.get_randomexp() == 0) {
			setExp(template.get_exp());
		} else {
			int level = getLevel();
			int exp = level * level;
			exp += 1;
			setExp(exp);
		}
		if (template.get_randomlawful() == 0) {
			setLawful(template.get_lawful());
			setTempLawful(template.get_lawful());
		} else {
			double randomlawful = rate * (template.get_randomlawful() - template.get_lawful());
			int lawful = (int) (template.get_lawful() + randomlawful);
			setLawful(lawful);
			setTempLawful(lawful);
		}
		setPickupItem(template.is_picupitem());
		if (template.is_bravespeed()) {
			setBraveSpeed(1);
		} else {
			setBraveSpeed(0);
		}
		if (template.get_digestitem() > 0) {
			_digestItems = new HashMap<>();
		}
		setKarma(template.getKarma());
		setLightSize(template.getLightSize());
		setAgrochao(template.is_agrochao());
		mobSkill = new L1MobSkillUse(this);
	}

	private int _passispeed;

	public int getPassispeed() {
		return _passispeed;
	}

	public void setPassispeed(int i) {
		_passispeed = i;
	}

	private int _atkspeed;

	public int getAtkspeed() {
		return _atkspeed;
	}

	public void setAtkspeed(int i) {
		_atkspeed = i;
	}

	private boolean _pickupItem;

	public boolean isPickupItem() {
		return _pickupItem;
	}

	public void setPickupItem(boolean flag) {
		_pickupItem = flag;
	}

	@Override
	public L1Inventory getInventory() {
		return _inventory;
	}

	public void setInventory(L1Inventory inventory) {
		_inventory = inventory;
	}

	public L1Npc getNpcTemplate() {
		return _npcTemplate;
	}

	public int getNpcId() {
		return _npcTemplate.get_npcId();
	}

	public void setPetcost(int i) {
		_petcost = i;
	}

	public int getPetcost() {
		return _petcost;
	}

	public void setSpawn(L1Spawn spawn) {
		_spawn = spawn;
	}

	public L1Spawn getSpawn() {
		return _spawn;
	}

	public void setSpawnNumber(int number) {
		_spawnNumber = number;
	}

	public int getSpawnNumber() {
		return _spawnNumber;
	}

	public void onDecay(boolean isReuseId) {
		int id = 0;
		if (isReuseId) {
			id = getId();
		} else {
			id = 0;
		}
		_spawn.executeSpawnTask(_spawnNumber, id);
	}

	@Override
	public void onPerceive(L1PcInstance perceivedFrom) {
		// Cheap fix because GMRoom is also the jail.. this hides the bugboard
		if (this.getSpawn() == NpcSpawnTable.getBugBoard() && !perceivedFrom.isGm())
			return;

		perceivedFrom.sendPackets(new S_Light(this.getId(), getLightSize()));
		perceivedFrom.addKnownObject(this);
		perceivedFrom.sendPackets(new S_NPCPack(this));
		onNpcAI();
	}

	public void deleteMe() {
		if (getNpcId() == 45477 && !_truedead) {
			resurrect(getMaxHp() / 2);
			_truedead = true;
		} else {
			_destroyed = true;
			if (getInventory() != null) {
				getInventory().clearItems();
			}
			allTargetClear();
			stopHpRegeneration();
			stopMpRegeneration();
			_master = null;
			L1World.getInstance().removeVisibleObject(this);
			L1World.getInstance().removeObject(this);
			List<L1PcInstance> players = L1World.getInstance().getRecognizePlayer(this);

			if (players.size() > 0) {
				S_RemoveObject s_deleteNewObject = new S_RemoveObject(this);
				for (L1PcInstance pc : players) {
					if (pc != null) {
						pc.removeKnownObject(this);
						pc.sendPackets(s_deleteNewObject);
					}
				}
			}
			removeAllKnownObjects();

			// TODO: test!
			if (_chatTaskFuture != null) {
				if (!_chatTaskFuture.isDone()) {
					_chatTaskFuture.cancel(true);
					_chatTask = null;
				}
			}
//		if (_chatTask != null) {
//			_chatTask.cancel();
//			_chatTask = null;
//		}
//		if (_chatTimer != null) {
//			_chatTimer.cancel();
//			_chatTimer.purge();
//			_chatTimer = null;
//		}

			L1MobGroupInfo mobGroupInfo = getMobGroupInfo();
			if (mobGroupInfo == null) {
				if (isReSpawn()) {
					onDecay(true);
				}
			} else {
				if (mobGroupInfo.removeMember(this) == 0) {
					setMobGroupInfo(null);
					if (isReSpawn()) {
						onDecay(false);
					}
				}
			}
		}
	}

	public void ReceiveManaDamage(L1Character attacker, int damageMp) {
	}

	public void receiveDamage(L1Character attacker, int damage) {
	}

	public void setDigestItem(L1ItemInstance item) {
		_digestItems.put(new Integer(item.getId()), new Integer(getNpcTemplate().get_digestitem()));
		if (!_digestItemRunning) {
			DigestItemTimer digestItemTimer = new DigestItemTimer();
			GeneralThreadPool.getInstance().execute(digestItemTimer);
		}
	}

	public void onGetItem(L1ItemInstance item, int count) {
		refineItem();
		getInventory().shuffle();
		if (getNpcTemplate().get_digestitem() > 0) {
			setDigestItem(item);
		}
	}

	public void approachPlayer(L1PcInstance pc) {
		if (pc.hasSkillEffect(60) || pc.hasSkillEffect(97)) {
			return;
		}
		if (getHiddenStatus() == HIDDEN_STATUS_SINK) {
			if (getCurrentHp() == getMaxHp()) {
				if (pc.getLocation().getTileLineDistance(this.getLocation()) <= 2) {
					appearOnGround(pc);
				}
			}
		} else if (getHiddenStatus() == HIDDEN_STATUS_FLY) {
			if (getCurrentHp() == getMaxHp()) {
				if (pc.getLocation().getTileLineDistance(this.getLocation()) <= 1) {
					appearOnGround(pc);
				}
			} else {
				// if (getNpcTemplate().get_npcId() != 45681) { // hrIO
				searchItemFromAir();
				// }
			}
		} else if (getHiddenStatus() == HIDDEN_STATUS_ICE) {
			if (getCurrentHp() < getMaxHp()) {
				appearOnGround(pc);
			}
		}
	}

	public void appearOnGround(L1PcInstance pc) {
		// fix for invis gms not to de-sink hidden mops
		if (pc.isGmInvis()) {
			return;
		} else if (getHiddenStatus() == HIDDEN_STATUS_SINK) {
			setHiddenStatus(HIDDEN_STATUS_NONE);
			broadcastPacket(new S_DoActionGFX(getId(), ActionCodes.ACTION_Appear));
			setStatus(0);
			broadcastPacket(new S_NPCPack(this));
			if (!pc.hasSkillEffect(60) && !pc.hasSkillEffect(97) && !pc.isGm()) {
				_hateList.add(pc, 0);
				_target = pc;
			}
			onNpcAI();
		} else if (getHiddenStatus() == HIDDEN_STATUS_FLY) {
			setHiddenStatus(HIDDEN_STATUS_NONE);
			broadcastPacket(new S_DoActionGFX(getId(), ActionCodes.ACTION_Movedown));
			setStatus(0);
			broadcastPacket(new S_NPCPack(this));
			if (!pc.hasSkillEffect(60) && !pc.hasSkillEffect(97) && !pc.isGm()) {
				_hateList.add(pc, 0);
				_target = pc;
			}
			onNpcAI();
			startChat(CHAT_TIMING_HIDE);
		}
	}

	public void setDirectionMove(int dir) {
		if (dir >= 0) {
			int nx = 0;
			int ny = 0;

			switch (dir) {
			case 1:
				nx = 1;
				ny = -1;
				setHeading(1);
				break;

			case 2:
				nx = 1;
				ny = 0;
				setHeading(2);
				break;

			case 3:
				nx = 1;
				ny = 1;
				setHeading(3);
				break;

			case 4:
				nx = 0;
				ny = 1;
				setHeading(4);
				break;

			case 5:
				nx = -1;
				ny = 1;
				setHeading(5);
				break;

			case 6:
				nx = -1;
				ny = 0;
				setHeading(6);
				break;

			case 7:
				nx = -1;
				ny = -1;
				setHeading(7);
				break;

			case 0:
				nx = 0;
				ny = -1;
				setHeading(0);
				break;

			default:
				break;

			}

			/*
			 * This code section would prevent mobs from walking through doors, however,
			 * it's iterating through a for loop of every spawned door in game for every
			 * tile based movement of every mob in game. That's no good.
			 *
			 * However, something like this might be necessary if we want mobs to be able to
			 * open doors in the future. So I'm leaving this code in place but commented
			 * out. -Tricid
			 *
			 * PS. It's surrounded by try because its possible (and likely) it'll find no
			 * door, throwing a null pointer exception.
			 *
			 * PPS. If this is ever used, the additions in L1DoorInstance.java and
			 * L1DoorTable.java that set the doors location impassible need to be removed.
			 */

			/*
			 * try { if (DoorTable .getInstance() .findByDoorLoc(getX() + nx, getY() + ny,
			 * getMap().getId()).getOpenStatus() == 29) { return; } } catch (Exception e) {
			 *
			 * }
			 */

			getMap().setPassable(getLocation(), true);

			int nnx = getX() + nx;
			int nny = getY() + ny;
			setX(nnx);
			setY(nny);

			getMap().setPassable(getLocation(), false);

			broadcastPacket(new S_MoveCharPacket(this));

			if (getMovementDistance() > 0) {
				if (this instanceof L1GuardInstance || this instanceof L1MerchantInstance
						|| this instanceof L1MonsterInstance) {
					if (getLocation().getLineDistance(new Point(getHomeX(), getHomeY())) > getMovementDistance()) {
						teleport(getHomeX(), getHomeY(), getHeading());
					}
				}
			}
			//
			if (getNpcTemplate().get_npcId() >= 45912 && getNpcTemplate().get_npcId() <= 45916) {
				if (getX() >= 32591 && getX() <= 32644 && getY() >= 32643 && getY() <= 32688 && getMapId() == 4) {
					teleport(getHomeX(), getHomeY(), getHeading());
				}
			}
		}
	}

	public int moveDirection(int x, int y) {
		return moveDirection(x, y, getLocation().getLineDistance(new Point(x, y)));
	}

	public int moveDirection(int x, int y, double d) {
		int dir = 0;
		if (hasSkillEffect(40) && d >= 2D) {
			return -1;
			// } else if (d > 30D) {
			// The distance at which mobs will give up the chase.
			// This should be relative to the pathing range.
		} else if (d > 15.0 + Config.NPC_PATHING_RANGE) {
			return -1;
		} else if (d > Config.NPC_PATHING_RANGE) {
			dir = targetDirection(x, y);
			dir = checkObject(getX(), getY(), getMapId(), dir);
		} else {
			dir = _serchCource(x, y);
			if (dir == -1) {
				dir = targetDirection(x, y);
				if (!isExsistCharacterBetweenTarget(dir)) {
					dir = checkObject(getX(), getY(), getMapId(), dir);
				}
			}
		}
		return dir;
	}

	private boolean isExsistCharacterBetweenTarget(int dir) {
		if (!(this instanceof L1MonsterInstance) || (_target == null)) { //
			return false;
		}

		int locX = getX();
		int locY = getY();
		int targetX = locX;
		int targetY = locY;

		if (dir == 1) {
			targetX = locX + 1;
			targetY = locY - 1;
		} else if (dir == 2) {
			targetX = locX + 1;
		} else if (dir == 3) {
			targetX = locX + 1;
			targetY = locY + 1;
		} else if (dir == 4) {
			targetY = locY + 1;
		} else if (dir == 5) {
			targetX = locX - 1;
			targetY = locY + 1;
		} else if (dir == 6) {
			targetX = locX - 1;
		} else if (dir == 7) {
			targetX = locX - 1;
			targetY = locY - 1;
		} else if (dir == 0) {
			targetY = locY - 1;
		}

		for (L1Object object : L1World.getInstance().getVisibleObjects(this, 1)) {
			// PC, Summon, Pet
			if (object instanceof L1PcInstance || object instanceof L1SummonInstance
					|| object instanceof L1PetInstance) {
				L1Character cha = (L1Character) object;
				if (cha.getX() == targetX && cha.getY() == targetY && cha.getMapId() == getMapId()) {
					if (object instanceof L1PcInstance) {
						L1PcInstance pc = (L1PcInstance) object;
						if (pc.isGhost()) { //
							continue;
						}
					}
					_hateList.add(cha, 0);
					_target = cha;
					return true;
				}
			}
		}
		return false;
	}

	public int targetReverseDirection(int tx, int ty) {
		int dir = targetDirection(tx, ty);
		dir += 4;
		if (dir > 7) {
			dir -= 8;
		}
		return dir;
	}

	public static int checkObject(int x, int y, short m, int d) {
		L1Map map = L1WorldMap.getInstance().getMap(m);
		if (d == 1) {
			if (map.isPassable(x, y, 1)) {
				return 1;
			} else if (map.isPassable(x, y, 0)) {
				return 0;
			} else if (map.isPassable(x, y, 2)) {
				return 2;
			}
		} else if (d == 2) {
			if (map.isPassable(x, y, 2)) {
				return 2;
			} else if (map.isPassable(x, y, 1)) {
				return 1;
			} else if (map.isPassable(x, y, 3)) {
				return 3;
			}
		} else if (d == 3) {
			if (map.isPassable(x, y, 3)) {
				return 3;
			} else if (map.isPassable(x, y, 2)) {
				return 2;
			} else if (map.isPassable(x, y, 4)) {
				return 4;
			}
		} else if (d == 4) {
			if (map.isPassable(x, y, 4)) {
				return 4;
			} else if (map.isPassable(x, y, 3)) {
				return 3;
			} else if (map.isPassable(x, y, 5)) {
				return 5;
			}
		} else if (d == 5) {
			if (map.isPassable(x, y, 5)) {
				return 5;
			} else if (map.isPassable(x, y, 4)) {
				return 4;
			} else if (map.isPassable(x, y, 6)) {
				return 6;
			}
		} else if (d == 6) {
			if (map.isPassable(x, y, 6)) {
				return 6;
			} else if (map.isPassable(x, y, 5)) {
				return 5;
			} else if (map.isPassable(x, y, 7)) {
				return 7;
			}
		} else if (d == 7) {
			if (map.isPassable(x, y, 7)) {
				return 7;
			} else if (map.isPassable(x, y, 6)) {
				return 6;
			} else if (map.isPassable(x, y, 0)) {
				return 0;
			}
		} else if (d == 0) {
			if (map.isPassable(x, y, 0)) {
				return 0;
			} else if (map.isPassable(x, y, 7)) {
				return 7;
			} else if (map.isPassable(x, y, 1)) {
				return 1;
			}
		}
		return -1;
	}

	private int _serchCource(int x, int y) {
		int i;
		int locCenter = Config.NPC_PATHING_RANGE + 1;
		int diff_x = x - locCenter;
		int diff_y = y - locCenter;
		int[] locBace = { getX() - diff_x, getY() - diff_y, 0, 0 };
		int[] locNext = new int[4];
		int[] locCopy;
		int[] dirFront = new int[5];
		boolean serchMap[][] = new boolean[locCenter * 2 + 1][locCenter * 2 + 1];
		LinkedList<int[]> queueSerch = new LinkedList<>();

		for (int j = Config.NPC_PATHING_RANGE * 2 + 1; j > 0; j--) {
			for (i = Config.NPC_PATHING_RANGE - Math.abs(locCenter - j); i >= 0; i--) {
				serchMap[j][locCenter + i] = true;
				serchMap[j][locCenter - i] = true;
			}
		}

		int[] firstCource = { 2, 4, 6, 0, 1, 3, 5, 7 };
		for (i = 0; i < 8; i++) {
			System.arraycopy(locBace, 0, locNext, 0, 4);
			_moveLocation(locNext, firstCource[i]);
			if (locNext[0] - locCenter == 0 && locNext[1] - locCenter == 0) {
				return firstCource[i];
			}
			if (serchMap[locNext[0]][locNext[1]]) {
				int tmpX = locNext[0] + diff_x;
				int tmpY = locNext[1] + diff_y;
				boolean found = false;
				if (i == 0) {
					found = getMap().isPassable(tmpX, tmpY + 1, i);
				} else if (i == 1) {
					found = getMap().isPassable(tmpX - 1, tmpY + 1, i);
				} else if (i == 2) {
					found = getMap().isPassable(tmpX - 1, tmpY, i);
				} else if (i == 3) {
					found = getMap().isPassable(tmpX - 1, tmpY - 1, i);
				} else if (i == 4) {
					found = getMap().isPassable(tmpX, tmpY - 1, i);
				} else if (i == 5) {
					found = getMap().isPassable(tmpX + 1, tmpY - 1, i);
				} else if (i == 6) {
					found = getMap().isPassable(tmpX + 1, tmpY, i);
				} else if (i == 7) {
					found = getMap().isPassable(tmpX + 1, tmpY + 1, i);
				}
				if (found) {
					locCopy = new int[4];
					System.arraycopy(locNext, 0, locCopy, 0, 4);
					locCopy[2] = firstCource[i];
					locCopy[3] = firstCource[i];
					queueSerch.add(locCopy);
				}
				serchMap[locNext[0]][locNext[1]] = false;
			}
		}
		locBace = null;

		while (queueSerch.size() > 0) {
			locBace = queueSerch.removeFirst();
			_getFront(dirFront, locBace[2]);
			for (i = 4; i >= 0; i--) {
				System.arraycopy(locBace, 0, locNext, 0, 4);
				_moveLocation(locNext, dirFront[i]);
				if (locNext[0] - locCenter == 0 && locNext[1] - locCenter == 0) {
					return locNext[3];
				}
				if (serchMap[locNext[0]][locNext[1]]) {
					int tmpX = locNext[0] + diff_x;
					int tmpY = locNext[1] + diff_y;
					boolean found = false;
					if (i == 0) {
						found = getMap().isPassable(tmpX, tmpY + 1, i);
					} else if (i == 1) {
						found = getMap().isPassable(tmpX - 1, tmpY + 1, i);
					} else if (i == 2) {
						found = getMap().isPassable(tmpX - 1, tmpY, i);
					} else if (i == 3) {
						found = getMap().isPassable(tmpX - 1, tmpY - 1, i);
					} else if (i == 4) {
						found = getMap().isPassable(tmpX, tmpY - 1, i);
					}
					if (found) {
						locCopy = new int[4];
						System.arraycopy(locNext, 0, locCopy, 0, 4);
						locCopy[2] = dirFront[i];
						queueSerch.add(locCopy);
					}
					serchMap[locNext[0]][locNext[1]] = false;
				}
			}
			locBace = null;
		}
		return -1;
	}

	private void _moveLocation(int[] ary, int d) {
		if (d == 1) {
			ary[0] = ary[0] + 1;
			ary[1] = ary[1] - 1;
		} else if (d == 2) {
			ary[0] = ary[0] + 1;
		} else if (d == 3) {
			ary[0] = ary[0] + 1;
			ary[1] = ary[1] + 1;
		} else if (d == 4) {
			ary[1] = ary[1] + 1;
		} else if (d == 5) {
			ary[0] = ary[0] - 1;
			ary[1] = ary[1] + 1;
		} else if (d == 6) {
			ary[0] = ary[0] - 1;
		} else if (d == 7) {
			ary[0] = ary[0] - 1;
			ary[1] = ary[1] - 1;
		} else if (d == 0) {
			ary[1] = ary[1] - 1;
		}
		ary[2] = d;
	}

	private void _getFront(int[] ary, int d) {
		if (d == 1) {
			ary[4] = 2;
			ary[3] = 0;
			ary[2] = 1;
			ary[1] = 3;
			ary[0] = 7;
		} else if (d == 2) {
			ary[4] = 2;
			ary[3] = 4;
			ary[2] = 0;
			ary[1] = 1;
			ary[0] = 3;
		} else if (d == 3) {
			ary[4] = 2;
			ary[3] = 4;
			ary[2] = 1;
			ary[1] = 3;
			ary[0] = 5;
		} else if (d == 4) {
			ary[4] = 2;
			ary[3] = 4;
			ary[2] = 6;
			ary[1] = 3;
			ary[0] = 5;
		} else if (d == 5) {
			ary[4] = 4;
			ary[3] = 6;
			ary[2] = 3;
			ary[1] = 5;
			ary[0] = 7;
		} else if (d == 6) {
			ary[4] = 4;
			ary[3] = 6;
			ary[2] = 0;
			ary[1] = 5;
			ary[0] = 7;
		} else if (d == 7) {
			ary[4] = 6;
			ary[3] = 0;
			ary[2] = 1;
			ary[1] = 5;
			ary[0] = 7;
		} else if (d == 0) {
			ary[4] = 2;
			ary[3] = 6;
			ary[2] = 0;
			ary[1] = 1;
			ary[0] = 7;
		}
	}

	private void useHealPotion(int healHp, int effectId) {
		broadcastPacket(new S_SkillSound(getId(), effectId));
		if (this.hasSkillEffect(POLLUTE_WATER)) {
			healHp /= 2;
		}
		if (this instanceof L1PetInstance) {
			((L1PetInstance) this).setCurrentHp(getCurrentHp() + healHp);
		} else if (this instanceof L1SummonInstance) {
			((L1SummonInstance) this).setCurrentHp(getCurrentHp() + healHp);
		} else {
			setCurrentHpDirect(getCurrentHp() + healHp);
		}
	}

	private void useHastePotion(int time) {
		broadcastPacket(new S_SkillHaste(getId(), 1, time));
		broadcastPacket(new S_SkillSound(getId(), 191));
		setMoveSpeed(1);
		setSkillEffect(STATUS_HASTE, time * 1000);
	}

	public static final int USEITEM_HEAL = 0;
	public static final int USEITEM_HASTE = 1;
	public static int[] healPotions = { POTION_OF_GREATER_HEALING, POTION_OF_EXTRA_HEALING, POTION_OF_HEALING };
	public static int[] hastePotions = { B_POTION_OF_GREATER_HASTE_SELF, POTION_OF_GREATER_HASTE_SELF,
			B_POTION_OF_HASTE_SELF, POTION_OF_HASTE_SELF };

	public void useItem(int type, int chance) {
		if (hasSkillEffect(71)) {
			return;
		}

		new Random();
		if (ThreadLocalRandom.current().nextInt(100) > chance) {
			return;
		}

		if (type == USEITEM_HEAL) {
			if (getInventory().consumeItem(POTION_OF_GREATER_HEALING, 1)) {
				useHealPotion(75, 197);
			} else if (getInventory().consumeItem(POTION_OF_EXTRA_HEALING, 1)) {
				useHealPotion(45, 194);
			} else if (getInventory().consumeItem(POTION_OF_HEALING, 1)) {
				useHealPotion(15, 189);
			}
		} else if (type == USEITEM_HASTE) {
			if (hasSkillEffect(1001)) {
				return;
			}

			if (getInventory().consumeItem(B_POTION_OF_GREATER_HASTE_SELF, 1)) {
				useHastePotion(2100);
			} else if (getInventory().consumeItem(POTION_OF_GREATER_HASTE_SELF, 1)) {
				useHastePotion(1800);
			} else if (getInventory().consumeItem(B_POTION_OF_HASTE_SELF, 1)) {
				useHastePotion(350);
			} else if (getInventory().consumeItem(POTION_OF_HASTE_SELF, 1)) {
				useHastePotion(300);
			}
		}
	}

	// Npc Skills
	public boolean nearTeleport(int nx, int ny) {
		int rdir = ThreadLocalRandom.current().nextInt(8);
		int dir;
		for (int i = 0; i < 8; i++) {
			dir = rdir + i;
			if (dir > 7) {
				dir -= 8;
			}
			if (dir == 1) {
				nx++;
				ny--;
			} else if (dir == 2) {
				nx++;
			} else if (dir == 3) {
				nx++;
				ny++;
			} else if (dir == 4) {
				ny++;
			} else if (dir == 5) {
				nx--;
				ny++;
			} else if (dir == 6) {
				nx--;
			} else if (dir == 7) {
				nx--;
				ny--;
			} else if (dir == 0) {
				ny--;
			}
			if (getMap().isPassable(nx, ny)) {
				dir += 4;
				if (dir > 7) {
					dir -= 8;
				}
				teleport(nx, ny, dir);
				setCurrentMp(getCurrentMp() - 10);
				return true;
			}
		}
		return false;
	}

	public void teleport(int nx, int ny, int dir) {
		for (L1PcInstance pc : L1World.getInstance().getRecognizePlayer(this)) {
			pc.sendPackets(new S_SkillSound(getId(), 169));
			pc.sendPackets(new S_RemoveObject(this));
			pc.removeKnownObject(this);
		}
		setX(nx);
		setY(ny);
		setHeading(dir);
	}

	// ----------From L1Character-------------
	private String _nameId;

	public String getNameId() {
		return _nameId;
	}

	public void setNameId(String s) {
		_nameId = s;
	}

	private boolean _Agro;

	public boolean isAgro() {
		return _Agro;
	}

	public void setAgro(boolean flag) {
		_Agro = flag;
	}

	private boolean _Agrochao;

	public boolean isAgrochao() {
		return _Agrochao;
	}

	public void setAgrochao(boolean flag) {
		_Agrochao = flag;
	}

	private boolean _Agrocoi;

	public boolean isAgrocoi() {
		return _Agrocoi;
	}

	public void setAgrocoi(boolean flag) {
		_Agrocoi = flag;
	}

	private boolean _Agrososc;

	public boolean isAgrososc() {
		return _Agrososc;
	}

	public void setAgrososc(boolean flag) {
		_Agrososc = flag;
	}

	private int _homeX;

	public int getHomeX() {
		return _homeX;
	}

	public void setHomeX(int i) {
		_homeX = i;
	}

	private int _homeY;

	public int getHomeY() {
		return _homeY;
	}

	public void setHomeY(int i) {
		_homeY = i;
	}

	private boolean _reSpawn;

	public boolean isReSpawn() {
		return _reSpawn;
	}

	public void setreSpawn(boolean flag) {
		_reSpawn = flag;
	}

	private int _lightSize;

	public int getLightSize() {
		return _lightSize;
	}

	public void setLightSize(int i) {
		_lightSize = i;
	}

	private boolean _weaponBreaked;

	public boolean isWeaponBreaked() {
		return _weaponBreaked;
	}

	public void setWeaponBreaked(boolean flag) {
		_weaponBreaked = flag;
	}

	private int _hiddenStatus;

	public int getHiddenStatus() {
		return _hiddenStatus;
	}

	public void setHiddenStatus(int i) {
		_hiddenStatus = i;
	}

	private int _movementDistance = 0;

	public int getMovementDistance() {
		return _movementDistance;
	}

	public void setMovementDistance(int i) {
		_movementDistance = i;
	}

	private int _tempLawful = 0;

	public int getTempLawful() {
		return _tempLawful;
	}

	public void setTempLawful(int i) {
		_tempLawful = i;
	}

	protected int calcSleepTime(int sleepTime, int type) {
		switch (getMoveSpeed()) {
		case 0:
			break;
		case 1:
			sleepTime -= (sleepTime * 0.25);
			break;
		case 2:
			sleepTime *= 2;
			break;
		}
		if (getBraveSpeed() == 1) {
			sleepTime -= (sleepTime * 0.25);
		}
		if (hasSkillEffect(L1SkillId.WIND_SHACKLE)) {
			if (type == ATTACK_SPEED || type == MAGIC_SPEED) {
				sleepTime += (sleepTime * 0.25);
			}
		}
		return sleepTime;
	}

	protected void setAiRunning(boolean aiRunning) {
		_aiRunning = aiRunning;
	}

	protected boolean isAiRunning() {
		return _aiRunning;
	}

	protected void setActived(boolean actived) {
		_actived = actived;
	}

	protected boolean isActived() {
		return _actived;
	}

	protected void setFirstAttack(boolean firstAttack) {
		_firstAttack = firstAttack;
	}

	protected boolean isFirstAttack() {
		return _firstAttack;
	}

	protected void setSleepTime(int sleep_time) {
		_sleep_time = sleep_time;
	}

	protected int getSleepTime() {
		return _sleep_time;
	}

	protected void setDeathProcessing(boolean deathProcessing) {
		_deathProcessing = deathProcessing;
	}

	protected boolean isDeathProcessing() {
		return _deathProcessing;
	}

	public int drainMana(int drain) {
		if (_drainedMana >= Config.MANA_DRAIN_LIMIT_PER_NPC) {
			return 0;
		}
		int result = Math.min(drain, getCurrentMp());
		if (_drainedMana + result > Config.MANA_DRAIN_LIMIT_PER_NPC) {
			result = Config.MANA_DRAIN_LIMIT_PER_NPC - _drainedMana;
		}
		_drainedMana += result;
		return result;
	}

	public boolean _destroyed = false;

	protected void transform(int transformId) {
		stopHpRegeneration();
		stopMpRegeneration();
		int transformGfxId = getNpcTemplate().getTransformGfxId();
		if (transformGfxId != 0) {
			broadcastPacket(new S_SkillSound(getId(), transformGfxId));
		}
		L1Npc npcTemplate = NpcTable.getInstance().getTemplate(transformId);
		setting_template(npcTemplate);

		broadcastPacket(new S_ChangeShape(getId(), getTempCharGfx()));
		for (L1PcInstance pc : L1World.getInstance().getRecognizePlayer(this)) {
			onPerceive(pc);
		}

	}

	public void setRest(boolean _rest) {
		this._rest = _rest;
	}

	public boolean isRest() {
		return _rest;
	}

	private boolean _isResurrect;

	public boolean isResurrect() {
		return _isResurrect;
	}

	public void setResurrect(boolean flag) {
		_isResurrect = flag;
	}

	@Override
	public synchronized void resurrect(int hp) {
		if (_destroyed) {
			return;
		}
		if (_deleteTask != null) {
			if (!_future.cancel(false)) {
				return;
			}
			_deleteTask = null;
			_future = null;
		}
		super.resurrect(hp);

		L1SkillUse skill = new L1SkillUse();
		skill.handleCommands(null, CANCELLATION, getId(), getX(), getY(), null, 0, L1SkillUse.TYPE_LOGIN, this);
	}

	private DeleteTimer _deleteTask;
	private ScheduledFuture<?> _future = null;

	protected synchronized void startDeleteTimer() {
		if (_deleteTask != null) {
			return;
		}
		_deleteTask = new DeleteTimer(getId());
		_future = GeneralThreadPool.getInstance().schedule(_deleteTask, Config.NPC_DELETION_TIME * 1000);
	}

	protected static class DeleteTimer implements Runnable {
		private int _id;
		private String originalThreadName;

		protected DeleteTimer(int oId) {
			_id = oId;
			if (!(L1World.getInstance().findObject(_id) instanceof L1NpcInstance)) {
				throw new IllegalArgumentException("allowed only L1NpcInstance");
			}
		}

		@Override
		public void run() {
			try {
				originalThreadName = Thread.currentThread().getName();
				Thread.currentThread().setName("L1NpcInstance-DeleteTimer");
				L1NpcInstance npc = (L1NpcInstance) L1World.getInstance().findObject(_id);
				if (npc == null || !npc.isDead() || npc._destroyed) {
					// Leak investigation.
					if (npc == null) {
						_log.warn("DeleteTimer#run: npc was null.");
						return;
					}
					if (!npc.isDead())
						_log.warn("DeleteTimer#run: !npc.isDead().");
					if (npc._destroyed)
						_log.warn("DeleteTimer#run: npc._destroyed.");
					_log.warn(String.format("DeleteTimer#run: trouble with npc_templateid %d.", npc.getNpcId()));
					// cancel();
					return;
				}
				try {
					npc.deleteMe();
					// cancel();
				} catch (Exception e) {
					// More leak investigation.
					_log.warn(String.format("DeleteTimer#run: trouble with npc_templateid %d.", npc.getNpcId()));
					_log.error(e.getLocalizedMessage(), e);

					_log.error("", e);
				}
			} catch (Exception e) {
				_log.error("", e);
			} finally {
				Thread.currentThread().setName(originalThreadName);
			}
		}
	}

	private L1MobGroupInfo _mobGroupInfo = null;

	public boolean isInMobGroup() {
		return getMobGroupInfo() != null;
	}

	public L1MobGroupInfo getMobGroupInfo() {
		return _mobGroupInfo;
	}

	public void setMobGroupInfo(L1MobGroupInfo m) {
		_mobGroupInfo = m;
	}

	private int _mobGroupId = 0;

	public int getMobGroupId() {
		return _mobGroupId;
	}

	public void setMobGroupId(int i) {
		_mobGroupId = i;
	}

	public void startChat(int chatTiming) {
		if ((chatTiming == CHAT_TIMING_APPEARANCE && this.isDead()) || (chatTiming == CHAT_TIMING_DEAD && !this.isDead())) {
			return;
		}
		if (chatTiming == CHAT_TIMING_HIDE && this.isDead()) {
			return;
		}

		int npcId = this.getNpcTemplate().get_npcId();
		L1NpcChat npcChat = null;
		if (chatTiming == CHAT_TIMING_APPEARANCE) {
			npcChat = NpcChatTable.getInstance().getTemplateAppearance(npcId);
		} else if (chatTiming == CHAT_TIMING_DEAD) {
			npcChat = NpcChatTable.getInstance().getTemplateDead(npcId);
		} else if (chatTiming == CHAT_TIMING_HIDE) {
			npcChat = NpcChatTable.getInstance().getTemplateHide(npcId);
		} else if (chatTiming == CHAT_TIMING_GAME_TIME) {
			npcChat = NpcChatTable.getInstance().getTemplateGameTime(npcId);
		}
		if (npcChat == null) {
			return;
		}

		if (_chatTaskFuture != null) {
			if (!_chatTaskFuture.isDone()) {
				_chatTaskFuture.cancel(true);
				_chatTask = null;
			}
		}
//		// Fix timer thread leak
//		if (_chatTask != null) {
//			_chatTask.cancel();
//			_chatTask = null;
//		}
//		if (_chatTimer != null) {
//			_chatTimer.cancel();
//			_chatTimer.purge();
//			_chatTimer = null;
//		}

		// _chatTimer = new Timer("L1NpcInstance-Chat-"+getNpcId(),true);
		_chatTask = new L1NpcChatTimer(this, npcChat);
		if (!npcChat.isRepeat()) {
			// _chatTimer.schedule(_chatTask, npcChat.getStartDelayTime());
			_chatTaskFuture = GeneralThreadPool.getInstance().schedule(_chatTask, npcChat.getStartDelayTime());
		} else {
			// _chatTimer.schedule(_chatTask, npcChat.getStartDelayTime(),
			// npcChat.getRepeatInterval());
			_chatTaskFuture = GeneralThreadPool.getInstance().scheduleAtFixedRate(_chatTask,
					npcChat.getStartDelayTime(), npcChat.getRepeatInterval());
		}
	}
}
