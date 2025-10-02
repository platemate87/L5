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
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l1j.server.server.GeneralThreadPool;
import l1j.server.server.encryptions.IdFactory;
import l1j.server.server.model.L1World;
import l1j.server.server.serverpackets.S_DollPack;
import l1j.server.server.serverpackets.S_SkillSound;
import l1j.server.server.templates.L1Npc;

public class L1DollInstance extends L1NpcInstance {

	private static Logger _log = LoggerFactory.getLogger(L1DollInstance.class);

	private static final long serialVersionUID = 1L;
	public static final int DOLLTYPE_BUGBEAR = 0;
	public static final int DOLLTYPE_SUCCUBUS = 1;
	public static final int DOLLTYPE_WAREWOLF = 2;
	public static final int DOLLTYPE_ELDER = 3;
	public static final int DOLLTYPE_CRUSTANCEAN = 4;
	public static final int DOLLTYPE_GOLEM = 5;
	public static final int DOLLTYPE_YETI = 6;
	public static final int DOLLTYPE_SCARECROW = 7;
	public static final int DOLLTYPE_COCKATRICE = 8;
	public static final int DOLL_TIME = 1800000;
//	private ScheduledFuture<?> _dollFuture;
	private int _dollType;
	private int _itemObjId;

	@Override
	public boolean noTarget(int depth) {
		if (_master.isDead()) {
			deleteDoll();
			return true;
		} else if (_master != null && _master.getMapId() == getMapId()) {
			if (getLocation().getTileLineDistance(_master.getLocation()) > 2) {
				int dir = moveDirection(_master.getX(), _master.getY());
				if (dir == -1) {
					if (!isAiRunning()) {
						startAI();
					}
					return true;
				} else {
					setDirectionMove(dir);
					setSleepTime(calcSleepTime(getPassispeed(), MOVE_SPEED));
				}
			}
		} else {
			deleteDoll();
			return true;
		}
		return false;
	}

	class DollTimer implements Runnable {
		private String originalThreadName;

		@Override
		public void run() {
			try {
				originalThreadName = Thread.currentThread().getName();
				Thread.currentThread().setName("L1DollInstance-DollTimer");
				if (_destroyed) {
					return;
				}
				deleteDoll();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				_log.error("", e);
			} finally {
				Thread.currentThread().setName(originalThreadName);
			}
		}
	}

	public L1DollInstance(L1Npc template, L1PcInstance master, int dollType, int itemObjId) {
		super(template);
		setId(IdFactory.getInstance().nextId());
		setDollType(dollType);
		setItemObjId(itemObjId);
		GeneralThreadPool.getInstance().schedule(new DollTimer(), DOLL_TIME);

		setMaster(master);
		setX(master.getX() + ThreadLocalRandom.current().nextInt(5) - 2);
		setY(master.getY() + ThreadLocalRandom.current().nextInt(5) - 2);
		setMap(master.getMapId());
		setHeading(5);
		setLightSize(template.getLightSize());
		L1World.getInstance().storeObject(this);
		L1World.getInstance().addVisibleObject(this);
		for (L1PcInstance pc : L1World.getInstance().getRecognizePlayer(this)) {
			onPerceive(pc);
		}
		master.addDoll(this);
		if (!isAiRunning()) {
			startAI();
		}
		if (isMpRegeneration()) {
			master.startMpRegenerationByDoll();
		}
		master.addAc(getArmorClassByDoll());
		master.addWater(getResistWaterByDoll());
	}

	public void deleteDoll() {
		if (isMpRegeneration()) {
			((L1PcInstance) _master).stopMpRegenerationByDoll();
		}
		((L1PcInstance) _master).addAc(-getArmorClassByDoll());
		((L1PcInstance) _master).addWater(-getResistWaterByDoll());
		_master.getDollList().remove(getId());
		deleteMe();
	}

	@Override
	public void onPerceive(L1PcInstance perceivedFrom) {
		perceivedFrom.addKnownObject(this);
		perceivedFrom.sendPackets(new S_DollPack(this, perceivedFrom));
	}

	@Override
	public void onItemUse() {
		if (!isActived()) {
			useItem(USEITEM_HASTE, 100);
		}
	}

