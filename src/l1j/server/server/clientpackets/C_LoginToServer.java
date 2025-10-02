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

import static l1j.server.server.model.skill.L1SkillId.COOKING_1_0_N;
import static l1j.server.server.model.skill.L1SkillId.COOKING_1_0_S;
import static l1j.server.server.model.skill.L1SkillId.COOKING_1_6_N;
import static l1j.server.server.model.skill.L1SkillId.COOKING_1_6_S;
import static l1j.server.server.model.skill.L1SkillId.COOKING_2_0_N;
import static l1j.server.server.model.skill.L1SkillId.COOKING_2_0_S;
import static l1j.server.server.model.skill.L1SkillId.COOKING_2_6_N;
import static l1j.server.server.model.skill.L1SkillId.COOKING_2_6_S;
import static l1j.server.server.model.skill.L1SkillId.COOKING_3_0_N;
import static l1j.server.server.model.skill.L1SkillId.COOKING_3_0_S;
import static l1j.server.server.model.skill.L1SkillId.COOKING_3_6_N;
import static l1j.server.server.model.skill.L1SkillId.COOKING_3_6_S;
import static l1j.server.server.model.skill.L1SkillId.SHAPE_CHANGE;
import static l1j.server.server.model.skill.L1SkillId.STATUS_BLUE_POTION;
import static l1j.server.server.model.skill.L1SkillId.STATUS_BRAVE;
import static l1j.server.server.model.skill.L1SkillId.STATUS_CHAT_PROHIBITED;
import static l1j.server.server.model.skill.L1SkillId.STATUS_ELFBRAVE;
import static l1j.server.server.model.skill.L1SkillId.STATUS_HASTE;

import java.io.File;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l1j.server.Config;
import l1j.server.L1DatabaseFactory;
import l1j.server.server.ActionCodes;
import l1j.server.server.GMCommands;
import l1j.server.server.command.L1Commands;
import l1j.server.server.controllers.JailController;
import l1j.server.server.controllers.JailController.JailInfo;
import l1j.server.server.controllers.WarTimeController;
import l1j.server.server.datatables.CharacterTable;
import l1j.server.server.datatables.GetBackRestartTable;
import l1j.server.server.datatables.LogReporterTable;
import l1j.server.server.datatables.SkillTable;
import l1j.server.server.log.LogIP;
import l1j.server.server.model.Getback;
import l1j.server.server.model.L1CastleLocation;
import l1j.server.server.model.L1Clan;
import l1j.server.server.model.L1Cooking;
import l1j.server.server.model.L1PolyMorph;
import l1j.server.server.model.L1War;
import l1j.server.server.model.L1World;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.model.Instance.L1SummonInstance;
import l1j.server.server.model.skill.L1SkillUse;
import l1j.server.server.network.Client;
import l1j.server.server.serverpackets.S_ActiveSpells;
import l1j.server.server.serverpackets.S_AddSkill;
import l1j.server.server.serverpackets.S_BookmarkLoad;
import l1j.server.server.serverpackets.S_CharTitle;
import l1j.server.server.serverpackets.S_CharacterConfig;
import l1j.server.server.serverpackets.S_Emblem;
import l1j.server.server.serverpackets.S_InitialAbilityGrowth;
import l1j.server.server.serverpackets.S_InvList;
import l1j.server.server.serverpackets.S_Karma;
import l1j.server.server.serverpackets.S_LoginGame;
import l1j.server.server.serverpackets.S_Mail;
import l1j.server.server.serverpackets.S_MapID;
import l1j.server.server.serverpackets.S_OwnCharPack;
import l1j.server.server.serverpackets.S_OwnCharStatus;
import l1j.server.server.serverpackets.S_PacketBox;
import l1j.server.server.serverpackets.S_PlayTime;
import l1j.server.server.serverpackets.S_RuneSlot;
import l1j.server.server.serverpackets.S_SPMR;
import l1j.server.server.serverpackets.S_ServerMessage;
import l1j.server.server.serverpackets.S_SkillBrave;
import l1j.server.server.serverpackets.S_SkillHaste;
import l1j.server.server.serverpackets.S_SkillIconGFX;
import l1j.server.server.serverpackets.S_SummonPack;
import l1j.server.server.serverpackets.S_SystemMessage;
import l1j.server.server.serverpackets.S_War;
import l1j.server.server.serverpackets.S_Weather;
import l1j.server.server.serverpackets.S_bonusstats;
import l1j.server.server.templates.L1Command;
import l1j.server.server.templates.L1GetBackRestart;
import l1j.server.server.templates.L1Skill;
import l1j.server.server.utils.SQLUtil;
import l1j.server.server.utils.SystemUtil;

