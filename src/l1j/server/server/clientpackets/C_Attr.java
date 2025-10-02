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

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l1j.server.Config;
import l1j.server.server.controllers.WarTimeController;
import l1j.server.server.datatables.CharacterTable;
import l1j.server.server.datatables.ClanTable;
import l1j.server.server.datatables.HouseTable;
import l1j.server.server.datatables.NpcTable;
import l1j.server.server.datatables.PetTable;
import l1j.server.server.log.LogStatusUp;
import l1j.server.server.model.L1CastleLocation;
import l1j.server.server.model.L1Character;
import l1j.server.server.model.L1ChatParty;
import l1j.server.server.model.L1Clan;
import l1j.server.server.model.L1DeathMatch;
import l1j.server.server.model.L1Object;
import l1j.server.server.model.L1Party;
import l1j.server.server.model.L1PolyRace;
import l1j.server.server.model.L1Quest;
import l1j.server.server.model.L1Teleport;
import l1j.server.server.model.L1War;
import l1j.server.server.model.L1World;
import l1j.server.server.model.Instance.L1ItemInstance;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.model.Instance.L1PetInstance;
import l1j.server.server.model.item.L1ItemId;
import l1j.server.server.model.map.L1Map;
import l1j.server.server.network.Client;
import l1j.server.server.serverpackets.S_ChangeName;
import l1j.server.server.serverpackets.S_CharTitle;
import l1j.server.server.serverpackets.S_CharVisualUpdate;
import l1j.server.server.serverpackets.S_ClanName;
import l1j.server.server.serverpackets.S_OwnCharStatus2;
import l1j.server.server.serverpackets.S_PacketBox;
import l1j.server.server.serverpackets.S_Resurrection;
import l1j.server.server.serverpackets.S_ServerMessage;
import l1j.server.server.serverpackets.S_SkillSound;
import l1j.server.server.serverpackets.S_Trade;
import l1j.server.server.templates.L1House;
import l1j.server.server.templates.L1Npc;
import l1j.server.server.templates.L1Pet;
import l1j.server.server.utils.FaceToFace;

// Referenced classes of package l1j.server.server.clientpackets:
// ClientBasePacket
public class C_Attr extends ClientBasePacket {
	private static final Logger _log = LoggerFactory.getLogger(C_Attr.class.getName());
	private static final String C_ATTR = "[C] C_Attr";
	private static final int HEADING_TABLE_X[] = { 0, 1, 1, 1, 0, -1, -1, -1 };
	private static final int HEADING_TABLE_Y[] = { -1, -1, 0, 1, 1, 1, 0, -1 };

