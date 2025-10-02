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

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import l1j.server.server.ActionCodes;
import l1j.server.server.GeneralThreadPool;
import l1j.server.server.datatables.ExpTable;
import l1j.server.server.datatables.ItemTable;
import l1j.server.server.datatables.PetItemTable;
import l1j.server.server.datatables.PetTable;
import l1j.server.server.datatables.PetTypeTable;
import l1j.server.server.encryptions.IdFactory;
import l1j.server.server.model.L1Attack;
import l1j.server.server.model.L1Character;
import l1j.server.server.model.L1Inventory;
import l1j.server.server.model.L1PetFoodTimer;
import l1j.server.server.model.L1World;
import l1j.server.server.model.ZoneType;
import l1j.server.server.model.skill.L1SkillId;
import l1j.server.server.serverpackets.S_DoActionGFX;
import l1j.server.server.serverpackets.S_HPMeter;
import l1j.server.server.serverpackets.S_Light;
import l1j.server.server.serverpackets.S_NpcChatPacket;
import l1j.server.server.serverpackets.S_PetMenuPacket;
import l1j.server.server.serverpackets.S_PetPack;
import l1j.server.server.serverpackets.S_ServerMessage;
import l1j.server.server.templates.L1Item;
import l1j.server.server.templates.L1Npc;
import l1j.server.server.templates.L1Pet;
import l1j.server.server.templates.L1PetItem;
import l1j.server.server.templates.L1PetType;

public class L1PetInstance extends L1NpcInstance {

	private static final long serialVersionUID = 1L;
	private int _currentPetStatus;
	private L1PcInstance _petMaster;
	private int _itemObjId;
	private L1PetType _type;
	private int _expPercent;

	@Override
	public boolean noTarget(int depth) {
		if (_currentPetStatus == 3) { // If pet is in rest mode
			return true;
		} else if (_currentPetStatus == 4) {
			if (_petMaster != null && _petMaster.getMapId() == getMapId()
					&& getLocation().getTileLineDistance(_petMaster.getLocation()) < 5) {
				int dir = targetReverseDirection(_petMaster.getX(), _petMaster.getY());
				dir = checkObject(getX(), getY(), getMapId(), dir);
				setDirectionMove(dir);
				setSleepTime(calcSleepTime(getPassispeed(), MOVE_SPEED));
			} else {
				_currentPetStatus = 3;
				return true;
			}
		} else if (_currentPetStatus == 5) {
			if (Math.abs(getHomeX() - getX()) > 1 || Math.abs(getHomeY() - getY()) > 1) {
				int dir = moveDirection(getHomeX(), getHomeY());
				if (dir == -1) { // If the pet cant find a way to the owner
					// Original code
					/*
					 * setHomeX(getX()); setHomeY(getY());
					 */
					// Fix by Ssargon, should make summons move better without
					// getting stuck
					try {
						// TODO -- do we actually need this? Thread.sleep(200);
						// Prevent infinite recursion by max-bounding retry
						// depth
						if (depth > 80) {
							setHomeX(getX());
							setHomeY(getY());
							return true;
						} else {
							return noTarget(depth + 1);
						}
					} catch (Exception exception) {
						setHomeX(getX());
						setHomeY(getY());
						return true;
					}
				} else {
					setDirectionMove(dir);
					setSleepTime(calcSleepTime(getPassispeed(), MOVE_SPEED));
				}
			}
		} else if (_currentPetStatus == 7) {
			if (_petMaster != null && _petMaster.getMapId() == getMapId()
					&& getLocation().getTileLineDistance(_petMaster.getLocation()) <= 1) {
				_currentPetStatus = 3;
				return true;
			}
			int locx = _petMaster.getX() + ThreadLocalRandom.current().nextInt(1);
			int locy = _petMaster.getY() + ThreadLocalRandom.current().nextInt(1);
			int dir = moveDirection(locx, locy);
			if (dir == -1) {
				_currentPetStatus = 3;
				return true;
			}
			setDirectionMove(dir);
			setSleepTime(calcSleepTime(getPassispeed(), MOVE_SPEED));
		} else if (_petMaster != null && _petMaster.getMapId() == getMapId()) {
			if (getLocation().getTileLineDistance(_petMaster.getLocation()) > 2) {
				int dir = moveDirection(_petMaster.getX(), _petMaster.getY());
				if (dir == -1) {
					return false;
				}
				setDirectionMove(dir);
				setSleepTime(calcSleepTime(getPassispeed(), MOVE_SPEED));
			}
		} else {
			_currentPetStatus = 3;
			return true;
		}
		return false;
	}

