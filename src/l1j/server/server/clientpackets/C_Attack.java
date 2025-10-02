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
package l1j.server.server.clientpackets;

import static l1j.server.server.model.Instance.L1PcInstance.REGENSTATE_ATTACK;
import static l1j.server.server.model.skill.L1SkillId.ABSOLUTE_BARRIER;
import static l1j.server.server.model.skill.L1SkillId.MEDITATION;

import l1j.server.Config;
import l1j.server.server.ActionCodes;
import l1j.server.server.log.LogSpeedHack;
import l1j.server.server.model.AcceleratorChecker;
import l1j.server.server.model.L1Character;
import l1j.server.server.model.L1Object;
import l1j.server.server.model.L1World;
import l1j.server.server.model.Instance.L1ItemInstance;
import l1j.server.server.model.Instance.L1NpcInstance;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.model.Instance.L1PetInstance;
import l1j.server.server.model.Instance.L1SummonInstance;
import l1j.server.server.model.item.L1ItemId;
import l1j.server.server.model.item.WeaponType;
import l1j.server.server.network.Client;
import l1j.server.server.serverpackets.S_AttackPacket;
import l1j.server.server.serverpackets.S_ServerMessage;
import l1j.server.server.serverpackets.S_UseArrowSkill;

// Referenced classes of package l1j.server.server.clientpackets:
// ClientBasePacket
public class C_Attack extends ClientBasePacket {

	private int _targetX = 0;

	private int _targetY = 0;

	public C_Attack(byte[] decrypt, Client client) {
		super(decrypt);
		int targetId = readD();
		int x = readH();
		int y = readH();
		_targetX = x;
		_targetY = y;

		L1PcInstance pc = client.getActiveChar();

		if (pc.isGhost() || pc.isDead() || pc.isTeleport() || pc.isInvisble() || pc.isInvisDelay()) {
			return;
		}

		if (pc.getInventory().getWeight240() >= 197) {
			pc.sendPackets(new S_ServerMessage(110));
			return;
		}

		L1Object target = L1World.getInstance().findObject(targetId);

		if (target instanceof L1Character) {
			if (target.getMapId() != pc.getMapId() || pc.getLocation().getLineDistance(target.getLocation()) > 20D) {
				return;
			}
		}

		if (target instanceof L1NpcInstance) {
			int hiddenStatus = ((L1NpcInstance) target).getHiddenStatus();
			if (hiddenStatus == L1NpcInstance.HIDDEN_STATUS_SINK || hiddenStatus == L1NpcInstance.HIDDEN_STATUS_FLY) {
				return;
			}
		}

		if (Config.CHECK_ATTACK_INTERVAL) {
			int result;
			result = pc.getAcceleratorChecker().checkInterval(AcceleratorChecker.ACT_TYPE.ATTACK);
			if (result == AcceleratorChecker.R_LIMITEXCEEDED) {
				LogSpeedHack lsh = new LogSpeedHack();
				lsh.storeLogSpeedHack(pc);
				return;
			}
		}

		if (pc.hasSkillEffect(ABSOLUTE_BARRIER)) {
			pc.killSkillEffectTimer(ABSOLUTE_BARRIER);
			pc.startHpRegeneration();
			pc.startMpRegeneration();
			pc.startMpRegenerationByDoll();
		}
		pc.killSkillEffectTimer(MEDITATION);

		pc.delInvis();

		pc.setRegenState(REGENSTATE_ATTACK);

		if (target != null && !((L1Character) target).isDead()) {
			if (target instanceof L1PcInstance) {
				pc._pinkName.onAction((L1PcInstance) target);
			} else if (target instanceof L1PetInstance || target instanceof L1SummonInstance) {
				L1Character master = ((L1NpcInstance) target).getMaster();

				if (master != null && master instanceof L1PcInstance) {
					pc._pinkName.onAction(((L1PcInstance) master));
				}
			}

			target.onAction(pc);
		} else {
			L1ItemInstance weapon = pc.getWeapon();
			int weaponId = 0;
			int weaponType = 0;
			L1ItemInstance ammo = null;
			if (weapon != null) {
				weaponId = weapon.getItem().getItemId();
				weaponType = weapon.getItem().getType1();
				if (weaponType == WeaponType.Bow) {
					ammo = pc.getInventory().getArrow();
				} else if (weaponType == WeaponType.Gauntlet) {
					ammo = pc.getInventory().getSting();
				}
			}
			pc.setHeading(pc.targetDirection(x, y));
			if (weaponType == WeaponType.Bow && (weaponId == L1ItemId.SAYHAS_BOW || ammo != null)) {
				calcOrbit(pc.getX(), pc.getY(), pc.getHeading());
				if (ammo != null) {
					pc.sendAndBroadcast(new S_UseArrowSkill(pc, 0, 66, _targetX, _targetY, true));
					pc.getInventory().removeItem(ammo, 1);
				} else if (weaponId == L1ItemId.SAYHAS_BOW) {
					pc.sendAndBroadcast(new S_UseArrowSkill(pc, 0, 2349, _targetX, _targetY, true));
				}
			} else if (weaponType == WeaponType.Gauntlet && ammo != null) {
				calcOrbit(pc.getX(), pc.getY(), pc.getHeading());
				pc.sendAndBroadcast(new S_UseArrowSkill(pc, 0, 2989, _targetX, _targetY, true));
				pc.getInventory().removeItem(ammo, 1);
			} else {
				pc.sendAndBroadcast(new S_AttackPacket(pc, 0, ActionCodes.ACTION_Attack));
			}
		}
	}

	private void calcOrbit(int cX, int cY, int head) {
		float disX = Math.abs(cX - _targetX);
		float disY = Math.abs(cY - _targetY);
		float dis = Math.max(disX, disY);
		float avgX = 0;
		float avgY = 0;
		if (dis == 0) {
			if (head == 1) {
				avgX = 1;
				avgY = -1;
			} else if (head == 2) {
				avgX = 1;
				avgY = 0;
			} else if (head == 3) {
				avgX = 1;
				avgY = 1;
			} else if (head == 4) {
				avgX = 0;
				avgY = 1;
			} else if (head == 5) {
				avgX = -1;
				avgY = 1;
			} else if (head == 6) {
				avgX = -1;
				avgY = 0;
			} else if (head == 7) {
				avgX = -1;
				avgY = -1;
			} else if (head == 0) {
				avgX = 0;
				avgY = -1;
			}
		} else {
			avgX = disX / dis;
			avgY = disY / dis;
		}

		int addX = (int) Math.floor((avgX * 15) + 0.59f);
		int addY = (int) Math.floor((avgY * 15) + 0.59f);

		if (cX > _targetX) {
			addX *= -1;
		}
		if (cY > _targetY) {
			addY *= -1;
		}

		_targetX = _targetX + addX;
		_targetY = _targetY + addY;
	}
}