	@Override
	public void onGetItem(L1ItemInstance item, int count) {
		if (getNpcTemplate().get_digestitem() > 0) {
			setDigestItem(item);
		}
		if (Arrays.binarySearch(hastePotions, item.getItem().getItemId()) >= 0) {
			useItem(USEITEM_HASTE, 100);
		}
	}

	public int getDollType() {
		return _dollType;
	}

	public void setDollType(int i) {
		_dollType = i;
	}

	public int getItemObjId() {
		return _itemObjId;
	}

	public void setItemObjId(int i) {
		_itemObjId = i;
	}

	public boolean isMpRegeneration() {
		boolean isMpRegeneration = false;
		int dollType = getDollType();
		if (dollType == DOLLTYPE_SUCCUBUS || dollType == DOLLTYPE_ELDER) {
			isMpRegeneration = true;
		}
		return isMpRegeneration;
	}

	public int getWeightReductionByDoll() {
		int weightReduction = 0;
		int dollType = getDollType();
		if (dollType == DOLLTYPE_BUGBEAR) {
			weightReduction = 20;
		}
		return weightReduction;
	}

	public int getDamageReductionByDoll() {
		int damageReduction = 0;
		int dollType = getDollType();
		if (dollType == DOLLTYPE_GOLEM) {
			int chance = ThreadLocalRandom.current().nextInt(100) + 1;
			if (chance <= 4) {
				damageReduction = 15;
				if (_master instanceof L1PcInstance) {
					L1PcInstance pc = (L1PcInstance) _master;
					pc.sendPackets(new S_SkillSound(_master.getId(), 6320));
				}
				_master.broadcastPacket(new S_SkillSound(_master.getId(), 6320));
			}
		}
		return damageReduction;
	}

	public int getArmorClassByDoll() {
		int armorClass = 0;
		int dollType = getDollType();
		if (dollType == DOLLTYPE_YETI) {
			armorClass = -3;
		}
		return armorClass;
	}

	public int getResistWaterByDoll() {
		int resistWater = 0;
		int dollType = getDollType();
		if (dollType == DOLLTYPE_YETI) {
			resistWater = 7;
		}
		return resistWater;
	}

	public int getRangedDmgByDoll() {
		int rangedDamage = 0;
		int dollType = getDollType();
		if (dollType == DOLLTYPE_COCKATRICE) {
			rangedDamage = 1;
		}
		return rangedDamage;
	}

	public int getMeleeDmgByDoll() {
		int damage = 0;
		int dollType = getDollType();
		if (dollType == DOLLTYPE_WAREWOLF || dollType == DOLLTYPE_CRUSTANCEAN) {
			int chance = ThreadLocalRandom.current().nextInt(100) + 1;
			if (chance <= 3) {
				damage = 15;
				if (_master instanceof L1PcInstance) {
					L1PcInstance pc = (L1PcInstance) _master;
					pc.sendPackets(new S_SkillSound(_master.getId(), 6319));
				}
				_master.broadcastPacket(new S_SkillSound(_master.getId(), 6319));
			}
		}
		return damage;
	}

	public int getRangedHitByDoll() {
		int rangedHit = 0;
		int dollType = getDollType();
		if (dollType == DOLLTYPE_COCKATRICE) {
			rangedHit = 1;
		}
		if (dollType == DOLLTYPE_SCARECROW) {
			rangedHit = 2;
		}
		return rangedHit;
	}

	public int getMeleeHitByDoll() {
		int meleeHit = 0;
		int dollType = getDollType();
		if (dollType == DOLLTYPE_SCARECROW) {
			meleeHit = 2;
		}
		return meleeHit;
	}

	public int getHPBonusByDoll() {
		int hpBonus = 0;
		int dollType = getDollType();
		if (dollType == DOLLTYPE_SCARECROW) {
			hpBonus = 50;
		}
		return hpBonus;
	}

	public int getMpBonusByDoll() {
		int mpBonus = 0;
		int dollType = getDollType();
		if (dollType == DOLLTYPE_SCARECROW) {
			mpBonus = 30;
		}
		return mpBonus;
	}
}