	public C_Attr(byte abyte0[], Client client) throws Exception {
		super(abyte0);
		int i = readH();
		int c;
		String name;
		int attrcode;

		if (i == 479) {
			attrcode = i;
		} else {
			readD();
			attrcode = readH();
		}

		L1PcInstance pc = client.getActiveChar();

		switch (attrcode) {
		case 97:
			c = readC();
			L1PcInstance joinPc = (L1PcInstance) L1World.getInstance().findObject(pc.getTempID());
			pc.setTempID(0);

			if (joinPc != null) {
				if (c == 0) { // No
					joinPc.sendPackets(new S_ServerMessage(96, pc.getName()));
				} else if (c == 1) { // Yes
					int clan_id = pc.getClanid();
					String clanName = pc.getClanname();
					L1Clan clan = L1World.getInstance().getClan(clanName);
					if (clan != null) {
						int maxMember = 0;
						int charisma = pc.getCha();
						boolean lv45quest = false;

						if (pc.getQuest().isEnd(L1Quest.QUEST_LEVEL45)) {
							lv45quest = true;
						}

						if (pc.getLevel() >= 50) { // Lv50
							if (lv45quest) { // Lv45
								maxMember = charisma * 9;
							} else {
								maxMember = charisma * 3;
							}
						} else { // Lv50
							if (lv45quest) { // Lv45
								maxMember = charisma * 6;
							} else {
								maxMember = charisma * 2;
							}
						}

						if (Config.MAX_CLAN_MEMBER > 0) {
							maxMember = Config.MAX_CLAN_MEMBER;
						}

						if (joinPc.getClanid() == 0) {
							String clanMembersName[] = clan.getAllMembers();

							if (maxMember <= clanMembersName.length) {
								joinPc.sendPackets(new S_ServerMessage(188, pc.getName()));
								return;
							}

							for (L1PcInstance clanMembers : clan.getOnlineClanMember()) {
								clanMembers.sendPackets(new S_ServerMessage(94, joinPc.getName()));
							}

							joinPc.setClanid(clan_id);
							joinPc.setClanname(clanName);
							joinPc.setClanRank(L1Clan.CLAN_RANK_PROBATION);
							joinPc.setTitle("");
							joinPc.sendPackets(new S_CharTitle(joinPc.getId(), ""));
							joinPc.broadcastPacket(new S_CharTitle(joinPc.getId(), ""));
							joinPc.setLastJoinedPledge();
							joinPc.sendPackets(new S_ClanName(joinPc, clan.getClanId(), joinPc.getClanRank()));
							joinPc.save();
							clan.addMemberName(joinPc.getName());
							joinPc.sendPackets(new S_ServerMessage(95, clanName));

							// seems to be required to allow you to leave a clan right after you join
							joinPc.sendPackets(
									new S_PacketBox(S_PacketBox.MSG_RANK_CHANGED, L1Clan.CLAN_RANK_PROBATION));
						} else {
							if (Config.CLAN_ALLIANCE) {
								changeClan(client, pc, joinPc, maxMember);
							} else {
								joinPc.sendPackets(new S_ServerMessage(89));
							}
						}
					}
				}
			}
			break;
		case 217:
		case 221:
		case 222:
			c = readC();
			L1PcInstance enemyLeader = (L1PcInstance) L1World.getInstance().findObject(pc.getTempID());
			if (enemyLeader == null) {
				return;
			}
			pc.setTempID(0);
			String clanName = pc.getClanname();
			String enemyClanName = enemyLeader.getClanname();

			if (c == 0) { // No
				if (i == 217) {
					enemyLeader.sendPackets(new S_ServerMessage(236, clanName));
				} else if (i == 221 || i == 222) {
					enemyLeader.sendPackets(new S_ServerMessage(237, clanName));
				}
			} else if (c == 1) { // Yes
				if (i == 217) {
					L1War war = new L1War();
					war.handleCommands(2, enemyClanName, clanName);
				} else if (i == 221 || i == 222) {
					for (L1War war : L1World.getInstance().getWarList()) {
						if (war.CheckClanInWar(clanName)) {
							if (i == 221) {
								war.SurrenderWar(enemyClanName, clanName);
							} else if (i == 222) {
								war.CeaseWar(enemyClanName, clanName);
							}
							break;
						}
					}
				}
			}
			break;
		case 252:
			c = readC();
			L1PcInstance trading_partner = (L1PcInstance) L1World.getInstance().findObject(pc.getTradeID());

			if (trading_partner != null) {
				L1PcInstance facingPlayer = FaceToFace.faceToFace(pc);

				if (facingPlayer == null || trading_partner.getId() != facingPlayer.getId()) {
					trading_partner.sendPackets(new S_ServerMessage(253, pc.getName()));
					pc.setTradeID(0);
					trading_partner.setTradeID(0);
					break;
				}

				if (c == 0) // No
				{
					trading_partner.sendPackets(new S_ServerMessage(253, pc.getName()));
					pc.setTradeID(0);
					trading_partner.setTradeID(0);
				} else if (c == 1) // Yes
				{
					pc.sendPackets(new S_Trade(trading_partner.getName()));
					trading_partner.sendPackets(new S_Trade(pc.getName()));
				}
			}
			break;
		case 321:
			c = readC();
			L1PcInstance resusepc1 = (L1PcInstance) L1World.getInstance().findObject(pc.getTempID());
			pc.setTempID(0);
			if (resusepc1 != null) {
				if (c == 0) { // No

				} else if (c == 1) { // Yes
					pc.sendPackets(new S_SkillSound(pc.getId(), '\346'));
					pc.broadcastPacket(new S_SkillSound(pc.getId(), '\346'));
					// pc.resurrect(pc.getLevel());
					// pc.setCurrentHp(pc.getLevel());
					pc.resurrect(pc.getMaxHp() / 2);
					pc.setCurrentHp(pc.getMaxHp() / 2);
					pc.startHpRegeneration();
					pc.startMpRegeneration();
					pc.startMpRegenerationByDoll();
					pc.stopPcDeleteTimer();
					pc.sendPackets(new S_Resurrection(pc, resusepc1, 0));
					pc.broadcastPacket(new S_Resurrection(pc, resusepc1, 0));
					pc.sendPackets(new S_CharVisualUpdate(pc));
					pc.broadcastPacket(new S_CharVisualUpdate(pc));
				}
			}
			break;
		case 322:
			c = readC();
			L1PcInstance resusepc2 = (L1PcInstance) L1World.getInstance().findObject(pc.getTempID());
			pc.setTempID(0);
			if (resusepc2 != null) {
				if (c == 0) { // No

				} else if (c == 1) { // Yes
					pc.sendPackets(new S_SkillSound(pc.getId(), '\346'));
					pc.broadcastPacket(new S_SkillSound(pc.getId(), '\346'));
					pc.resurrect(pc.getMaxHp());
					pc.setCurrentHp(pc.getMaxHp());
					pc.startHpRegeneration();
					pc.startMpRegeneration();
					pc.startMpRegenerationByDoll();
					pc.stopPcDeleteTimer();
					pc.sendPackets(new S_Resurrection(pc, resusepc2, 0));
					pc.broadcastPacket(new S_Resurrection(pc, resusepc2, 0));
					pc.sendPackets(new S_CharVisualUpdate(pc));
					pc.broadcastPacket(new S_CharVisualUpdate(pc));

					if (pc.getExpRes() == 1 && pc.isGres() && pc.isGresValid()) {
						pc.resExp();
						pc.setExpRes(0);
						pc.setGres(false);
					}
				}
			}
			break;
		case 325:
			c = readC();
			name = readS();
			L1PetInstance pet = (L1PetInstance) L1World.getInstance().findObject(pc.getTempID());
			pc.setTempID(0);
			renamePet(pet, name);
			break;
		case 512:
			c = readC();
			name = readS();
			int houseId = pc.getTempID();
			pc.setTempID(0);
			if (name.length() <= 16) {
				L1House house = HouseTable.getInstance().getHouseTable(houseId);
				house.setHouseName(name);
				HouseTable.getInstance().updateHouse(house);
			} else {
				pc.sendPackets(new S_ServerMessage(513));
			}
			break;
		case 630:
			c = readC();
			L1PcInstance fightPc = (L1PcInstance) L1World.getInstance().findObject(pc.getFightId());
			if (c == 0) {
				pc.setFightId(0);
				fightPc.setFightId(0);
				fightPc.sendPackets(new S_ServerMessage(631, pc.getName()));
			} else if (c == 1) {
				fightPc.sendPackets(new S_PacketBox(S_PacketBox.MSG_DUEL, fightPc.getFightId(), fightPc.getId()));
				pc.sendPackets(new S_PacketBox(S_PacketBox.MSG_DUEL, pc.getFightId(), pc.getId()));
			}
			break;
		case 653:
			c = readC();
			L1PcInstance target653 = (L1PcInstance) L1World.getInstance().findObject(pc.getPartnerId());
			if (c == 0) { // No
				return;
			} else if (c == 1) { // Yes
				if (target653 != null) {
					target653.setPartnerId(0);
					target653.save();
					target653.sendPackets(new S_ServerMessage(662));
				} else {
					CharacterTable.updatePartnerId(pc.getPartnerId());
				}
			}
			pc.setPartnerId(0);
			pc.save();
			pc.sendPackets(new S_ServerMessage(662));
			break;
		case 654:
			c = readC();
			L1PcInstance partner = (L1PcInstance) L1World.getInstance().findObject(pc.getTempID());
			pc.setTempID(0);
			if (partner != null) {
				if (c == 0) { // No
					partner.sendPackets(new S_ServerMessage(656, pc.getName()));
				} else if (c == 1) { // Yes
					pc.setPartnerId(partner.getId());
					pc.save();
					pc.sendPackets(new S_ServerMessage(790));
					pc.sendPackets(new S_ServerMessage(655, partner.getName()));
					partner.setPartnerId(pc.getId());
					partner.save();
					partner.sendPackets(new S_ServerMessage(790));
					partner.sendPackets(new S_ServerMessage(655, pc.getName()));
				}
			}
			break;
		case 729:
			c = readC();
			if (c == 0) { // No

			} else if (c == 1) { // Yes
				callClan(pc);
			}
			break;
		case 738:
			c = readC();
			if (c == 0) { // No

			} else if (c == 1 && pc.getExpRes() == 1) { // Yes
				int cost = 0;
				int level = pc.getLevel();
				int lawful = pc.getLawful();
				if (level < 45) {
					cost = level * level * 100;
				} else {
					cost = level * level * 200;
				}
				if (lawful >= 0) {
					cost = (cost / 2);
				}
				if (pc.getInventory().consumeItem(L1ItemId.ADENA, cost)) {
					pc.resExp();
					pc.setExpRes(0);
				} else {
					pc.sendPackets(new S_ServerMessage(189));
				}
			}
			break;
		case 951:
			c = readC();
			L1PcInstance chatPc = (L1PcInstance) L1World.getInstance().findObject(pc.getPartyID());
			if (chatPc != null) {
				if (c == 0) { // No
					chatPc.sendPackets(new S_ServerMessage(423, pc.getName()));
					pc.setPartyID(0);
				} else if (c == 1) { // Yes
					if (chatPc.isInChatParty()) {
						if (chatPc.getChatParty().isVacancy() || chatPc.isGm()) {
							chatPc.getChatParty().addMember(pc);
						} else {
							chatPc.sendPackets(new S_ServerMessage(417));
						}
					} else {
						L1ChatParty chatParty = new L1ChatParty();
						chatParty.addMember(chatPc);
						chatParty.addMember(pc);
						chatPc.sendPackets(new S_ServerMessage(424, pc.getName()));
					}
				}
			}
			break;
		case 953:
			c = readC();
			L1PcInstance target = (L1PcInstance) L1World.getInstance().findObject(pc.getPartyID());
			if (target != null) {
				if (c == 0) // No
				{
					target.sendPackets(new S_ServerMessage(423, pc.getName()));
					pc.setPartyID(0);
				} else if (c == 1) // Yes
				{
					if (target.isInParty()) {
						if (target.getParty().isVacancy() || target.isGm()) {
							target.getParty().addMember(pc);
						} else {
							target.sendPackets(new S_ServerMessage(417));
						}
					} else {
						L1Party party = new L1Party();
						party.addMember(target);
						party.addMember(pc);
						target.sendPackets(new S_ServerMessage(424, pc.getName()));
					}
				}
			}
			break;
		case 479:
			if (readC() == 1) {
				String s = readS();
				if (!(pc.getLevel() - 50 > pc.getBonusStats())) {
					return;
				}
				if (s.toLowerCase().equals("str".toLowerCase())) {
					if (pc.getBaseStr() < 35) {
						pc.addBaseStr((byte) 1);
						pc.setBonusStats(pc.getBonusStats() + 1);
						pc.sendPackets(new S_OwnCharStatus2(pc));
						pc.sendPackets(new S_CharVisualUpdate(pc));
						pc.save();
					} else {
						pc.sendPackets(new S_ServerMessage(481));
					}
				} else if (s.toLowerCase().equals("dex".toLowerCase())) {
					if (pc.getBaseDex() < 35) {
						pc.addBaseDex((byte) 1);
						pc.resetBaseAc();
						pc.setBonusStats(pc.getBonusStats() + 1);
						pc.sendPackets(new S_OwnCharStatus2(pc));
						pc.sendPackets(new S_CharVisualUpdate(pc));
						pc.save();
					} else {
						pc.sendPackets(new S_ServerMessage(481));
					}
				} else if (s.toLowerCase().equals("con".toLowerCase())) {
					if (pc.getBaseCon() < 35) {
						pc.addBaseCon((byte) 1);
						pc.setBonusStats(pc.getBonusStats() + 1);
						pc.sendPackets(new S_OwnCharStatus2(pc));
						pc.sendPackets(new S_CharVisualUpdate(pc));
						pc.save();
					} else {
						pc.sendPackets(new S_ServerMessage(481));
					}
				} else if (s.toLowerCase().equals("int".toLowerCase())) {
					if (pc.getBaseInt() < 35) {
						pc.addBaseInt((byte) 1);
						pc.setBonusStats(pc.getBonusStats() + 1);
						pc.sendPackets(new S_OwnCharStatus2(pc));
						pc.sendPackets(new S_CharVisualUpdate(pc));
						pc.save();
					} else {
						pc.sendPackets(new S_ServerMessage(481));
					}
				} else if (s.toLowerCase().equals("wis".toLowerCase())) {
					if (pc.getBaseWis() < 35) {
						pc.addBaseWis((byte) 1);
						pc.resetBaseMr();
						pc.setBonusStats(pc.getBonusStats() + 1);
						pc.sendPackets(new S_OwnCharStatus2(pc));
						pc.sendPackets(new S_CharVisualUpdate(pc));
						pc.save();
					} else {
						pc.sendPackets(new S_ServerMessage(481));
					}
				} else if (s.toLowerCase().equals("cha".toLowerCase())) {
					if (pc.getBaseCha() < 35) {
						pc.addBaseCha((byte) 1);
						pc.setBonusStats(pc.getBonusStats() + 1);
						pc.sendPackets(new S_OwnCharStatus2(pc));
						pc.sendPackets(new S_CharVisualUpdate(pc));
						pc.save();
					} else {
						pc.sendPackets(new S_ServerMessage(481));
					}
				}
				int str = pc.getBaseStr();
				int dex = pc.getBaseDex();
				int con = pc.getBaseCon();
				int Int = pc.getBaseInt();
				int wis = pc.getBaseWis();
				int cha = pc.getBaseCha();
				LogStatusUp lsu = new LogStatusUp();
				lsu.storeLogStatusUp(pc, str, dex, con, Int, wis, cha);
			}
			break;
		case 1256:
			L1PolyRace.getInstance().requsetAttr(pc, readC());
			break;
		case 1268:
			L1DeathMatch.getInstance().requsetAttr(pc, readC());
			break;
		default:
			break;
		}
	}