	public L1PetInstance(L1Npc template, L1PcInstance master, L1Pet l1pet) {
		super(template);
		_petMaster = master;
		_itemObjId = l1pet.get_itemobjid();
		_type = PetTypeTable.getInstance().get(template.get_npcId());
		setId(l1pet.get_objid());
		setName(l1pet.get_name());
		setLevel(l1pet.get_level());
		setMaxHp(l1pet.get_hp());
		setCurrentHpDirect(l1pet.get_hp());
		setMaxMp(l1pet.get_mp());
		setCurrentMpDirect(l1pet.get_mp());
		setExp(l1pet.get_exp());
		setExpPercent(ExpTable.getExpPercentage(l1pet.get_level(), l1pet.get_exp()));
		setLawful(l1pet.get_lawful());
		setTempLawful(l1pet.get_lawful());

		setFood(l1pet.getFood());
		startFoodTimer(this);

		ItemTable items = ItemTable.getInstance();
		setWeapon(items.createItem(l1pet.get_weapon()));
		setArmor(items.createItem(l1pet.get_armor()));
		setMaster(master);

		// make sure when we pull them out that they're not stuck somewhere they can't
		// get to you
		// limit to 15 tries
		for (int x = 0; x < 15 && moveDirection(master.getX(), master.getY()) == -1; x++) {
			setX(master.getX() + ThreadLocalRandom.current().nextInt(5) - 2);
			setY(master.getY() + ThreadLocalRandom.current().nextInt(5) - 2);
		}

		setMap(master.getMapId());
		setHeading(5);
		setLightSize(template.getLightSize());
		_currentPetStatus = 3;
		L1World.getInstance().storeObject(this);
		L1World.getInstance().addVisibleObject(this);
		for (L1PcInstance pc : L1World.getInstance().getRecognizePlayer(this)) {
			onPerceive(pc);
		}
		master.addPet(this);
	}

	public L1PetInstance(L1NpcInstance target, L1PcInstance master, int itemid) {
		super(null);
		_petMaster = master;
		_itemObjId = itemid;
		_type = PetTypeTable.getInstance().get(target.getNpcTemplate().get_npcId());

		setId(IdFactory.getInstance().nextId());
		setting_template(target.getNpcTemplate());
		setCurrentHpDirect(target.getCurrentHp());
		setCurrentMpDirect(target.getCurrentMp());
		setExp(750); // Lvl 5 EXP
		setExpPercent(0);
		setLawful(0);
		setTempLawful(0);

		setFood(target.getFood());
		startFoodTimer(this);

		setMaster(master);
		setX(target.getX());
		setY(target.getY());
		setMap(target.getMapId());
		setHeading(target.getHeading());
		setLightSize(target.getLightSize());
		setPetcost(6);
		setInventory(target.getInventory());
		target.setInventory(null);
		_currentPetStatus = 3;
		target.deleteMe();
		L1World.getInstance().storeObject(this);
		L1World.getInstance().addVisibleObject(this);
		for (L1PcInstance pc : L1World.getInstance().getRecognizePlayer(this)) {
			onPerceive(pc);
		}
		master.addPet(this);
		PetTable.getInstance().storeNewPet(target, getId(), itemid);
	}