// Referenced classes of package l1j.server.server.clientpackets:
// ClientBasePacket
public class C_LoginToServer extends ClientBasePacket {
	private static final String C_LOGIN_TO_SERVER = "[C] C_LoginToServer";
	private static Logger _log = LoggerFactory.getLogger(C_LoginToServer.class.getName());
	// See note on updateIcons()
	private static List<String> accountsWithIcons = new ArrayList<>();

	public C_LoginToServer(byte abyte0[], Client client) throws FileNotFoundException, Exception {
		super(abyte0);

		String login = client.getAccountName();
		String charName = readS();

		if (client.getActiveChar() != null) {
			_log.info("Invalid character logged in from " + client.getHostname() + ".");
			client.close();
			return;
		}
		L1PcInstance pc = L1PcInstance.load(charName);
		if (pc == null || !login.equals(pc.getAccountName())) {
			_log.info("Invalid login request=" + charName + " account=" + login + " host=" + client.getHostname());
			client.close();
			return;
		}

		// auto-run any auto-run commands the user has access to
		for (L1Command command : L1Commands.availableCommandList(pc.getAccessLevel().getLevel())) {
			if (command.isRunOnLogin())
				GMCommands.getInstance().handleCommands(pc, command.getName());
		}

		if (Config.LEVEL_DOWN_RANGE != 0) {
			if (pc.getHighLevel() - pc.getLevel() >= Config.LEVEL_DOWN_RANGE) {
				_log.info("Login request of the character which exceeded: char=" + charName + " account=" + login
						+ " host=" + client.getHostname());
				client.kick();
				return;
			}
		}
		_log.info("Character login: char=" + charName + " account=" + login + " host=" + client.getHostname()
				+ " Current Memory: " + SystemUtil.getUsedMemoryMB() + "MB RAM" + " CharactersOnline="
				+ (L1World.getInstance().getAllPlayers().size() + 1));
		client.setLastActiveCharName(charName);
		LogIP li = new LogIP();
		li.storeLogIP(pc, client.getHostname());

		int currentHpAtLoad = pc.getCurrentHp();
		int currentMpAtLoad = pc.getCurrentMp();

		pc.clearSkillMastery();

		pc.setOnlineStatus(1);
		CharacterTable.updateOnlineStatus(pc);

		L1World.getInstance().storeObject(pc);

		pc.setNetConnection(client);

		pc.setPacketOutput(client);
		client.setActiveChar(pc);

		pc.sendPackets(new S_InitialAbilityGrowth(pc));
		pc.sendPackets(new S_LoginGame());
		pc.sendPackets(new S_BookmarkLoad(pc));

		// TODO -- set minigame playing if working

		GetBackRestartTable gbrTable = GetBackRestartTable.getInstance();
		L1GetBackRestart[] gbrList = gbrTable.getGetBackRestartTableList();
		for (L1GetBackRestart gbr : gbrList) {
			if (pc.getMapId() == gbr.getArea()) {
				pc.setX(gbr.getLocX());
				pc.setY(gbr.getLocY());
				pc.setMap(gbr.getMapId());
				break;
			}
		}
		if (Config.GET_BACK) {
			int[] loc = Getback.GetBack_Location(pc, true);
			pc.setX(loc[0]);
			pc.setY(loc[1]);
			pc.setMap((short) loc[2]);
		}

		int castle_id = L1CastleLocation.getCastleIdByArea(pc);
		if (0 < castle_id) {
			if (WarTimeController.getInstance().isNowWar(castle_id)) {
				L1Clan clan = L1World.getInstance().getClan(pc.getClanname());
				if (clan != null) {
					if (clan.getCastleId() != castle_id) {
						int[] loc = new int[3];
						loc = L1CastleLocation.getGetBackLoc(castle_id);
						pc.setX(loc[0]);
						pc.setY(loc[1]);
						pc.setMap((short) loc[2]);
					}
				} else {
					int[] loc = new int[3];
					loc = L1CastleLocation.getGetBackLoc(castle_id);
					pc.setX(loc[0]);
					pc.setY(loc[1]);
					pc.setMap((short) loc[2]);
				}
			}
		}

		L1World.getInstance().addVisibleObject(pc);
		pc.sendPackets(new S_Mail(pc, 0));
		pc.sendPackets(new S_Mail(pc, 1));
		pc.sendPackets(new S_Mail(pc, 2));
		pc.sendPackets(new S_ActiveSpells(pc));

		pc.beginGameTimeCarrier();

		pc.sendPackets(new S_OwnCharStatus(pc));
		pc.sendPackets(new S_MapID(pc.getMapId(), pc.getMap().isUnderwater()));
		pc.sendPackets(new S_OwnCharPack(pc));
		pc.sendPackets(new S_SPMR(pc));

		S_CharTitle s_charTitle = new S_CharTitle(pc.getId(), pc.getTitle());
		pc.sendPackets(s_charTitle);
		pc.broadcastPacket(s_charTitle);

		pc.sendVisualEffectAtLogin();

		pc.sendPackets(new S_Weather(L1World.getInstance().getWeather()));

		items(pc);
		skills(pc);
		buff(client, pc);
		buffBlessOfAin(pc);

		pc.turnOnOffLight();

		pc.sendPackets(new S_Karma(pc));
		pc.sendPackets(new S_PacketBox(S_PacketBox.DODGE_RATE_PLUS, pc.getDodge()));
		pc.sendPackets(new S_PacketBox(S_PacketBox.DODGE_RATE_MINUS, pc.getNdodge()));

		pc.sendPackets(new S_RuneSlot(S_RuneSlot.RUNE_CLOSE_SLOT, 3));
		pc.sendPackets(new S_RuneSlot(S_RuneSlot.RUNE_OPEN_SLOT, 1));

		pc.setEquipped(pc, true);

		// TODO -- pc.setServivalScream();

		// TODO -- checkPledgeRecommendation

		if (pc.getCurrentHp() > 0) {
			pc.setDead(false);
			pc.setStatus(0);
		} else {
			pc.setDead(true);
			pc.setStatus(ActionCodes.ACTION_Die);
		}
		if (pc.getLevel() >= 51 && pc.getLevel() - 50 > pc.getBonusStats()) {
			if ((pc.getBaseStr() + pc.getBaseDex() + pc.getBaseCon() + pc.getBaseInt() + pc.getBaseWis()
					+ pc.getBaseCha()) < 210) {
				pc.sendPackets(new S_bonusstats(pc.getId(), 1));
			}
		}
		if (Config.CHARACTER_CONFIG_IN_SERVER_SIDE) {
			pc.sendPackets(new S_CharacterConfig(pc.getId()));
		}

		searchSummon(pc);

		WarTimeController.getInstance().checkCastleWar(pc);

		if (pc.getClanid() != 0) {
			L1Clan clan = L1World.getInstance().getClan(pc.getClanname());
			if (clan != null) {
				if (pc.getClanid() == clan.getClanId()
						&& pc.getClanname().toLowerCase().equals(clan.getClanName().toLowerCase())) {
					L1PcInstance[] clanMembers = clan.getOnlineClanMember();
					for (L1PcInstance clanMember : clanMembers) {
						if (clanMember.getId() != pc.getId()) {
							clanMember.sendPackets(new S_ServerMessage(843, pc.getName()));
						}
					}
					for (L1War war : L1World.getInstance().getWarList()) {
						boolean ret = war.CheckClanInWar(pc.getClanname());
						if (ret) {
							String enemy_clan_name = war.GetEnemyClanName(pc.getClanname());
							if (enemy_clan_name != null) {
								pc.sendPackets(new S_War(8, pc.getClanname(), enemy_clan_name));
							}
							break;
						}
					}
				} else {
					pc.setClanid(0);
					pc.setClanname("");
					pc.setClanRank(0);
					pc.save();
				}
			}
		}
		if (pc.getPartnerId() != 0) {
			L1PcInstance partner = (L1PcInstance) L1World.getInstance().findObject(pc.getPartnerId());
			if (partner != null && partner.getPartnerId() != 0) {
				if (pc.getPartnerId() == partner.getId() && partner.getPartnerId() == pc.getId()) {
					pc.sendPackets(new S_ServerMessage(548));
					partner.sendPackets(new S_ServerMessage(549));
				}
			}
		}
		if (currentHpAtLoad > pc.getCurrentHp()) {
			pc.setCurrentHp(currentHpAtLoad);
		}
		if (currentMpAtLoad > pc.getCurrentMp()) {
			pc.setCurrentMp(currentMpAtLoad);
		}
		pc.startHpRegeneration();
		pc.startMpRegeneration();
		pc.startObjectAutoUpdate();
		client.CharReStart(false);
		pc.beginExpMonitor();
		pc.save();
		pc.sendPackets(new S_OwnCharStatus(pc));
		if (pc.getHellTime() > 0) {
			pc.beginHell(false);
		}
		checkUnreadMail(pc);
		updateIcons(pc);

		// TODO -- pc.startExpirationTimer();
		pc.startMapLimiter();
		pc.sendPackets(new S_PlayTime());

		if (Config.ALT_POLYEVENT)
			pc.sendPackets(new S_SystemMessage("\\fRPolymorph Event Is Currently Running. Enjoy!"));

		if (pc.getMapId() == 99 && !pc.isGm()) {
			pc.sendPackets(new S_SystemMessage("\\fRYou've logged in while jailed!"));

			JailInfo jailInfo = JailController.getInstance().getJailInfo(pc.getName());

			if (jailInfo == null) {
				pc.sendPackets(new S_SystemMessage("\\fRFor more info, contact a GameMaster."));
			} else {
				pc.sendPackets(new S_SystemMessage("\\fRReason: " + jailInfo.getMessage()));
				pc.sendPackets(new S_SystemMessage("\\fRWill be unjailed in: " + jailInfo.getReleaseTimeFormatted()));
			}
		}

		long lastReport = LogReporterTable.getLastSuspicion(pc.getId());
		long lastReportEndTime = lastReport + (Config.REPORT_TIME_MINUTES * 60000);

		if (lastReport > 0 && lastReportEndTime > System.currentTimeMillis()) {
			pc.enableLogPackets(lastReportEndTime);
		}
	}