	private void changeClan(Client client, L1PcInstance pc, L1PcInstance joinPc, int maxMember) {
		int clanId = pc.getClanid();
		String clanName = pc.getClanname();
		L1Clan clan = L1World.getInstance().getClan(clanName);
		String clanMemberName[] = clan.getAllMembers();

		int clanNum = clanMemberName.length;
		int oldClanId = joinPc.getClanid();

		String oldClanName = joinPc.getClanname();
		L1Clan oldClan = L1World.getInstance().getClan(oldClanName);
		String oldClanMemberName[] = oldClan.getAllMembers();

		int oldClanNum = oldClanMemberName.length;

		if (clan != null && oldClan != null && joinPc.isCrown() && joinPc.getId() == oldClan.getLeaderId()) {
			if (maxMember < clanNum + oldClanNum) {
				joinPc.sendPackets(new S_ServerMessage(188, pc.getName()));
				return;
			}

			L1PcInstance clanMember[] = clan.getOnlineClanMember();

			for (L1PcInstance element : clanMember) {
				element.sendPackets(new S_ServerMessage(94, joinPc.getName()));
			}

			for (String element : oldClanMemberName) {
				L1PcInstance oldClanMember = L1World.getInstance().getPlayer(element);
				if (oldClanMember != null) {
					oldClanMember.setClanid(clanId);
					oldClanMember.setClanname(clanName);
					if (oldClanMember.getId() == joinPc.getId()) {
						oldClanMember.setClanRank(L1Clan.CLAN_RANK_GUARDIAN);
					} else {
						oldClanMember.setClanRank(L1Clan.CLAN_RANK_PROBATION);
					}
					try {
						oldClanMember.save();
					} catch (Exception e) {
						_log.error(e.getLocalizedMessage(), e);
					}
					clan.addMemberName(oldClanMember.getName());
					oldClanMember.sendPackets(new S_ServerMessage(95, clanName));
				} else {
					try {
						L1PcInstance offClanMember = CharacterTable.getInstance()
								.restoreCharacter(element);
						offClanMember.setClanid(clanId);
						offClanMember.setClanname(clanName);
						offClanMember.setClanRank(L1Clan.CLAN_RANK_PROBATION);
						offClanMember.save();
						clan.addMemberName(offClanMember.getName());
					} catch (Exception e) {
						_log.error(e.getLocalizedMessage(), e);
					}
				}
			}
			String emblem_file = String.valueOf(oldClanId);
			File file = new File("emblem/" + emblem_file);
			file.delete();
			ClanTable.getInstance().deleteClan(oldClanName);
		}
	}