	@Override
	public void receiveDamage(L1Character attacker, int damage) {
		if (attacker instanceof L1PcInstance) {
			((L1PcInstance) attacker).setLastAggressiveAct();
		}

		if (getCurrentHp() > 0) {
			boolean isMaster = attacker == _master;

			if (damage > 0 && !isMaster) {
				setHate(attacker, 0);
				removeSkillEffect(L1SkillId.FOG_OF_SLEEPING);
			}
			if (attacker instanceof L1PcInstance && damage > 0) {
				L1PcInstance player = (L1PcInstance) attacker;
				player.setPetTarget(this);
			}
			int newHp = getCurrentHp() - damage;
			if (newHp <= 0) {
				death(attacker);
			} else {
				setCurrentHp(newHp);
			}
		} else if (!isDead()) {
			death(attacker);
		}
	}

	public synchronized void death(L1Character lastAttacker) {
		if (!isDead()) {
			setStatus(ActionCodes.ACTION_Die);
			setCurrentHp(0);
			// Temporarily disabling xp loss for pets until
			// death is implemented properly
			// deathPenalty();
			setDead(true);
			getMap().setPassable(getLocation(), true);
			broadcastPacket(new S_DoActionGFX(getId(), ActionCodes.ACTION_Die));
		}
	}

	public void deathPenalty() {
		int oldLevel = getLevel();
		int needExp = ExpTable.getNeedExpNextLevel(oldLevel);
		int exp = 0;
		if (oldLevel >= 1 && oldLevel < 11) {
			exp = 0;
		} else if (oldLevel >= 11 && oldLevel < 45) {
			exp = (int) (needExp * 0.05);
		} else if (oldLevel == 45) {
			exp = (int) (needExp * 0.045);
		} else if (oldLevel == 46) {
			exp = (int) (needExp * 0.04);
		} else if (oldLevel == 47) {
			exp = (int) (needExp * 0.035);
		} else if (oldLevel == 48) {
			exp = (int) (needExp * 0.03);
			// Modified to scale down the XP death loss % at higher lvls.
		} else if (oldLevel >= 49 && oldLevel < 65) {
			exp = (int) (needExp * 0.025);
		} else if (oldLevel >= 65 && oldLevel < 70) {
			exp = (int) (needExp * 0.0125);
		} else if (oldLevel >= 65 && oldLevel < 75) {
			exp = (int) (needExp * 0.00625);
		} else if (oldLevel >= 75 && oldLevel < 79) {
			exp = (int) (needExp * 0.003125);
		} else if (oldLevel >= 79 && oldLevel < 80) {
			exp = (int) (needExp * 0.0015625);
		} else if (oldLevel >= 80) {
			exp = (int) (needExp * 0.00078125);
		}

		if (exp == 0) {
			return;
		}

		L1PcInstance pc = (L1PcInstance) getMaster();
		setExp(getExp() - exp);
		setLevel(ExpTable.getLevelByExp(getExp()));
		setExpPercent(ExpTable.getExpPercentage(getLevel(), getExp()));
		pc.sendPackets(new S_PetPack(this, pc));
	}

	public void evolvePet(int new_itemobjid) {
		L1Pet l1pet = PetTable.getInstance().getTemplate(_itemObjId);
		if (l1pet == null) {
			return;
		}
		int newNpcId = _type.getNpcIdForEvolving();
		int tmpMaxHp = getMaxHp();
		int tmpMaxMp = getMaxMp();
		transform(newNpcId);
		_type = PetTypeTable.getInstance().get(newNpcId);
		setLevel(1);
		setMaxHp(tmpMaxHp / 2);
		setMaxMp(tmpMaxMp / 2);
		setCurrentHpDirect(getMaxHp());
		setCurrentMpDirect(getMaxMp());
		setExp(0);
		setExpPercent(0);
		collect(true);
		getInventory().clearItems();
		PetTable.getInstance().deletePet(_itemObjId);
		l1pet.set_itemobjid(new_itemobjid);
		l1pet.set_npcid(newNpcId);
		l1pet.set_name(getName());
		l1pet.set_level(getLevel());
		l1pet.set_hp(getMaxHp());
		l1pet.set_mp(getMaxMp());
		l1pet.set_exp(getExp());
		l1pet.setFood(getFood());
		L1ItemInstance armor = getArmor();
		L1ItemInstance weapon = getWeapon();
		if (weapon != null)
			l1pet.set_weapon(weapon.getItemId());
		if (armor != null)
			l1pet.set_armor(armor.getItemId());
		PetTable.getInstance().storeNewPet(this, getId(), new_itemobjid);
		_itemObjId = new_itemobjid;
	}

