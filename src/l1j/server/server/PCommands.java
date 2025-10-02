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
package l1j.server.server;

import static l1j.server.server.model.skill.L1SkillId.ABSOLUTE_BARRIER;
import static l1j.server.server.model.skill.L1SkillId.ADVANCE_SPIRIT;
import static l1j.server.server.model.skill.L1SkillId.BERSERKERS;
import static l1j.server.server.model.skill.L1SkillId.BLESS_WEAPON;
import static l1j.server.server.model.skill.L1SkillId.BONE_BREAK;
import static l1j.server.server.model.skill.L1SkillId.CONFUSION;
import static l1j.server.server.model.skill.L1SkillId.DECREASE_WEIGHT;
import static l1j.server.server.model.skill.L1SkillId.EARTH_BIND;
import static l1j.server.server.model.skill.L1SkillId.EARTH_SKIN;
import static l1j.server.server.model.skill.L1SkillId.GREATER_HASTE;
import static l1j.server.server.model.skill.L1SkillId.HASTE;
import static l1j.server.server.model.skill.L1SkillId.HOLY_WEAPON;
import static l1j.server.server.model.skill.L1SkillId.IMMUNE_TO_HARM;
import static l1j.server.server.model.skill.L1SkillId.LIGHT;
import static l1j.server.server.model.skill.L1SkillId.MASS_SHOCK_STUN;
import static l1j.server.server.model.skill.L1SkillId.NATURES_TOUCH;
import static l1j.server.server.model.skill.L1SkillId.PHYSICAL_ENCHANT_DEX;
import static l1j.server.server.model.skill.L1SkillId.PHYSICAL_ENCHANT_STR;
import static l1j.server.server.model.skill.L1SkillId.PURIFY_STONE;
import static l1j.server.server.model.skill.L1SkillId.SHIELD;
import static l1j.server.server.model.skill.L1SkillId.SHOCK_STUN;
import static l1j.server.server.model.skill.L1SkillId.STORM_SHOT;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l1j.server.Config;
import l1j.server.server.datatables.LogReporterTable;
import l1j.server.server.datatables.NpcSpawnTable;
import l1j.server.server.model.L1Object;
import l1j.server.server.model.L1Teleport;
import l1j.server.server.model.L1World;
import l1j.server.server.model.Instance.L1BoardInstance;
import l1j.server.server.model.Instance.L1ItemInstance;
import l1j.server.server.model.Instance.L1NpcInstance;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.model.skill.L1SkillUse;
import l1j.server.server.serverpackets.S_SystemMessage;

public class PCommands {
	private static Logger _log = LoggerFactory.getLogger(PCommands.class.getName());
	private static PCommands _instance;

	private static int[] PowerBuffSkills = { DECREASE_WEIGHT, PHYSICAL_ENCHANT_DEX, PHYSICAL_ENCHANT_STR, BLESS_WEAPON,
			GREATER_HASTE, BERSERKERS, IMMUNE_TO_HARM, ABSOLUTE_BARRIER, ADVANCE_SPIRIT, STORM_SHOT, EARTH_SKIN,
			NATURES_TOUCH };

	private static int[] BuffSkills = { LIGHT, SHIELD, HOLY_WEAPON, DECREASE_WEIGHT, PHYSICAL_ENCHANT_DEX,
			PHYSICAL_ENCHANT_STR, BLESS_WEAPON, HASTE };