	private void checkUnreadMail(final L1PcInstance character) {
		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet result = null;

		try {
			connection = L1DatabaseFactory.getInstance().getConnection();
			statement = connection
					.prepareStatement("SELECT COUNT(*) FROM mail WHERE inbox_id=? AND receiver=? AND read_status=?");
			statement.setInt(1, character.getId());
			statement.setString(2, character.getName());
			statement.setInt(3, 0);

			result = statement.executeQuery();
			result.next();
			int count = result.getInt(1);
			if (count > 0) {
				String message = String.format("You've got %d unread %s!", count, count == 1 ? "message" : "messages");
				character.sendPackets(new S_SystemMessage(message));
			}
		} catch (SQLException e) {
			_log.error(e.getLocalizedMessage(), e);
		} finally {
			SQLUtil.close(statement);
			SQLUtil.close(connection);
		}
	}

	// Updates the given client with all pledge icons.
	// Keeps a cache of seen account names - it's a slow leak, but seems better
	// than bombarding each client every time a character logs on.
	private void updateIcons(final L1PcInstance character) {
		if (accountsWithIcons.contains(character.getAccountName()))
			return;

		accountsWithIcons.add(character.getAccountName());

		for (L1Clan clan : L1World.getInstance().getAllClans()) {
			if (new File(String.valueOf("emblem/" + clan.getClanId())).isFile()) {
				character.sendPackets(new S_Emblem(clan.getClanId()));
			}
		}
	}

