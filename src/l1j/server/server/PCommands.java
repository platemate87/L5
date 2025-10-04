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
import static l1j.server.server.model.skill.L1SkillId.COOKING_2_0_N;
import static l1j.server.server.model.skill.L1SkillId.COOKING_2_0_S;
import static l1j.server.server.model.skill.L1SkillId.COOKING_2_3_N;
import static l1j.server.server.model.skill.L1SkillId.COOKING_2_3_S;
import static l1j.server.server.model.skill.L1SkillId.COOKING_3_0_N;
import static l1j.server.server.model.skill.L1SkillId.COOKING_3_0_S;
import static l1j.server.server.model.skill.L1SkillId.COOKING_3_2_N;
import static l1j.server.server.model.skill.L1SkillId.COOKING_3_2_S;
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
import static l1j.server.server.model.skill.L1SkillId.SOUL_OF_FLAME;
import static l1j.server.server.model.skill.L1SkillId.STORM_SHOT;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l1j.server.Config;
import l1j.server.server.clientpackets.C_CreateParty;
import l1j.server.server.datatables.LogReporterTable;
import l1j.server.server.datatables.NpcSpawnTable;
import l1j.server.server.model.L1Object;
import l1j.server.server.model.L1Teleport;
import l1j.server.server.model.L1World;
import l1j.server.server.model.Instance.L1BoardInstance;
import l1j.server.server.model.Instance.L1DollInstance;
import l1j.server.server.model.Instance.L1ItemInstance;
import l1j.server.server.model.Instance.L1NpcInstance;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.model.skill.L1SkillUse;
import l1j.server.server.serverpackets.S_ServerMessage;
import l1j.server.server.serverpackets.S_SystemMessage;
import l1j.server.server.utils.CalcStat;
import l1j.server.server.model.item.L1ItemId;
import l1j.server.server.model.item.WeaponType;
import l1j.server.server.templates.L1Item;

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
                        "-warp 1-10, -karma, -buff, -stats, -bug, -drop, -help, -dkbuff, -dmg, -potions, -invite, -report");
        private static final S_SystemMessage CommandsHelpNoBuff = new S_SystemMessage(
                        "-rank, -warp 1-10, -karma, -stats, -bug, -drop, -help, -invite");
        private static final S_SystemMessage InviteUsage = new S_SystemMessage("-invite <playername>");
        private static final S_SystemMessage InviteDisabled = new S_SystemMessage("The -invite command is disabled.");
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
        private static final S_SystemMessage StatsDisabled = new S_SystemMessage("The -stats command is disabled.");

        private static final int[] STR_HIT = { -2, -2, -2, -2, -2, -2, -2, -2, -1, -1, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 5,
                        6, 6, 6, 7, 7, 7, 8, 8, 8, 9, 9, 9, 10, 10, 10, 11, 11, 11, 12, 12, 12, 13, 13, 13, 14, 14, 14, 15, 15,
                        15, 16, 16, 16, 17, 17, 17 };

        private static final int[] DEX_HIT = { -2, -2, -2, -2, -2, -2, -1, -1, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 6, 7, 8, 9, 10,
                        11, 12, 13, 14, 15, 16, 17, 18, 19, 19, 19, 20, 20, 20, 21, 21, 21, 22, 22, 22, 23, 23, 23, 24, 24, 24,
                        25, 25, 25, 26, 26, 26, 27, 27, 27, 28 };

        private static final int[] STR_DMG = new int[128];
        private static final int[] DEX_DMG = new int[128];

        static {
                int dmg = -6;
                for (int str = 0; str <= 22; str++) {
                        if (str % 2 == 1) {
                                dmg++;
                        }
                        STR_DMG[str] = dmg;
                }
                for (int str = 23; str <= 28; str++) {
                        if (str % 3 == 2) {
                                dmg++;
                        }
                        STR_DMG[str] = dmg;
                }
                for (int str = 29; str <= 32; str++) {
                        if (str % 2 == 1) {
                                dmg++;
                        }
                        STR_DMG[str] = dmg;
                }
                for (int str = 33; str <= 34; str++) {
                        dmg++;
                        STR_DMG[str] = dmg;
                }
                for (int str = 35; str <= 127; str++) {
                        if ((str + 1) % 4 == 0) {
                                dmg++;
                        }
                        STR_DMG[str] = dmg;
                }

                for (int dex = 0; dex <= 14; dex++) {
                        DEX_DMG[dex] = 0;
                }
                DEX_DMG[15] = 1;
                DEX_DMG[16] = 2;
                DEX_DMG[17] = 3;
                DEX_DMG[18] = 4;
                DEX_DMG[19] = 4;
                DEX_DMG[20] = 4;
                DEX_DMG[21] = 5;
                DEX_DMG[22] = 5;
                DEX_DMG[23] = 5;
                dmg = 5;
                for (int dex = 24; dex <= 35; dex++) {
                        if (dex % 3 == 0) {
                                dmg++;
                        }
                        DEX_DMG[dex] = dmg;
                }
                for (int dex = 36; dex <= 127; dex++) {
                        if (dex % 4 == 0) {
                                dmg++;
                        }
                        DEX_DMG[dex] = dmg;
                }
        }

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
                        } else if (cmd2.startsWith("stats")) {
                                showStats(player);
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
                        } else if (cmd2.startsWith("invite")) {
                                invite(player, cmd2);
                        }
                        _log.trace(player.getName() + " used " + cmd2);
                } catch (Exception e) {
                        _log.error(e.getLocalizedMessage(), e);
                }
        }

        private void invite(L1PcInstance player, String cmd2) {
                if (!Config.PLAYER_COMMANDS) {
                        player.sendPackets(InviteDisabled);
                        return;
                }

                String[] args = cmd2.split("\\s+");
                if (args.length < 2) {
                        player.sendPackets(InviteUsage);
                        return;
                }

                String targetName = args[1];
                if (targetName.isEmpty()) {
                        player.sendPackets(InviteUsage);
                        return;
                }

                L1PcInstance target = L1World.getInstance().getPlayer(targetName);
                if (target == null) {
                        player.sendPackets(new S_ServerMessage(109));
                        return;
                }

                C_CreateParty.inviteByName(player, target);
        }

        public void showPHelp(L1PcInstance player) {
                player.sendPackets(Config.PLAYER_BUFF && Config.PLAYER_COMMANDS ? CommandsHelp : CommandsHelpNoBuff);
        }

        private void showStats(L1PcInstance player) {
                if (!Config.PLAYER_COMMANDS) {
                        player.sendPackets(StatsDisabled);
                        return;
                }

                player.sendPackets(new S_SystemMessage("-- stats: " + player.getName() + " --"));

                int er = player.getEr();
                int wisMr = CalcStat.calcStatMr(player.getWis());
                int mr = player.getMr();
                int hpr = player.getHpr() + player.getInventory().hpRegenPerTick();
                int mpr = player.getMpr() + player.getInventory().mpRegenPerTick();

                CombatSnapshot snapshot = buildCombatSnapshot(player);

                player.sendPackets(new S_SystemMessage(String.format(
                                "ER: %d | MR: %d | WIS MR: %d | HPR: %d | MPR: %d", er, mr, wisMr, hpr, mpr)));
                player.sendPackets(new S_SystemMessage(String.format(
                                "Hit: +%d | Dmg (S): %d-%d | Dmg (L): %d-%d | Base Magic Hit: +%d | Magic Dmg Bonus: +%d | Magic Crit: %d",
                                snapshot.hitRate, snapshot.small.min, snapshot.small.max, snapshot.large.min,
                                snapshot.large.max, player.getOriginalMagicHit(), player.getOriginalMagicDamage(),
                                player.getOriginalMagicCritical())));
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

        private static CombatSnapshot buildCombatSnapshot(L1PcInstance player) {
                WeaponSnapshot weapon = buildWeaponSnapshot(player);
                int hitRate = computeHitRate(player, weapon);
                DamageRange small = computeDamageRange(player, weapon, true);
                DamageRange large = computeDamageRange(player, weapon, false);
                return new CombatSnapshot(hitRate, small, large);
        }

        private static DamageRange computeDamageRange(L1PcInstance player, WeaponSnapshot weapon, boolean smallTarget) {
                int weaponBase = smallTarget ? weapon.weaponSmall : weapon.weaponLarge;
                int weaponMin = 0;
                int weaponMax = 0;

                if (weapon.isRanged || weapon.weaponType == WeaponType.Fist) {
                        weaponMin = 0;
                        weaponMax = 0;
                } else if (weaponBase > 0) {
                        weaponMax = weaponBase;
                        if (player.hasSkillEffect(SOUL_OF_FLAME)) {
                                weaponMin = weaponBase;
                        } else {
                                weaponMin = 1;
                        }
                }

                int projectileMin = 0;
                int projectileMax = 0;
                if (weapon.isBow) {
                        if (weapon.arrow != null) {
                                int arrowBase = smallTarget ? weapon.arrow.getItem().getDmgSmall()
                                                : weapon.arrow.getItem().getDmgLarge();
                                if (arrowBase <= 0) {
                                        arrowBase = 1;
                                }
                                projectileMin = 1;
                                projectileMax = arrowBase;
                        } else if (weapon.weaponId == L1ItemId.SAYHAS_BOW) {
                                projectileMin = 1;
                                projectileMax = 15;
                        }
                } else if (weapon.isGauntlet && weapon.sting != null) {
                        int stingBase = smallTarget ? weapon.sting.getItem().getDmgSmall()
                                        : weapon.sting.getItem().getDmgLarge();
                        if (stingBase <= 0) {
                                stingBase = 1;
                        }
                        projectileMin = 1;
                        projectileMax = stingBase;
                }

                int additive = weapon.weaponAddDmg + weapon.weaponEnchant + weapon.statusDamage + weapon.additionalDamage;
                int min = weaponMin + projectileMin + additive;
                int max = weaponMax + projectileMax + additive;

                if (weapon.weaponType == WeaponType.Fist) {
                        min = 1;
                        max = 2;
                }

                if (min < 0) {
                        min = 0;
                }
                if (max < min) {
                        max = min;
                }

                return new DamageRange(min, max);
        }

        private static int computeHitRate(L1PcInstance player, WeaponSnapshot weapon) {
                int hitRate = player.getLevel();
                hitRate += getStrHitBonus(player.getStr());
                hitRate += getDexHitBonus(player.getDex());
                hitRate += weapon.weaponAddHit + weapon.weaponEnchant / 2;
                if (weapon.isRanged) {
                        hitRate += player.getBowHitup() + player.getOriginalBowHitup() + player.getBowHitModifierByArmor();
                } else {
                        hitRate += player.getHitup() + player.getOriginalHitup() + player.getHitModifierByArmor();
                }
                hitRate += getWeightHitModifier(player);
                hitRate += getCookingHitModifier(player, weapon.isRanged);
                hitRate += getDollHitModifier(player, weapon.isRanged);
                return hitRate;
        }

        private static WeaponSnapshot buildWeaponSnapshot(L1PcInstance player) {
                WeaponSnapshot snapshot = new WeaponSnapshot();
                L1ItemInstance weapon = player.getWeapon();

                snapshot.weaponType = WeaponType.Fist;
                snapshot.weaponId = 0;
                snapshot.weaponSmall = 0;
                snapshot.weaponLarge = 0;
                snapshot.weaponAddHit = 0;
                snapshot.weaponAddDmg = 0;
                snapshot.weaponEnchant = 0;
                snapshot.isBow = false;
                snapshot.isGauntlet = false;
                snapshot.isRanged = false;
                snapshot.attrEnchantLevel = 0;

                if (weapon != null) {
                        L1Item item = weapon.getItem();
                        snapshot.weaponId = item.getItemId();
                        snapshot.weaponType = item.getType1();
                        snapshot.weaponSmall = item.getDmgSmall();
                        snapshot.weaponLarge = item.getDmgLarge();
                        snapshot.weaponAddHit = item.getHitModifier() + weapon.getHitByMagic();
                        snapshot.weaponAddDmg = item.getDmgModifier() + weapon.getDmgByMagic();
                        snapshot.isBow = snapshot.weaponType == WeaponType.Bow;
                        snapshot.isGauntlet = snapshot.weaponType == WeaponType.Gauntlet;
                        snapshot.isRanged = snapshot.isBow || snapshot.isGauntlet;
                        snapshot.weaponEnchant = weapon.getEnchantLevel() - (snapshot.isRanged ? 0 : weapon.get_durability());
                        if (snapshot.isBow) {
                                snapshot.arrow = player.getInventory().getArrow();
                        } else if (snapshot.isGauntlet) {
                                snapshot.sting = player.getInventory().getSting();
                        }
                        snapshot.attrEnchantLevel = weapon.getAttrEnchantLevel();
                }

                snapshot.statusDamage = snapshot.isBow ? getDexDamage(player.getDex()) : getStrDamage(player.getStr());
                snapshot.attrEnchantDamage = Config.ELEMENTAL_ENCHANTING && snapshot.attrEnchantLevel > 0
                                ? snapshot.attrEnchantLevel * 2 - 1
                                : 0;

                int damageBonus = snapshot.isRanged
                                ? player.getBowDmgup() + player.getOriginalBowDmgup() + player.getBowDmgModifierByArmor()
                                : player.getDmgup() + player.getOriginalDmgup() + player.getDmgModifierByArmor();
                damageBonus += getCookingDmgModifier(player, snapshot.isRanged);
                damageBonus += getDollDmgModifier(player, snapshot.isRanged);
                damageBonus += snapshot.attrEnchantDamage;
                snapshot.additionalDamage = damageBonus;

                return snapshot;
        }

        private static int getWeightHitModifier(final L1PcInstance pc) {
                int weightModifier = 0;
                int currentWeight = pc.getInventory().getWeight240();
                if (80 < currentWeight && 120 >= currentWeight) {
                        weightModifier = -1;
                } else if (121 <= currentWeight && 160 >= currentWeight) {
                        weightModifier = -3;
                } else if (161 <= currentWeight && 200 >= currentWeight) {
                        weightModifier = -5;
                }
                return weightModifier;
        }

        private static int getCookingHitModifier(final L1PcInstance pc, final boolean ranged) {
                int cookingModifier = 0;
                if (!ranged && (pc.hasSkillEffect(COOKING_2_0_N) || pc.hasSkillEffect(COOKING_2_0_S))) {
                        cookingModifier += 1;
                }
                if (!ranged && (pc.hasSkillEffect(COOKING_3_2_N) || pc.hasSkillEffect(COOKING_3_2_S))) {
                        cookingModifier += 2;
                }
                if (ranged && (pc.hasSkillEffect(COOKING_2_3_N) || pc.hasSkillEffect(COOKING_2_3_S)
                                || pc.hasSkillEffect(COOKING_3_0_N) || pc.hasSkillEffect(COOKING_3_0_S))) {
                        cookingModifier += 1;
                }
                return cookingModifier;
        }

        private static int getCookingDmgModifier(final L1PcInstance pc, final boolean ranged) {
                int damage = 0;
                if (!ranged && (pc.hasSkillEffect(COOKING_2_0_N) || pc.hasSkillEffect(COOKING_2_0_S)
                                || pc.hasSkillEffect(COOKING_3_2_N) || pc.hasSkillEffect(COOKING_3_2_S))) {
                        damage += 1;
                }
                if (ranged && (pc.hasSkillEffect(COOKING_2_3_N) || pc.hasSkillEffect(COOKING_2_3_S)
                                || pc.hasSkillEffect(COOKING_3_0_N) || pc.hasSkillEffect(COOKING_3_0_S))) {
                        damage += 1;
                }
                return damage;
        }

        private static int getDollHitModifier(final L1PcInstance pc, final boolean ranged) {
                int hitRate = 0;
                for (Object dollObject : pc.getDollList().values()) {
                        L1DollInstance doll = (L1DollInstance) dollObject;
                        hitRate += ranged ? doll.getRangedHitByDoll() : doll.getMeleeHitByDoll();
                }
                return hitRate;
        }

        private static int getDollDmgModifier(final L1PcInstance pc, final boolean ranged) {
                int damage = 0;
                for (Object dollObject : pc.getDollList().values()) {
                        L1DollInstance doll = (L1DollInstance) dollObject;
                        damage += ranged ? doll.getRangedDmgByDoll() : doll.getMeleeDmgByDoll();
                }
                return damage;
        }

        private static int getStrHitBonus(int str) {
                if (str <= 0) {
                        return STR_HIT[0];
                }
                int index = Math.min(STR_HIT.length - 1, str - 1);
                return STR_HIT[index];
        }

        private static int getDexHitBonus(int dex) {
                if (dex <= 0) {
                        return DEX_HIT[0];
                }
                int index = Math.min(DEX_HIT.length - 1, dex - 1);
                return DEX_HIT[index];
        }

        private static int getStrDamage(int str) {
                if (str < 0) {
                        str = 0;
                }
                if (str >= STR_DMG.length) {
                        str = STR_DMG.length - 1;
                }
                return STR_DMG[str];
        }

        private static int getDexDamage(int dex) {
                if (dex < 0) {
                        dex = 0;
                }
                if (dex >= DEX_DMG.length) {
                        dex = DEX_DMG.length - 1;
                }
                return DEX_DMG[dex];
        }

        private static final class CombatSnapshot {
                private final int hitRate;
                private final DamageRange small;
                private final DamageRange large;

                private CombatSnapshot(int hitRate, DamageRange small, DamageRange large) {
                        this.hitRate = hitRate;
                        this.small = small;
                        this.large = large;
                }
        }

        private static final class DamageRange {
                private final int min;
                private final int max;

                private DamageRange(int min, int max) {
                        this.min = min;
                        this.max = max;
                }
        }

        private static final class WeaponSnapshot {
                private int weaponId;
                private int weaponType;
                private int weaponSmall;
                private int weaponLarge;
                private int weaponAddHit;
                private int weaponAddDmg;
                private int weaponEnchant;
                private boolean isBow;
                private boolean isGauntlet;
                private boolean isRanged;
                private L1ItemInstance arrow;
                private L1ItemInstance sting;
                private int statusDamage;
                private int attrEnchantLevel;
                private int attrEnchantDamage;
                private int additionalDamage;
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