	public void liberate() {
		L1MonsterInstance monster = new L1MonsterInstance(getNpcTemplate());
		monster.setId(IdFactory.getInstance().nextId());
		monster.setX(getX());
		monster.setY(getY());
		monster.setMap(getMapId());
		monster.setHeading(getHeading());
		monster.set_storeDroped(true);
		monster.setInventory(getInventory());
		setInventory(null);
		monster.setLevel(getLevel());
		monster.setMaxHp(getMaxHp());
		monster.setCurrentHpDirect(getCurrentHp());
		monster.setMaxMp(getMaxMp());
		monster.setCurrentMpDirect(getCurrentMp());
		_petMaster.getPetList().remove(getId());
		deleteMe();
		_petMaster.getInventory().removeItem(_itemObjId, 1);
		PetTable.getInstance().deletePet(_itemObjId);
		L1World.getInstance().storeObject(monster);
		L1World.getInstance().addVisibleObject(monster);
		for (L1PcInstance pc : L1World.getInstance().getRecognizePlayer(monster)) {
			onPerceive(pc);
		}
	}

	public void collect() {
		collect(false);
	}

	public void collect(boolean unequip) {
		L1Inventory targetInventory = _petMaster.getInventory();
		List<L1ItemInstance> items = _inventory.getItems();
		int last = _inventory.getSize() - 1;
		for (int i = last; i >= 0; i--) {
			L1ItemInstance item = items.get(i);

			if (!unequip && item.isEquipped()) {
				continue;
			} else if (unequip && item.isEquipped()) {
				// Not the best way to check, but the same way it's done
				// in C_UsePetItem.java
				int itemId = item.getItemId();
				if (itemId >= 40749 && itemId <= 40752 || itemId >= 40756 && itemId <= 40758) {
					removeWeapon(item);
				} else if (itemId >= 40761 && itemId <= 40766) {
					removeArmor(item);
				}
			}

			if (_petMaster.getInventory().checkAddItem( //
					item, item.getCount()) == L1Inventory.OK) {
				_inventory.tradeItem(item, item.getCount(), targetInventory);
				_petMaster.sendPackets(new S_ServerMessage(143, getName(), item.getLogName()));
			} else {
				targetInventory = L1World.getInstance().getInventory(getX(), getY(), getMapId());
				_inventory.tradeItem(item, item.getCount(), targetInventory);
			}
		}
	}

	public void dropItem() {
		dropItem(false);
	}

	public void dropItem(boolean collectEquipment) {
		L1Inventory targetInventory = L1World.getInstance().getInventory(getX(), getY(), getMapId());

		L1Inventory masterInventory = _petMaster.getInventory();

		List<L1ItemInstance> items = _inventory.getItems();
		int size = _inventory.getSize();
		for (int i = 0; i < size; i++) {
			L1ItemInstance item = items.get(0);

			if (collectEquipment && item.isEquipped() && _petMaster.getInventory().checkAddItem( //
					item, item.getCount()) == L1Inventory.OK) {

				item.setEquipped(false);
				_inventory.tradeItem(item, item.getCount(), masterInventory);
			} else {
				item.setEquipped(false);
				_inventory.tradeItem(item, item.getCount(), targetInventory);
			}
		}
	}

	public void call() {
		int id = _type.getMessageId(L1PetType.getMessageNumber(getLevel()));
		if (id != 0) {
			broadcastPacket(new S_NpcChatPacket(this, "$" + id, 0));
		}
		setCurrentPetStatus(7);
	}

