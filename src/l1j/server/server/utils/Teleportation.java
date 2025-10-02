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
package l1j.server.server.utils;

import static l1j.server.server.model.skill.L1SkillId.BLOODLUST;
import static l1j.server.server.model.skill.L1SkillId.WIND_SHACKLE;

import java.util.HashSet;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l1j.server.server.command.executor.L1Follow;
import l1j.server.server.model.L1Clan;
import l1j.server.server.model.L1Location;
import l1j.server.server.model.L1World;
import l1j.server.server.model.ZoneType;
import l1j.server.server.model.Instance.L1DollInstance;
import l1j.server.server.model.Instance.L1NpcInstance;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.model.Instance.L1PetInstance;
import l1j.server.server.model.Instance.L1SummonInstance;
import l1j.server.server.model.map.L1Map;
import l1j.server.server.model.map.L1WorldMap;
import l1j.server.server.model.skill.L1SkillId;
import l1j.server.server.serverpackets.S_CharVisualUpdate;
import l1j.server.server.serverpackets.S_DollPack;
import l1j.server.server.serverpackets.S_MapID;
import l1j.server.server.serverpackets.S_OtherCharPacks;
import l1j.server.server.serverpackets.S_OwnCharPack;
import l1j.server.server.serverpackets.S_PetPack;
import l1j.server.server.serverpackets.S_SkillBrave;
import l1j.server.server.serverpackets.S_SkillIconWindShackle;
import l1j.server.server.serverpackets.S_SummonPack;

// Referenced classes of package l1j.server.server.utils:
// FaceToFace
public class Teleportation {
	private static Logger _log = LoggerFactory.getLogger(Teleportation.class.getName());

	private Teleportation() {
	}

	public static void teleport(L1PcInstance pc) {
		teleport(pc, false);
	}