	private void items(L1PcInstance pc) {
		CharacterTable.getInstance().restoreInventory(pc);
		pc.sendPackets(new S_InvList(pc.getInventory().getItems()));
	}

	private void skills(L1PcInstance pc) {
		Connection con = null;
		PreparedStatement pstm = null;
		ResultSet rs = null;

		try {
			con = L1DatabaseFactory.getInstance().getConnection();
			pstm = con.prepareStatement("SELECT * FROM character_skills WHERE char_obj_id=?");
			pstm.setInt(1, pc.getId());
			rs = pstm.executeQuery();
			int i = 0;
			int lv1 = 0;
			int lv2 = 0;
			int lv3 = 0;
			int lv4 = 0;
			int lv5 = 0;
			int lv6 = 0;
			int lv7 = 0;
			int lv8 = 0;
			int lv9 = 0;
			int lv10 = 0;
			int lv11 = 0;
			int lv12 = 0;
			int lv13 = 0;
			int lv14 = 0;
			int lv15 = 0;
			int lv16 = 0;
			int lv17 = 0;
			int lv18 = 0;
			int lv19 = 0;
			int lv20 = 0;
			int lv21 = 0;
			int lv22 = 0;
			int lv23 = 0;
			int lv24 = 0;
			int lv25 = 0;
			int lv26 = 0;
			int lv27 = 0;
			int lv28 = 0;
			while (rs.next()) {
				int skillId = rs.getInt("skill_id");
				L1Skill l1skills = SkillTable.getInstance().findBySkillId(skillId);
				if (l1skills.getSkillLevel() == 1) {
					lv1 |= l1skills.getId();
				}
				if (l1skills.getSkillLevel() == 2) {
					lv2 |= l1skills.getId();
				}
				if (l1skills.getSkillLevel() == 3) {
					lv3 |= l1skills.getId();
				}
				if (l1skills.getSkillLevel() == 4) {
					lv4 |= l1skills.getId();
				}
				if (l1skills.getSkillLevel() == 5) {
					lv5 |= l1skills.getId();
				}
				if (l1skills.getSkillLevel() == 6) {
					lv6 |= l1skills.getId();
				}
				if (l1skills.getSkillLevel() == 7) {
					lv7 |= l1skills.getId();
				}
				if (l1skills.getSkillLevel() == 8) {
					lv8 |= l1skills.getId();
				}
				if (l1skills.getSkillLevel() == 9) {
					lv9 |= l1skills.getId();
				}
				if (l1skills.getSkillLevel() == 10) {
					lv10 |= l1skills.getId();
				}
				if (l1skills.getSkillLevel() == 11) {
					lv11 |= l1skills.getId();
				}
				if (l1skills.getSkillLevel() == 12) {
					lv12 |= l1skills.getId();
				}
				if (l1skills.getSkillLevel() == 13) {
					lv13 |= l1skills.getId();
				}
				if (l1skills.getSkillLevel() == 14) {
					lv14 |= l1skills.getId();
				}
				if (l1skills.getSkillLevel() == 15) {
					lv15 |= l1skills.getId();
				}
				if (l1skills.getSkillLevel() == 16) {
					lv16 |= l1skills.getId();
				}
				if (l1skills.getSkillLevel() == 17) {
					lv17 |= l1skills.getId();
				}
				if (l1skills.getSkillLevel() == 18) {
					lv18 |= l1skills.getId();
				}
				if (l1skills.getSkillLevel() == 19) {
					lv19 |= l1skills.getId();
				}
				if (l1skills.getSkillLevel() == 20) {
					lv20 |= l1skills.getId();
				}
				if (l1skills.getSkillLevel() == 21) {
					lv21 |= l1skills.getId();
				}
				if (l1skills.getSkillLevel() == 22) {
					lv22 |= l1skills.getId();
				}
				if (l1skills.getSkillLevel() == 23) {
					lv23 |= l1skills.getId();
				}
				if (l1skills.getSkillLevel() == 24) {
					lv24 |= l1skills.getId();
				}
				if (l1skills.getSkillLevel() == 25) {
					lv25 |= l1skills.getId();
				}
				if (l1skills.getSkillLevel() == 26) {
					lv26 |= l1skills.getId();
				}
				if (l1skills.getSkillLevel() == 27) {
					lv27 |= l1skills.getId();
				}
				if (l1skills.getSkillLevel() == 28) {
					lv28 |= l1skills.getId();
				}
				i = lv1 + lv2 + lv3 + lv4 + lv5 + lv6 + lv7 + lv8 + lv9 + lv10 + lv11 + lv12 + lv13 + lv14 + lv15 + lv16
						+ lv17 + lv18 + lv19 + lv20 + lv21 + lv22 + lv23 + lv24 + lv25 + lv26 + lv27 + lv28;
				pc.setSkillMastery(skillId);
			}
			if (i > 0) {
				pc.sendPackets(new S_AddSkill(lv1, lv2, lv3, lv4, lv5, lv6, lv7, lv8, lv9, lv10, lv11, lv12, lv13, lv14,
						lv15, lv16, lv17, lv18, lv19, lv20, lv21, lv22, lv23, lv24, lv25, lv26, lv27, lv28));
			}
		} catch (SQLException e) {
			_log.error(e.getLocalizedMessage(), e);
		} finally {
			SQLUtil.close(rs);
			SQLUtil.close(pstm);
			SQLUtil.close(con);
		}
	}