	public void setTarget(L1Character target) {
		if (target != null && (_currentPetStatus == 1 || _currentPetStatus == 2 || _currentPetStatus == 5)) {
			setHate(target, 0);
			if (!isAiRunning()) {
				startAI();
			}
		}
	}

	public void setMasterTarget(L1Character target) {
		if (target != null && (_currentPetStatus == 1 || _currentPetStatus == 5)) {
			setHate(target, 0);
			if (!isAiRunning()) {
				startAI();
			}
		}
	}

	@Override
	public void onPerceive(L1PcInstance perceivedFrom) {
		perceivedFrom.sendPackets(new S_Light(this.getId(), getLightSize()));
		perceivedFrom.addKnownObject(this);
		perceivedFrom.sendPackets(new S_PetPack(this, perceivedFrom));
		if (isDead()) {
			perceivedFrom.sendPackets(new S_DoActionGFX(getId(), ActionCodes.ACTION_Die));
		}
	}

	@Override
	public void onAction(L1PcInstance player) {
		L1Character cha = this.getMaster();
		L1PcInstance master = (L1PcInstance) cha;
		if (master.isTeleport()) {
			return;
		}
		if (getZoneType() == ZoneType.Safety) {
			L1Attack attack_mortion = new L1Attack(player, this);
			attack_mortion.action();
			return;
		}
		if (player == null || player.checkNonPvP(player, this)) {
			return;
		}
		L1Attack attack = new L1Attack(player, this);
		if (attack.calcHit()) {
			attack.calcDamage();
			attack.calcStaffOfMana();
			attack.addPcPoisonAttack(player, this);
		}
		attack.action();
		attack.commit();
	}

	@Override
	public void onTalkAction(L1PcInstance player) {
		if (isDead()) {
			return;
		}
		if (_petMaster.equals(player)) {
			player.sendPackets(new S_PetMenuPacket(this, getExpPercent()));
			L1Pet l1pet = PetTable.getInstance().getTemplate(_itemObjId);
			if (l1pet != null) {
				l1pet.set_exp(getExp());
				l1pet.set_level(getLevel());
				l1pet.set_hp(getMaxHp());
				l1pet.set_mp(getMaxMp());
				PetTable.getInstance().storePet(l1pet);
			}
		}
	}

	/*
	 * Save to DataBase
	 */
	public void save() {
		PetTable.getInstance().storePet(PetTable.getInstance().getTemplate(_itemObjId));
	}

	@Override
	public void onFinalAction(L1PcInstance player, String action) {
		int status = actionType(action);
		if (status == 0) {
			return;
		}
		if (status == 6) {
			liberate();
		} else {
			Object[] petList = _petMaster.getPetList().values().toArray();
			for (Object petObject : petList) {
				if (petObject instanceof L1PetInstance) {
					L1PetInstance pet = (L1PetInstance) petObject;
					if (_petMaster != null && _petMaster.getLevel() >= pet.getLevel()) {
						pet.setCurrentPetStatus(status);
					} else {
						L1PetType type = PetTypeTable.getInstance().get(pet.getNpcTemplate().get_npcId());
						int id = type.getDefyMessageId();
						if (id != 0) {
							broadcastPacket(new S_NpcChatPacket(pet, "$" + id, 0));
						}
					}
				}
			}
		}
	}

	@Override
	public void onItemUse() {
		if (!isActived()) {
			useItem(USEITEM_HASTE, 100);
		}
		if (getCurrentHp() * 100 / getMaxHp() < 40) {
			useItem(USEITEM_HEAL, 100);
		}
	}

	@Override
	public void onGetItem(L1ItemInstance item, int count) {
		if (getNpcTemplate().get_digestitem() > 0) {
			setDigestItem(item);
		}

		if (isFood(item.getItem())) {
			eatFood(item, count);
		}

		Arrays.sort(healPotions);
		Arrays.sort(hastePotions);
		if (Arrays.binarySearch(healPotions, item.getItem().getItemId()) >= 0) {
			if (getCurrentHp() != getMaxHp()) {
				useItem(USEITEM_HEAL, 100);
			}
		} else if (Arrays.binarySearch(hastePotions, item.getItem().getItemId()) >= 0) {
			useItem(USEITEM_HASTE, 100);
		}
	}

