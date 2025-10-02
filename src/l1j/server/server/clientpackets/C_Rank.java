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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l1j.server.server.datatables.CharacterTable;
import l1j.server.server.model.L1Clan;
import l1j.server.server.model.L1Quest;
import l1j.server.server.model.L1World;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.network.Client;
import l1j.server.server.serverpackets.S_PacketBox;
import l1j.server.server.serverpackets.S_ServerMessage;
import l1j.server.server.serverpackets.S_SystemMessage;

// Referenced classes of package l1j.server.server.clientpackets:
// ClientBasePacket
public class C_Rank extends ClientBasePacket {

	private static final String C_RANK = "[C] C_Rank";
	private static Logger _log = LoggerFactory.getLogger(C_Rank.class.getName());

	public C_Rank(byte abyte0[], Client client) throws Exception {
		super(abyte0);

		int data = readC();
		int rank = readC();

		L1PcInstance pc = client.getActiveChar();

		switch (data) {
		case 1:
			String pledgeMember = readS();
			pledgeRank(pc, rank, pledgeMember);
			break;
		case 9:
			pc.sendPackets(new S_PacketBox(S_PacketBox.DISPLAY_MAP_TIME, pc.getEnterTime(53), // Giran Prison
					pc.getEnterTime(78), // Ivory Tower
					pc.getEnterTime(451), // Lastabad
					pc.getEnterTime(30))); // Dragon Valley
			break;
		}

	}

	private static void pledgeRank(L1PcInstance royal, int rank, String player) throws Exception {
		L1PcInstance targetPc = L1World.getInstance().getPlayer(player);

		if (royal == null) {
			return;
		}

		L1Clan clan = L1World.getInstance().getClan(royal.getClanname());
		if (clan == null) {
			return;
		}

		if (rank < 1 && 3 < rank) {
			royal.sendPackets(new S_ServerMessage(781));
			return;
		}

		if (royal.isCrown()) {
			if (royal.getId() != clan.getLeaderId()) {
				royal.sendPackets(new S_ServerMessage(785));
				return;
			}
		} else {
			royal.sendPackets(new S_ServerMessage(518));
			return;
		}

		if (targetPc != null) {
			if (royal.getClanid() == targetPc.getClanid()) {
				try {
					if (!royal.getQuest().isEnd(L1Quest.QUEST_LEVEL45)) {
						royal.sendPackets(new S_SystemMessage("You have not completed the level 45 quest!"));
						return;
					}

					if (royal == targetPc) {
						royal.sendPackets(new S_SystemMessage("You cannot change your own rank."));
						return;
					}

					targetPc.setClanRank(rank);
					targetPc.save();
					String rankString = "$772";
					if (rank == L1Clan.CLAN_RANK_PROBATION) {
						rankString = "$773";
					} else if (rank == L1Clan.CLAN_RANK_PUBLIC) {
						rankString = "$774";
					} else if (rank == L1Clan.CLAN_RANK_GUARDIAN) {
						rankString = "$772";
					}

					targetPc.sendPackets(new S_PacketBox(S_PacketBox.MSG_RANK_CHANGED, rank));
					royal.sendPackets(
							new S_SystemMessage(String.format("You have changed %s's rank to:", targetPc.getName())));
					royal.sendPackets(new S_ServerMessage(Integer.parseInt(rankString.replace("$", ""))));
				} catch (Exception e) {
					_log.error(e.getLocalizedMessage(), e);
				}
			} else {
				royal.sendPackets(new S_ServerMessage(414));
				return;
			}
		} else {
			L1PcInstance restorePc = CharacterTable.getInstance().restoreCharacter(player);
			if (restorePc != null && restorePc.getClanid() == royal.getClanid()) {
				try {
					restorePc.setClanRank(rank);
					restorePc.save();
				} catch (Exception e) {
					_log.error(e.getLocalizedMessage(), e);
				}
			} else {
				royal.sendPackets(new S_ServerMessage(109, player));
				return;
			}
		}
	}

	@Override
	public String getType() {
		return C_RANK;
	}
}