	// Starting experiment to see whether caching common messages has an
	// effect.
	private static final S_SystemMessage DropHelp = new S_SystemMessage(
			"-drop [all|mine|party] [on|off] toggles drop messages.");
	private static final S_SystemMessage CommandsHelp = new S_SystemMessage(
			"-warp 1-10, -karma, -buff, -bug, -drop, -help, -dkbuff, -dmg, -potions, -report");
	private static final S_SystemMessage CommandsHelpNoBuff = new S_SystemMessage(
			"-rank, -warp 1-10, -karma, -bug, -drop, -help");
	private static final S_SystemMessage NoBuff = new S_SystemMessage("The -buff command is disabled.");
	private static final S_SystemMessage CannotBuff = new S_SystemMessage(
			"You cannot use -buff in your current state.");
	private static final S_SystemMessage BuffLevel = new S_SystemMessage("You must be level 45 to use -buff.");
	private static final S_SystemMessage NoWarpArea = new S_SystemMessage("You cannot -warp in this area.");
	private static final S_SystemMessage NoWarpState = new S_SystemMessage("You cannot -warp in your current state.");
	private static final S_SystemMessage NoWarp = new S_SystemMessage("The -warp command is disabled.");
	private static final S_SystemMessage NoPowerBuff = new S_SystemMessage("The -pbuff command is disabled.");
	private static final S_SystemMessage WarpLimit = new S_SystemMessage("-warp 1-7 only.");
	private static final S_SystemMessage WarpHelp = new S_SystemMessage(
			"-warp 1-Pandora, 2-SKT, 3-Giran, 4-Werldern, 5-Oren, 6-Orc Town, 7-Silent Cavern, 8-Gludio, 9-Silveria, 10-Behimous");
	private static final S_SystemMessage NotDK = new S_SystemMessage("Only Dragon Knights can use -dkbuff.");
	private static final S_SystemMessage DKHelp = new S_SystemMessage(
			"You have to equip Helm of Magic to use -dkbuff.");
	private static final S_SystemMessage NoMp = new S_SystemMessage("You don't have enough mana to use -dkbuff.");
	private static final S_SystemMessage NoDkBuff = new S_SystemMessage("The -dkbuff command is disabled.");
	private static final S_SystemMessage DmgHelp = new S_SystemMessage("dmg [on|off] toggles damage messages.");
	private static final S_SystemMessage PotionHelp = new S_SystemMessage(
			"potion [on|off] toggles healing potion messages.");
	private static final S_SystemMessage NoAutoTurning = new S_SystemMessage("The -turn command is disabled.");
	private static final S_SystemMessage OnlyDarkElvesTurn = new S_SystemMessage("Only Dark Elves can use -turn.");
	private static final S_SystemMessage ReportHelp = new S_SystemMessage("-report <charname> <reason>");

	private PCommands() {
	}

	public static PCommands getInstance() {
		if (_instance == null) {
			_instance = new PCommands();
		}
		return _instance;
	}

	public void handleCommands(L1PcInstance player, String cmd2) {
		try {
			if (cmd2.equalsIgnoreCase("help")) {
				showPHelp(player);
			} else if (cmd2.startsWith("buff")) {
				buff(player);
			} else if (cmd2.startsWith("dkbuff")) {
				dkbuff(player);
			} else if (cmd2.startsWith("warp")) {
				warp(player, cmd2);
			} else if (cmd2.startsWith("pbuff")) {
				powerBuff(player);
			} else if (cmd2.startsWith("bug")) {
				reportBug(player, cmd2);
			} else if (cmd2.startsWith("karma")) {
				checkKarma(player);
			} else if (cmd2.startsWith("drop")) {
				setDropOptions(player, cmd2);
			} else if (cmd2.startsWith("dmg")) {
				setDmgOptions(player, cmd2);
			} else if (cmd2.startsWith("potions")) {
				setPotionOptions(player, cmd2);
			} else if (cmd2.startsWith("turn")) {
				turnAllStones(player);
			} else if (cmd2.startsWith("report")) {
				try {
					String args[] = cmd2.split(" ");

					if (args.length < 3) {
						player.sendPackets(new S_SystemMessage("You must enter a reason!"));
						return;
					}

					String targetName = args[1];
					StringBuilder reason = new StringBuilder();

					for (int i = 2; i < args.length; i++) {
						reason.append(args[i] + " ");
					}

					L1PcInstance target = L1World.getInstance().getPlayer(targetName);

					long lastReport = LogReporterTable.getLastReport(player);

					// if they've reported someone in the last x hours
					long resetMillis = Config.REPORT_HOURS_RESET * 3600000;
					if (lastReport > System.currentTimeMillis() - resetMillis) {
						player.sendPackets(new S_SystemMessage(
								"You can only report someone every " + Config.REPORT_HOURS_RESET + " hours!"));
						return;
					}

					if (target == null || target.isGm()) {
						player.sendPackets(
								new S_SystemMessage("Cannot report " + targetName + " because they're offline!"));
						return;
					}

					if (target.getId() == player.getId()) {
						player.sendPackets(new S_SystemMessage("You cannot report yourself!"));
						return;
					}

					target.enableLogPackets();
					int insertedId = LogReporterTable.storeLogReport(player.getId(), player.getAccountName(),
							player.getNetConnection().getIp(), target.getId(), target.getName(), reason.toString());

					if (insertedId == -1) {
						player.sendPackets(new S_SystemMessage("There was an error reporting the target. Try again!"));
						return;
					}

					player.sendPackets(new S_SystemMessage(target.getName() + " has been reported!"));

					// Iterator<Packet> packetIterator =
					// target.getNetConnection().getLastClientPackets().iterator();
//					long firstPacketOfLog = -1;
//
//					while (packetIterator.hasNext()) {
//						Packet packet = packetIterator.next();
//
//						if(firstPacketOfLog == -1) {
//							firstPacketOfLog = packet.getTimestamp();
//						}
//
//						LogPacketsTable.storeLogPacket(target.getId(),
//								target.getName(),
//								target.getTempCharGfx(),
//								packet.getOpCode(),
//								packet.getPacket(),
//								"report",
//								packet.getTimestamp());
//					}

					// target.getNetConnection().clearClientPacketLog();
					// LogReporterTable.updatePacketStartTimestamp(insertedId, firstPacketOfLog);
				} catch (Exception ex) {
					player.sendPackets(ReportHelp);
				}
			}
			_log.trace(player.getName() + " used " + cmd2);
		} catch (Exception e) {
			_log.error(e.getLocalizedMessage(), e);
		}
	}