	private int actionType(String action) {
		int status = 0;
		if (action.equalsIgnoreCase("aggressive")) {
			status = 1;
		} else if (action.equalsIgnoreCase("defensive")) {
			status = 2;
		} else if (action.equalsIgnoreCase("stay")) {
			status = 3;
		} else if (action.equalsIgnoreCase("extend")) {
			status = 4;
		} else if (action.equalsIgnoreCase("alert")) {
			status = 5;
		} else if (action.equalsIgnoreCase("dismiss")) {
			status = 6;
		} else if (action.equalsIgnoreCase("getitem")) {
			collect();
		}
		return status;
	}

	@Override
	public void setCurrentHp(int i) {
		int currentHp = i;
		if (currentHp >= getMaxHp()) {
			currentHp = getMaxHp();
		}
		setCurrentHpDirect(currentHp);

		if (getMaxHp() > getCurrentHp()) {
			startHpRegeneration();
		}
		if (_petMaster != null) {
			int HpRatio = 100 * currentHp / getMaxHp();
			L1PcInstance Master = _petMaster;
			Master.sendPackets(new S_HPMeter(getId(), HpRatio));
		}
	}

	@Override
	public void setCurrentMp(int i) {
		int currentMp = i;
		if (currentMp >= getMaxMp()) {
			currentMp = getMaxMp();
		}
		setCurrentMpDirect(currentMp);

		if (getMaxMp() > getCurrentMp()) {
			startMpRegeneration();
		}
	}

	public void setCurrentPetStatus(int i) {
		_currentPetStatus = i;
		if (_currentPetStatus == 5) {
			setHomeX(getX());
			setHomeY(getY());
		}
		if (_currentPetStatus == 7) {
			allTargetClear();
		}
		if (_currentPetStatus == 3) {
			allTargetClear();
		} else {
			if (!isAiRunning()) {
				startAI();
			}
		}
	}

	public int getCurrentPetStatus() {
		return _currentPetStatus;
	}

	public int getItemObjId() {
		return _itemObjId;
	}

	public void setExpPercent(int expPercent) {
		_expPercent = expPercent;
	}

	public int getExpPercent() {
		return _expPercent;
	}

	private L1ItemInstance _weapon;

	public void setWeapon(L1ItemInstance weapon) {
	    _weapon = weapon;

	    if (weapon == null)
	        return;

	    // Atualiza o pet e salva usando método específico
	    PetTable.getInstance().updatePetWeapon(_itemObjId, weapon.getItemId());

	    // Aplica os bônus da arma
	    L1PetItem template = PetItemTable.getInstance().getTemplate(weapon.getItem().getItemId());
	    if (template == null) {
	        return;
	    }
	    setHitByWeapon(template.getHitModifier());
	    setDamageByWeapon(template.getDamageModifier());
	    addStr(template.getAddStr());
	    addCon(template.getAddCon());
	    addDex(template.getAddDex());
	    addInt(template.getAddInt());
	    addWis(template.getAddWis());
	    addMaxHp(template.getAddHp());
	    addMaxMp(template.getAddMp());
	    addSp(template.getAddSp());
	    addMr(template.getAddMr());
	    weapon.setEquipped(true);
	}

	public L1ItemInstance getWeapon() {
		return _weapon;
	}

	private L1ItemInstance _armor;