	private void searchSummon(L1PcInstance pc) {
		for (L1SummonInstance summon : L1World.getInstance().getAllSummons()) {
			if (summon.getMaster().getId() == pc.getId()) {
				summon.setMaster(pc);
				pc.addPet(summon);
				for (L1PcInstance visiblePc : L1World.getInstance().getVisiblePlayer(summon)) {
					visiblePc.sendPackets(new S_SummonPack(summon, visiblePc));
				}
			}
		}
	}

	private void buff(Client client, L1PcInstance pc) {
		Connection con = null;
		PreparedStatement pstm = null;
		ResultSet rs = null;

		try {
			con = L1DatabaseFactory.getInstance().getConnection();
			pstm = con.prepareStatement("SELECT * FROM character_buff WHERE char_obj_id=?");
			pstm.setInt(1, pc.getId());
			rs = pstm.executeQuery();
			while (rs.next()) {
				int skillid = rs.getInt("skill_id");
				int remaining_time = rs.getInt("remaining_time");
				if (skillid == SHAPE_CHANGE) {
					int poly_id = rs.getInt("poly_id");
					L1PolyMorph.doPoly(pc, poly_id, remaining_time, L1PolyMorph.MORPH_BY_LOGIN);
				} else if (skillid == STATUS_BRAVE) {
					pc.sendPackets(new S_SkillBrave(pc.getId(), 1, remaining_time));
					pc.broadcastPacket(new S_SkillBrave(pc.getId(), 1, 0));
					pc.setBraveSpeed(1);
					pc.setSkillEffect(skillid, remaining_time * 1000);
				} else if (skillid == STATUS_ELFBRAVE) {
					pc.sendPackets(new S_SkillBrave(pc.getId(), 3, remaining_time));
					pc.broadcastPacket(new S_SkillBrave(pc.getId(), 3, 0));
					pc.setBraveSpeed(1);
					pc.setSkillEffect(skillid, remaining_time * 1000);
				} else if (skillid == STATUS_HASTE) {
					pc.sendPackets(new S_SkillHaste(pc.getId(), 1, remaining_time));
					pc.broadcastPacket(new S_SkillHaste(pc.getId(), 1, 0));
					pc.setMoveSpeed(1);
					pc.setSkillEffect(skillid, remaining_time * 1000);
				} else if (skillid == STATUS_BLUE_POTION) {
					pc.sendPackets(new S_SkillIconGFX(34, remaining_time));
					pc.setSkillEffect(skillid, remaining_time * 1000);
				} else if (skillid == STATUS_CHAT_PROHIBITED) {
					pc.sendPackets(new S_SkillIconGFX(36, remaining_time));
					pc.setSkillEffect(skillid, remaining_time * 1000);
				} else if (skillid >= COOKING_1_0_N && skillid <= COOKING_1_6_N
						|| skillid >= COOKING_1_0_S && skillid <= COOKING_1_6_S
						|| skillid >= COOKING_2_0_N && skillid <= COOKING_2_6_N
						|| skillid >= COOKING_2_0_S && skillid <= COOKING_2_6_S
						|| skillid >= COOKING_3_0_N && skillid <= COOKING_3_6_N
						|| skillid >= COOKING_3_0_S && skillid <= COOKING_3_6_S) {
					L1Cooking.eatCooking(pc, skillid, remaining_time);
				} else {
					L1SkillUse l1skilluse = new L1SkillUse();
					l1skilluse.handleCommands(client.getActiveChar(), skillid, pc.getId(), pc.getX(), pc.getY(), null,
							remaining_time, L1SkillUse.TYPE_LOGIN);
				}
			}
		} catch (SQLException e) {
			_log.error(e.getLocalizedMessage(), e);
		} finally {
			SQLUtil.close(rs);
			SQLUtil.close(pstm);
			SQLUtil.close(con);
		}
	}

	private void buffBlessOfAin(L1PcInstance pc) {
		if (pc.getBlessOfAin() >= 2000000) {
			pc.setBlessOfAin(2000000);
			pc.sendPackets(new S_PacketBox(S_PacketBox.BLESS_OF_AIN, pc.getBlessOfAin()));
			return;
		}

		Timestamp logoutTime = pc.getLogoutTime();
		if (logoutTime == null) {
			logoutTime = new Timestamp(System.currentTimeMillis());
		}

		int tmp = (int) ((System.currentTimeMillis() - logoutTime.getTime()) / 900000);
		int sum = pc.getBlessOfAin() + (tmp * 10000);
		if (sum >= 2000000) {
			pc.setBlessOfAin(2000000);
		} else {
			pc.setBlessOfAin(sum);
		}

		pc.sendPackets(new S_PacketBox(S_PacketBox.BLESS_OF_AIN, pc.getBlessOfAin()));
	}

	@Override
	public String getType() {
		return C_LOGIN_TO_SERVER;
	}
}