	public void showPHelp(L1PcInstance player) {
		player.sendPackets(Config.PLAYER_BUFF && Config.PLAYER_COMMANDS ? CommandsHelp : CommandsHelpNoBuff);
	}

	public void buff(L1PcInstance player) {
		if (!Config.PLAYER_BUFF || !Config.PLAYER_COMMANDS) {
			player.sendPackets(NoBuff);
			return;
		}

		if (player.isPrivateShop() || player.hasSkillEffect(EARTH_BIND) || player.hasSkillEffect(SHOCK_STUN)
				|| player.hasSkillEffect(MASS_SHOCK_STUN) || player.hasSkillEffect(BONE_BREAK)
				|| player.hasSkillEffect(CONFUSION) || player.isParalyzed() || player.isPinkName() || player.isSleeped()
				|| player.isDead() || player.getMapId() == 99) {
			player.sendPackets(CannotBuff);
			return;
		}

		int level = player.getLevel();
		if (level < 45) {
			player.sendPackets(BuffLevel);
			return;
		}
		int max = 3;
		/*
		 * out for WK temporarily. if (level >= 60) max = BuffSkills.length; else if
		 * (level >= 55) max = 5; else if (level >= 50) max = 4;
		 */

		L1SkillUse skillUse = new L1SkillUse();
		for (int i = 0; i < max; i++)
			skillUse.handleCommands(player, BuffSkills[i], player.getId(), player.getX(), player.getY(), null, 0,
					L1SkillUse.TYPE_SPELLSC);
	}

	public void dkbuff(L1PcInstance player) {
		if (!Config.PLAYER_COMMANDS) {
			player.sendPackets(NoBuff);
			return;
		}

		if (!Config.DK_BUFF) {
			player.sendPackets(NoDkBuff);
			return;
		}

		if (!player.isDragonKnight()) {
			player.sendPackets(NotDK);
			return;
		}

		if (player.getCurrentMp() < 25) {
			player.sendPackets(NoMp);
			return;
		}

		L1SkillUse skillUse = new L1SkillUse();
		if (player.getInventory().checkEquipped(20013)) {
			skillUse.handleCommands(player, PHYSICAL_ENCHANT_DEX, player.getId(), player.getX(), player.getY(), null, 0,
					L1SkillUse.TYPE_NORMAL);
		} else if (player.getInventory().checkEquipped(20015)) {
			skillUse.handleCommands(player, PHYSICAL_ENCHANT_STR, player.getId(), player.getX(), player.getY(), null, 0,
					L1SkillUse.TYPE_NORMAL);
		} else
			player.sendPackets(DKHelp);
	}

