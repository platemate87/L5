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
package l1j.server.server.controllers;

import java.util.ArrayList;

import l1j.server.Config;
import l1j.server.server.model.L1Character;
import l1j.server.server.model.L1Object;
import l1j.server.server.model.L1World;
import l1j.server.server.model.Instance.L1MonsterInstance;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.serverpackets.S_SystemMessage;

public class TargetLockController {

	private static final TargetLockController INSTANCE = new TargetLockController();

	private TargetLockController() {
	}

	public static TargetLockController getInstance() {
		return INSTANCE;
	}

	public void handleLockRequest(L1PcInstance pc, int requestedTargetId) {
		if (pc == null || !Config.ENABLE_TARGET_LOCK_ASSIST) {
			return;
		}

		boolean allowNearest = requestedTargetId <= 0 && !pc.hasTargetLock();
		L1Character target = resolveTarget(pc, requestedTargetId, allowNearest);
		if (target != null) {
			pc.setTargetLock(target);
			pc.showTargetLockSelectionIndicator(target);
			pc.sendPackets(new S_SystemMessage(String.format("Target locked: %s", target.getName())));
			return;
		}

		pc.clearTargetLock();
		pc.sendPackets(new S_SystemMessage("Target lock cleared."));
	}

	public void handleAttackRequest(L1PcInstance pc, int requestedTargetId, int clickX, int clickY) {
		if (pc == null || !Config.ENABLE_TARGET_LOCK_ASSIST) {
			return;
		}

		L1Character target = pc.getTargetLockTarget();
		if (target == null || (requestedTargetId > 0 && target.getId() != requestedTargetId)) {
			boolean allowNearest = requestedTargetId <= 0;
			target = resolveTarget(pc, requestedTargetId, allowNearest);
		}
		if (target == null) {
			pc.sendPackets(new S_SystemMessage("There is no valid monster to attack."));
			return;
		}

		pc.setTargetLock(target);
		pc.showTargetLockSelectionIndicator(target);
		pc.startTargetLockAssist();
	}

	private L1Character resolveTarget(L1PcInstance pc, int requestedTargetId, boolean allowNearest) {
		L1Character target = null;
		if (requestedTargetId > 0) {
			L1Object obj = L1World.getInstance().findObject(requestedTargetId);
			if (obj instanceof L1MonsterInstance) {
				L1MonsterInstance monster = (L1MonsterInstance) obj;
				if (!monster.isDead() && monster.getMapId() == pc.getMapId()) {
					target = monster;
				}
			}
		}

		if (target == null && allowNearest) {
			target = findNearestMonster(pc);
		}

		return target;
	}

	private L1MonsterInstance findNearestMonster(L1PcInstance pc) {
		ArrayList<L1Object> objects = L1World.getInstance().getVisibleObjects(pc);
		L1MonsterInstance closest = null;
		double bestDistance = Double.MAX_VALUE;
		for (L1Object obj : objects) {
			if (!(obj instanceof L1MonsterInstance)) {
				continue;
			}
			L1MonsterInstance monster = (L1MonsterInstance) obj;
			if (monster.isDead() || monster.getMapId() != pc.getMapId()) {
				continue;
			}
			double distance = pc.getLocation().getLineDistance(monster.getLocation());
			if (distance < bestDistance) {
				bestDistance = distance;
				closest = monster;
			}
		}
		return closest;
	}
}
