/* This program is free software; you can redistribute it and/or modify
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

import static l1j.server.server.model.Instance.L1PcInstance.REGENSTATE_MOVE;
import static l1j.server.server.model.skill.L1SkillId.ABSOLUTE_BARRIER;
import static l1j.server.server.model.skill.L1SkillId.MEDITATION;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l1j.server.Config;
import l1j.server.server.command.executor.L1Follow;
import l1j.server.server.log.LogSpeedHack;
import l1j.server.server.model.AcceleratorChecker;
import l1j.server.server.model.Dungeon;
import l1j.server.server.model.DungeonRandom;
import l1j.server.server.model.L1Location;
import l1j.server.server.model.L1Teleport;
import l1j.server.server.model.ZoneType;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.model.trap.L1WorldTraps;
import l1j.server.server.network.Client;
import l1j.server.server.serverpackets.S_MoveCharPacket;

// Referenced classes of package l1j.server.server.clientpackets:
// ClientBasePacket
public class C_MoveChar extends ClientBasePacket {

	private static Logger _log = LoggerFactory.getLogger(C_MoveChar.class.getName());
	private static final byte HEADING_TABLE_X[] = { 0, 1, 1, 1, 0, -1, -1, -1 };
	private static final byte HEADING_TABLE_Y[] = { -1, -1, 0, 1, 1, 1, 0, -1 };

	private static final int CLIENT_LANGUAGE = Config.CLIENT_LANGUAGE;

	public C_MoveChar(byte decrypt[], Client client) throws Exception {
		super(decrypt);
		int locx = readH();
		int locy = readH();
		int heading = readC();

		L1PcInstance pc = client.getActiveChar();

		try {
			if (pc != null && pc.getZoneType() == ZoneType.Safety) {
				pc.setLastAggressiveAct(0);
			}
		} catch (Exception ex) {
			// TODO -- remove this in the future when I find out what is causing this
			// exception to be thrown
			// but for now, I wan't to make sure it doesn't crap out the client thread --
			// Smitty
			_log.warn("Check of zone type and resetting aggressive act failed.", ex);
		}

		if (pc.isTeleport()) {
			return;
		}

		if (Config.CHECK_MOVE_INTERVAL) {
			int result;
			result = pc.getAcceleratorChecker().checkInterval(AcceleratorChecker.ACT_TYPE.MOVE);
			if (result == AcceleratorChecker.R_LIMITEXCEEDED) {
				LogSpeedHack lsh = new LogSpeedHack();
				lsh.storeLogSpeedHack(pc);
				L1Teleport.teleport(pc, pc.getX(), pc.getY(), pc.getMapId(), 5, false);
				return;
			}
		}

		pc.killSkillEffectTimer(MEDITATION);
		pc.setCallClanId(0);

		if (!pc.hasSkillEffect(ABSOLUTE_BARRIER)) {
			pc.setRegenState(REGENSTATE_MOVE);
		}
		pc.getMap().setPassable(pc.getLocation(), true);

		// do the call after their current cell has been set passable,
		// otherwise it will always collide if they're going in the same direction
		if (!pc.getMap().isPassable(locx, locy)) {
			_log.warn(String.format("Player %s (%d) attempted to walk somewhere they're not allowed! Loc: %d, %d, %d",
					pc.getName(), pc.getId(), locx, locy, pc.getMapId()));
			int adjustHeading = pc.getHeading() >= 4 ? pc.getHeading() - 4 : pc.getHeading() + 4;

			L1Teleport.teleport(pc, locx += HEADING_TABLE_X[adjustHeading], locy += HEADING_TABLE_Y[adjustHeading],
					pc.getMapId(), pc.getHeading(), false);
			return;
		}

		if (CLIENT_LANGUAGE == 3) { // Taiwan Only
			heading ^= 0x49;
			locx = pc.getX();
			locy = pc.getY();
		}

		locx += HEADING_TABLE_X[heading];
		locy += HEADING_TABLE_Y[heading];

		if (Dungeon.getInstance().dg(locx, locy, pc.getMap().getId(), pc) || DungeonRandom.getInstance().dg(locx, locy, pc.getMap().getId(), pc)) {
			return;
		}

		// Esc bug fix. Don't remove.
		L1Location oldLoc = pc.getLocation();
		if ((oldLoc.getX() + 10 < locx) || (oldLoc.getX() - 10 > locx) || (oldLoc.getY() + 10 < locy)
				|| (oldLoc.getX() - 10 > locx)) {
			return;
		}
		pc.getLocation().set(locx, locy);
		pc.setHeading(heading);

		if (pc.isGmInvis() || pc.isGhost()) {
		} else if (pc.isInvisble()) {
			pc.broadcastPacketForFindInvis(new S_MoveCharPacket(pc), true);
		} else {
			pc.broadcastPacket(new S_MoveCharPacket(pc));
		}

		// wrapped in a try/catch just to make sure the GM command can't
		// crap out the client packet
		try {
			L1PcInstance followingGm = pc.getFollowingGm();
			if (followingGm != null)
				L1Follow.moveChar(pc, followingGm);
		} catch (Exception ex) {
			_log.warn("L1Follow: " + ex.getMessage());
		}

		// sendMapTileLog(pc);
		l1j.server.server.model.L1PolyRace.getInstance().checkLapFinish(pc);
		L1WorldTraps.getInstance().onPlayerMoved(pc);

		L1WorldTraps.getInstance().onPlayerMoved(pc);
		pc.getMap().setPassable(pc.getLocation(), false);
		// user.UpdateObject();
	}
}