	public static void teleport(L1PcInstance pc, boolean ignorePets) {
		if (pc.isDead() || pc.isTeleport()) {
			return;
		}

		int x = pc.getTeleportX();
		int y = pc.getTeleportY();
		short mapId = pc.getTeleportMapId();
		int head = pc.getTeleportHeading();

		L1Map map = L1WorldMap.getInstance().getMap(mapId);

		if (!map.isInMap(x, y) && !pc.isGm()) {
			x = pc.getX();
			y = pc.getY();
			mapId = pc.getMapId();
		}

		pc.setTeleport(true);

		// TODO -- remove this soon.. just being used to test if someone is actually
		// able to teleport where they choose
		_log.info(String.format("%s just teleported from mapid %d, x: %d, y: %d, to mapid: %d, x: %d, y: %d",
				pc.getName(), pc.getMapId(), pc.getX(), pc.getY(), mapId, x, y));

		L1Clan clan = L1World.getInstance().getClan(pc.getClanname());
		if (clan != null) {
			if (clan.getWarehouseUsingChar() == pc.getId()) {
				clan.setWarehouseUsingChar(0);
			}
		}

		L1World.getInstance().moveVisibleObject(pc, mapId);
		pc.setLocation(x, y, mapId);
		pc.setHeading(head);
		pc.sendPackets(new S_MapID(pc.getMapId(), pc.getMap().isUnderwater()));

		if (pc.isReserveGhost()) {
			pc.endGhost();
		}
		if (pc.isGhost() || pc.isGmInvis()) {
		} else if (pc.isInvisble()) {
			pc.broadcastPacketForFindInvis(new S_OtherCharPacks(pc, true), true);
		} else {
			pc.broadcastPacket(new S_OtherCharPacks(pc));
		}
		pc.sendPackets(new S_OwnCharPack(pc));
		pc.removeAllKnownObjects();
		pc.sendVisualEffectAtTeleport();
		pc.updateObject();
		pc.sendPackets(new S_CharVisualUpdate(pc));
		pc.killSkillEffectTimer(L1SkillId.MEDITATION);
		pc.setCallClanId(0);
		HashSet<L1PcInstance> subjects = new HashSet<>();
		subjects.add(pc);

		if (!ignorePets) {
			if (!pc.isGhost()) {
				if (pc.getMap().isTakePets()) {
					for (L1NpcInstance petNpc : pc.getPetList().values()) {
						L1Location loc = pc.getLocation().randomLocation(3, false);
						int nx = loc.getX();
						int ny = loc.getY();
						if (!pc.glanceCheck(nx, ny)) {
							nx = pc.getLocation().getX();
							ny = pc.getLocation().getY();
						}
						if (pc.getMapId() == 5125 || pc.getMapId() == 5131 || pc.getMapId() == 5132
								|| pc.getMapId() == 5133 || pc.getMapId() == 5134) {
							nx = 32799 + ThreadLocalRandom.current().nextInt(5) - 3;
							ny = 32864 + ThreadLocalRandom.current().nextInt(5) - 3;
						}
						teleport(petNpc, nx, ny, mapId, head);
						if (petNpc instanceof L1SummonInstance) {
							L1SummonInstance summon = (L1SummonInstance) petNpc;
							pc.sendPackets(new S_SummonPack(summon, pc));
						} else if (petNpc instanceof L1PetInstance) {
							L1PetInstance pet = (L1PetInstance) petNpc;
							pc.sendPackets(new S_PetPack(pet, pc));
						}

						for (L1PcInstance visiblePc : L1World.getInstance().getVisiblePlayer(petNpc)) {
							visiblePc.removeKnownObject(petNpc);
							subjects.add(visiblePc);
						}
					}
					for (L1DollInstance doll : pc.getDollList().values()) {
						L1Location loc = pc.getLocation().randomLocation(3, false);
						int nx = loc.getX();
						int ny = loc.getY();
						teleport(doll, nx, ny, mapId, head);
						pc.sendPackets(new S_DollPack(doll, pc));

						for (L1PcInstance visiblePc : L1World.getInstance().getVisiblePlayer(doll)) {
							visiblePc.removeKnownObject(doll);
							subjects.add(visiblePc);
						}
					}
				} else {
					for (L1DollInstance doll : pc.getDollList().values()) {
						L1Location loc = pc.getLocation().randomLocation(3, false);
						int nx = loc.getX();
						int ny = loc.getY();
						teleport(doll, nx, ny, mapId, head);
						pc.sendPackets(new S_DollPack(doll, pc));

						for (L1PcInstance visiblePc : L1World.getInstance().getVisiblePlayer(doll)) {
							visiblePc.removeKnownObject(doll);
							subjects.add(visiblePc);
						}
					}
				}
			}
		}

		for (L1PcInstance updatePc : subjects) {
			updatePc.updateObject();
		}

		if (pc.getZoneType() == ZoneType.Safety) {
			pc.setLastAggressiveAct(0);
		}

		pc.setTeleport(false);

		pc.startMapLimiter();

		// wrapped in a try/catch just to make sure the GM command can't
		// crap out the client packet
		try {
			L1PcInstance followingGm = pc.getFollowingGm();
			if (followingGm != null)
				L1Follow.moveChar(pc, followingGm);
		} catch (Exception ex) {
			_log.warn("L1Follow Teleport: " + ex.getMessage());
		}

		if (pc.hasSkillEffect(WIND_SHACKLE)) {
			pc.sendPackets(new S_SkillIconWindShackle(pc.getId(), pc.getSkillEffectTimeSec(WIND_SHACKLE)));
		}
		if (pc.hasSkillEffect(BLOODLUST)) {
			pc.sendPackets(new S_SkillBrave(pc.getId(), 1, pc.getSkillEffectTimeSec(BLOODLUST)));
			pc.broadcastPacket(new S_SkillBrave(pc.getId(), 1, 0));
		}
	}

	private static void teleport(L1NpcInstance npc, int x, int y, short map, int head) {
		L1World.getInstance().moveVisibleObject(npc, map);

		L1WorldMap.getInstance().getMap(npc.getMapId()).setPassable(npc.getX(), npc.getY(), true);
		npc.setX(x);
		npc.setY(y);
		npc.setMap(map);
		npc.setHeading(head);
		L1WorldMap.getInstance().getMap(npc.getMapId()).setPassable(npc.getX(), npc.getY(), false);
	}
}