	public void setArmor(L1ItemInstance armor) {
	    _armor = armor;

	    if (armor == null)
	        return;

	    PetTable.getInstance().updatePetArmor(_itemObjId, armor.getItem().getItemId());

	    L1PetItem template = PetItemTable.getInstance().getTemplate(armor.getItem().getItemId());
	    if (template == null) {
	        return;
	    }
	    addAc(template.getAddAc());
	    addStr(template.getAddStr());
	    addCon(template.getAddCon());
	    addDex(template.getAddDex());
	    addInt(template.getAddInt());
	    addWis(template.getAddWis());
	    addMaxHp(template.getAddHp());
	    addMaxMp(template.getAddMp());
	    addSp(template.getAddSp());
	    addMr(template.getAddMr());
	    armor.setEquipped(true);
	}


	public void removeArmor(final L1ItemInstance armor) {
		int itemId = armor.getItem().getItemId();
		L1PetItem template = PetItemTable.getInstance().getTemplate(itemId);
		if (template == null) {
			return;
		}
		addAc(-template.getAddAc());
		addStr(-template.getAddStr());
		addCon(-template.getAddCon());
		addDex(-template.getAddDex());
		addInt(-template.getAddInt());
		addWis(-template.getAddWis());
		addMaxHp(-template.getAddHp());
		addMaxMp(-template.getAddMp());
		addSp(-template.getAddSp());
		addMr(-template.getAddMr());
		setArmor(null);
		armor.setEquipped(false);
	}

	public void removeWeapon(final L1ItemInstance weapon) {
		int itemId = weapon.getItem().getItemId();
		L1PetItem template = PetItemTable.getInstance().getTemplate(itemId);
		if (template == null) {
			return;
		}
		setHitByWeapon(0);
		setDamageByWeapon(0);
		addStr(-template.getAddStr());
		addCon(-template.getAddCon());
		addDex(-template.getAddDex());
		addInt(-template.getAddInt());
		addWis(-template.getAddWis());
		addMaxHp(-template.getAddHp());
		addMaxMp(-template.getAddMp());
		addSp(-template.getAddSp());
		addMr(-template.getAddMr());
		setWeapon(null);
		weapon.setEquipped(false);
	}

	public L1ItemInstance getArmor() {
		return _armor;
	}

	private int _hitByWeapon;

	public void setHitByWeapon(int i) {
		_hitByWeapon = i;
	}

	public int getHitByWeapon() {
		return _hitByWeapon;
	}

	private int _damageByWeapon;

	public void setDamageByWeapon(int i) {
		_damageByWeapon = i;
	}

	public int getDamageByWeapon() {
		return _damageByWeapon;
	}

	public L1PetType getPetType() {
		return _type;
	}

	private L1PetFoodTimer _petFoodTimer;

	public void startFoodTimer(L1PetInstance pet) {
		_petFoodTimer = new L1PetFoodTimer(pet);
		GeneralThreadPool.getInstance().schedule(_petFoodTimer, 10000);
	}

	private void eatFood(L1ItemInstance item, int count) {
		if (item.getItem().getFoodVolume() == 0) {
			return;
		}

		int newFood = getFood();
		int eatenCount = 0;
		for (int i = 0; i < count; i++) {
			if (newFood >= 100) {
				break;
			}
			newFood += item.getItem().getFoodVolume() / 10;
			eatenCount++;
		}
		if (eatenCount == 0) {
			return;
		}

		getInventory().removeItem(item, eatenCount);
		setFood(Math.min(100, newFood));
		L1Pet l1pet = PetTable.getInstance().getTemplate(_itemObjId);
		l1pet.setFood(getFood()); // TODO -- seems odd I need to update it in both places...
		PetTable.getInstance().storePetFood(_itemObjId, getFood());
	}

	private boolean isFood(L1Item item) {
		int petId = _type.getBaseNpcId();
		int itemId = item.getItemId();
		boolean result = false;
		if (petId == 45313 || petId == 45710) { // Tiger, Battle Tiger
			if (itemId == 50572 || itemId == 50574) {
				result = true;
			}
		} else if (petId == 45711 || petId == 45712) { // Jindo puppy
			if (itemId == 50573 || itemId == 50575) {
				result = true;
			}
		} else { // All other pets
			if (item.getType2() == 0 && item.getType() == 7) {
				result = true;
			}
		}

		return result;
	}
}