	public void powerBuff(L1PcInstance player) {
		if (player.isDead() || player.isGhost() || player.isParalyzed() || player.hasSkillEffect(EARTH_BIND)
				|| player.hasSkillEffect(SHOCK_STUN) || player.hasSkillEffect(MASS_SHOCK_STUN)
				|| player.hasSkillEffect(BONE_BREAK) || player.hasSkillEffect(CONFUSION)) {
			return;
		}
		if (Config.POWER_BUFF && Config.PLAYER_COMMANDS) {
			L1SkillUse skillUse = new L1SkillUse();
			for (int powerBuffSkill : PowerBuffSkills)
				skillUse.handleCommands(player, powerBuffSkill, player.getId(), player.getX(), player.getY(), null,
						0, L1SkillUse.TYPE_SPELLSC);
		} else if (Config.PLAYER_COMMANDS && !Config.POWER_BUFF)
			player.sendPackets(NoPowerBuff);
	}

	public void warp(L1PcInstance player, String cmd2) {
		if (!Config.WARP) {
			player.sendPackets(NoWarp);
			return;
		}

		if (!player.getLocation().getMap().isEscapable()) {
			player.sendPackets(NoWarpArea);
			return;
		}

		if (player.isPrivateShop() || player.hasSkillEffect(EARTH_BIND) || player.hasSkillEffect(SHOCK_STUN)
				|| player.hasSkillEffect(MASS_SHOCK_STUN) || player.hasSkillEffect(BONE_BREAK)
				|| player.hasSkillEffect(CONFUSION) || player.isParalyzed() || player.isPinkName() || player.isSleeped()
				|| player.isDead() || player.getMapId() == 99) {
			player.sendPackets(NoWarpState);
			return;
		}

		try {
			int i = Integer.parseInt(cmd2.substring(5));
			if (i >= 1 && i <= 10) {
				if (System.currentTimeMillis() - player.getLastWarp() < 3000) {
					return;
				} else {
					player.setLastWarp(System.currentTimeMillis());
				}

				if (player.isPrivateShop() || player.hasSkillEffect(EARTH_BIND) || player.hasSkillEffect(SHOCK_STUN)
						|| player.hasSkillEffect(MASS_SHOCK_STUN) || player.hasSkillEffect(BONE_BREAK)
						|| player.hasSkillEffect(CONFUSION) || player.isParalyzed() || player.isPinkName()
						|| player.isSleeped() || player.isDead() || player.getMapId() == 99) {
					player.sendPackets(NoWarpState);
					return;
				}
				WarpDelay warpDelay = null;
				switch (i) {
				case 1: // Pandora
					warpDelay = new WarpDelay(player, 32644, 32955, (short) 0, 5, true);
					break;
				case 2: // SKT
					warpDelay = new WarpDelay(player, 33080, 33392, (short) 4, 5, true);
					break;
				case 3: // Giran
					warpDelay = new WarpDelay(player, 33442, 32797, (short) 4, 5, true);
					break;
				case 4: // Weldern
					warpDelay = new WarpDelay(player, 33705, 32504, (short) 4, 5, true);
					break;
				case 5: // Oren
					warpDelay = new WarpDelay(player, 34061, 32276, (short) 4, 5, true);
					break;
				case 6: // Orc Town
					warpDelay = new WarpDelay(player, 32715, 32448, (short) 4, 5, true);
					break;
				case 7: // Silent Cave
					warpDelay = new WarpDelay(player, 32857, 32898, (short) 304, 5, true);
					break;
				case 8: // Gludio
					warpDelay = new WarpDelay(player, 32608, 32734, (short) 4, 5, true);
					break;
				case 9: // Silveria
					warpDelay = new WarpDelay(player, 32841, 32856, (short) 1000, 5, true);
					break;
				case 10: // Behimous
					warpDelay = new WarpDelay(player, 32779, 32887, (short) 1001, 5, true);
				}
				if (warpDelay != null) {
					GeneralThreadPool.getInstance().schedule(warpDelay, 3000);
				}
			} else {
				player.sendPackets(WarpLimit);
			}
		} catch (Exception exception) {
			player.sendPackets(WarpHelp);
		}
	}