	private static void renamePet(L1PetInstance pet, String name) {
		if (pet == null || name == null) {
			throw new NullPointerException();
		}

		int petItemObjId = pet.getItemObjId();
		L1Pet petTemplate = PetTable.getInstance().getTemplate(petItemObjId);

		if (petTemplate == null) {
			throw new NullPointerException();
		}

		L1PcInstance pc = (L1PcInstance) pet.getMaster();

		if (PetTable.isNameExists(name)) {
			pc.sendPackets(new S_ServerMessage(327));
			return;
		}
		L1Npc l1npc = NpcTable.getInstance().getTemplate(pet.getNpcId());

		if (!(pet.getName().equalsIgnoreCase(l1npc.get_name()))) {
			pc.sendPackets(new S_ServerMessage(326));
			return;
		}
		pet.setName(name);
		petTemplate.set_name(name);
		PetTable.getInstance().storePet(petTemplate);
		L1ItemInstance item = pc.getInventory().getItem(pet.getItemObjId());
		pc.getInventory().updateItem(item);
		pc.sendPackets(new S_ChangeName(pet.getId(), name));
		pc.broadcastPacket(new S_ChangeName(pet.getId(), name));
	}

	private void callClan(L1PcInstance pc) {
		L1PcInstance callClanPc = (L1PcInstance) L1World.getInstance().findObject(pc.getTempID());
		pc.setTempID(0);

		if (callClanPc == null) {
			return;
		}

		if (!pc.getMap().isEscapable() && !pc.isGm()) {
			pc.sendPackets(new S_ServerMessage(647));
			L1Teleport.teleport(pc, pc.getLocation(), pc.getHeading(), false);
			return;
		}

		if (pc.getId() != callClanPc.getCallClanId()) {
			return;
		}

		boolean isInWarArea = false;
		int castleId = L1CastleLocation.getCastleIdByArea(callClanPc);
		if (castleId != 0) {
			isInWarArea = true;
			if (!WarTimeController.getInstance().isNowWar(castleId)) {
				isInWarArea = false;
			}
		}
		short mapId = callClanPc.getMapId();
		if (mapId != 0 && mapId != 4 && mapId != 304 || isInWarArea) {
			pc.sendPackets(new S_ServerMessage(547));
			return;
		}

		L1Map map = callClanPc.getMap();
		int locX = callClanPc.getX();
		int locY = callClanPc.getY();
		int heading = callClanPc.getCallClanHeading();
		locX += HEADING_TABLE_X[heading];
		locY += HEADING_TABLE_Y[heading];
		heading = (heading + 4) % 4;

		boolean isExsistCharacter = false;
		for (L1Object object : L1World.getInstance().getVisibleObjects(callClanPc, 1)) {
			if (object instanceof L1Character) {
				L1Character cha = (L1Character) object;
				if (cha.getX() == locX && cha.getY() == locY && cha.getMapId() == mapId) {
					isExsistCharacter = true;
					break;
				}
			}
		}

		if (locX == 0 && locY == 0 || !map.isPassable(locX, locY) || isExsistCharacter) {
			pc.sendPackets(new S_ServerMessage(627));
			return;
		}
		L1Teleport.teleport(pc, locX, locY, mapId, heading, true, L1Teleport.CALL_CLAN);
	}

	@Override
	public String getType() {
		return C_ATTR;
	}
}