	private class WarpDelay implements Runnable {

		L1PcInstance player;
		int x;
		int y;
		short mapid;
		int heading;
		boolean effectable;

		public WarpDelay(L1PcInstance player, int x, int y, short mapid, int heading, boolean effectable) {
			this.player = player;
			this.x = x;
			this.y = y;
			this.mapid = mapid;
			this.heading = heading;
			this.effectable = effectable;
		}

		@Override
		public void run() {
			try {
				L1Teleport.teleport(player, x, y, mapid, heading, effectable);
				player.setLastWarp(System.currentTimeMillis());

			} catch (Exception e) {
				// TODO Auto-generated catch block
				_log.error("", e);
			}
		}

	}

	private void reportBug(L1PcInstance pc, String bug) {
		Collection<L1Object> objects = L1World.getInstance().getObject();
		L1Object bugBoard = null;

		for (L1Object object : objects) {
			if (object instanceof L1NpcInstance) {
				L1NpcInstance npcObject = (L1NpcInstance) object;
				if (npcObject.getSpawn() != null && npcObject.getSpawn() == NpcSpawnTable.getBugBoard())
					bugBoard = npcObject;
			}
		}

		L1BoardInstance board = (L1BoardInstance) bugBoard;
		board.onAction(pc, 0);
	}

	private void checkKarma(L1PcInstance pc) {
		pc.sendPackets(new S_SystemMessage("Your karma is currently: " + pc.getKarma() + "."));
	}

	private void setDropOptions(final L1PcInstance pc, final String options) {
		List<String> pieces = Arrays.asList(options.split("\\s"));
		if (pieces.size() < 3) {
			pc.sendPackets(DropHelp);
			return;
		}
		boolean on = pieces.get(2).equals("on");
		if (pieces.get(1).equals("all")) {
			pc.setDropMessages(on);
			pc.setPartyDropMessages(on);
		} else if (pieces.get(1).equals("party")) {
			pc.setPartyDropMessages(on);
		} else if (pieces.get(1).equals("mine")) {
			pc.setDropMessages(on);
		} else {
			pc.sendPackets(DropHelp);
		}
	}

	private void setDmgOptions(final L1PcInstance pc, final String options) {
		List<String> pieces = Arrays.asList(options.split("\\s"));
		if (pieces.size() < 2) {
			pc.sendPackets(DmgHelp);
			return;
		}
		if (pieces.get(1).equals("on")) {
			pc.setDmgMessages(true);
		} else if (pieces.get(1).equals("off")) {
			pc.setDmgMessages(false);
		} else {
			pc.sendPackets(DmgHelp);
		}
	}

	private void setPotionOptions(final L1PcInstance pc, final String options) {
		List<String> pieces = Arrays.asList(options.split("\\s"));
		if (pieces.size() < 2) {
			pc.sendPackets(PotionHelp);
			return;
		}
		if (pieces.get(1).equals("on")) {
			pc.setPotionMessages(true);
		} else if (pieces.get(1).equals("off")) {
			pc.setPotionMessages(false);
		} else {
			pc.sendPackets(PotionHelp);
		}
	}

	private void turnAllStones(final L1PcInstance player) {
		if (!Config.AUTO_STONE) {
			player.sendPackets(NoAutoTurning);
			return;
		}

		if (!player.isDarkelf() || !player.isSkillMastery(PURIFY_STONE)) {
			player.sendPackets(OnlyDarkElvesTurn);
			return;
		}

		// TODO: Ugly hack. Should go through the normal skill mechanisms.
		l1j.server.server.templates.L1Skill skill = l1j.server.server.datatables.SkillTable.getInstance()
				.findBySkillId(PURIFY_STONE);
		int currentMana = player.getCurrentMp();
		int castingCost = skill.getMpConsume();
		for (int stone : l1j.server.server.model.item.L1ItemId.StoneList) {
			L1ItemInstance item = player.getInventory().findItemId(stone);
			if (item == null)
				continue;
			L1SkillUse.turnStone(player, item, .9, Math.min(item.getCount(), currentMana / castingCost), false);
			player.setCurrentMp(player.getCurrentMp() % castingCost);
			break;
		}
	}
}
