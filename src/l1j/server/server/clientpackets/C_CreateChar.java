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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l1j.server.Config;
import l1j.server.server.Account;
import l1j.server.server.BadNamesList;
import l1j.server.server.datatables.AccessLevelTable;
import l1j.server.server.datatables.CharacterTable;
import l1j.server.server.datatables.SkillTable;
import l1j.server.server.encryptions.IdFactory;
import l1j.server.server.model.Beginner;
import l1j.server.server.model.L1Attribute;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.network.Client;
import l1j.server.server.serverpackets.S_AddSkill;
import l1j.server.server.serverpackets.S_CharCreateStatus;
import l1j.server.server.serverpackets.S_NewCharPacket;
import l1j.server.server.templates.L1Skill;

// Referenced classes of package l1j.server.server.clientpackets:
// ClientBasePacket
public class C_CreateChar extends ClientBasePacket {
	private static Logger _log = LoggerFactory.getLogger(C_CreateChar.class.getName());
	private static final String C_CREATE_CHAR = "[C] C_CreateChar";

	private static final int[] MALE_LIST = new int[] { 0, 61, 138, 734, 2786, 6658, 6671 };
	private static final int[] FEMALE_LIST = new int[] { 1, 48, 37, 1186, 2796, 6661, 6650 };
	private static final int[] LOCX_LIST = new int[] { 32816, 32714, 32714, 32816, 32714, 32816, 32714 };
	private static final int[] LOCY_LIST = new int[] { 32734, 32877, 32877, 32734, 32877, 32734, 32877 };
	private static final short[] MAPID_LIST = new short[] { 68, 69, 69, 68, 69, 68, 69 };

	public C_CreateChar(byte[] abyte0, Client client) throws Exception {
		super(abyte0);
		L1PcInstance pc = new L1PcInstance();
		String name = readS();

		Account account = Account.load(client.getAccountName());
		int characterSlot = account.getCharacterSlot();
		int maxAmount = Config.DEFAULT_CHARACTER_SLOT + characterSlot;

		name = name.replaceAll("\\s", "");
		name = name.replaceAll("@", "");
		if ((name.length() == 0) || isInvalidName(name)) {
			S_CharCreateStatus s_charcreatestatus = new S_CharCreateStatus(S_CharCreateStatus.REASON_INVALID_NAME);
			client.sendPacket(s_charcreatestatus);
			return;
		}
		if (CharacterTable.doesCharNameExist(name)) {
			_log.trace("Character Name: " + pc.getName() + " already exists. Creation failed.");
			S_CharCreateStatus s_charcreatestatus1 = new S_CharCreateStatus(S_CharCreateStatus.REASON_ALREADY_EXSISTS);
			client.sendPacket(s_charcreatestatus1);
			return;
		}
		if (client.getAccount().countCharacters() >= maxAmount) {
			_log.trace("Account: " + client.getAccountName() + " attempted to create more than " + maxAmount
					+ " characters.");
			S_CharCreateStatus s_charcreatestatus1 = new S_CharCreateStatus(S_CharCreateStatus.REASON_WRONG_AMOUNT);
			client.sendPacket(s_charcreatestatus1);
			return;
		}
		pc.setName(name);
		pc.setType(readC());
		pc.set_sex(readC());
		pc.addBaseStr((byte) readC());
		pc.addBaseDex((byte) readC());
		pc.addBaseCon((byte) readC());
		pc.addBaseWis((byte) readC());
		pc.addBaseCha((byte) readC());
		pc.addBaseInt((byte) readC());
		boolean isStatusError = false;

		if (pc.get_sex() == 0) {
			pc.setClassId(MALE_LIST[pc.getType()]);
		} else {
			pc.setClassId(FEMALE_LIST[pc.getType()]);
		}

		Map<L1Attribute, Integer> startingStats = pc.getClassFeature().getFixedStats();
		int originalStr = startingStats.get(L1Attribute.Str);
		int originalDex = startingStats.get(L1Attribute.Dex);
		int originalCon = startingStats.get(L1Attribute.Con);
		int originalWis = startingStats.get(L1Attribute.Wis);
		int originalCha = startingStats.get(L1Attribute.Cha);
		int originalInt = startingStats.get(L1Attribute.Int);
		int originalAmount = pc.getClassFeature().getFloatingStats();

		if ((pc.getBaseStr() < originalStr || pc.getBaseDex() < originalDex || pc.getBaseCon() < originalCon
				|| pc.getBaseWis() < originalWis || pc.getBaseCha() < originalCha || pc.getBaseInt() < originalInt)
				|| (pc.getBaseStr() > originalStr + originalAmount || pc.getBaseDex() > originalDex + originalAmount
						|| pc.getBaseCon() > originalCon + originalAmount
						|| pc.getBaseWis() > originalWis + originalAmount
						|| pc.getBaseCha() > originalCha + originalAmount
						|| pc.getBaseInt() > originalInt + originalAmount)) {
			isStatusError = true;
		}

		Map<L1Attribute, Integer> startingMaxStats = pc.getClassFeature().getMaxFixedStats();
		int startingMaxSTR = startingMaxStats.get(L1Attribute.Str);
		int startingMaxDex = startingMaxStats.get(L1Attribute.Dex);
		int startingMaxCon = startingMaxStats.get(L1Attribute.Con);
		int startingMaxWis = startingMaxStats.get(L1Attribute.Wis);
		int startingMaxCha = startingMaxStats.get(L1Attribute.Cha);
		int startingMaxInt = startingMaxStats.get(L1Attribute.Int);

		if (pc.getBaseStr() > startingMaxSTR || pc.getBaseDex() > startingMaxDex || pc.getBaseCon() > startingMaxCon
				|| pc.getBaseWis() > startingMaxWis || pc.getBaseCha() > startingMaxCha
				|| pc.getBaseInt() > startingMaxInt) {
			isStatusError = true;
		}

		int statusAmount = pc.getDex() + pc.getCha() + pc.getCon() + pc.getInt() + pc.getStr() + pc.getWis();

		if (statusAmount != 75 || isStatusError) {
			_log.trace("Character have wrong value");
			S_CharCreateStatus s_charcreatestatus3 = new S_CharCreateStatus(S_CharCreateStatus.REASON_WRONG_AMOUNT);
			client.sendPacket(s_charcreatestatus3);
			return;
		}
		_log.trace("Character Name : " + pc.getName() + " ClassId: " + pc.getClassId());
		S_CharCreateStatus s_charcreatestatus2 = new S_CharCreateStatus(S_CharCreateStatus.REASON_OK);
		client.sendPacket(s_charcreatestatus2);
		initNewChar(client, pc);
	}

	private static void initNewChar(Client client, L1PcInstance pc) throws IOException, Exception {
		pc.setId(IdFactory.getInstance().nextId());

		pc.setBirthday();
		pc.setX(LOCX_LIST[pc.getType()]);
		pc.setY(LOCY_LIST[pc.getType()]);
		pc.setMap(MAPID_LIST[pc.getType()]);
		pc.setHeading(0);
		pc.setLawful(0);
		int initHp = pc.getClassFeature().getStartingHp();
		int initMp = pc.getClassFeature().getStartingMp(pc.getBaseWis());
		pc.addBaseMaxHp((short) initHp);
		pc.setCurrentHp((short) initHp);
		pc.addBaseMaxMp((short) initMp);
		pc.setCurrentMp((short) initMp);
		pc.resetBaseAc();
		pc.setTitle("");
		pc.setClanid(0);
		pc.setClanRank(0);
		pc.set_food(40);
		pc.setAccessLevel(AccessLevelTable.getInstance().getAccessLevel((short) client.getAccount().getAccessLevel()));
		pc.setGm(false);
		pc.setGmInvis(false);
		pc.setExp(0);
		pc.setHighLevel(0);
		pc.setStatus(0);
		pc.setClanname("");
		pc.setBonusStats(0);
		pc.setElixirStats(0);
		pc.resetBaseMr();
		pc.setElfAttr(0);
		pc.set_PKcount(0);
		pc.setPkCountForElf(0);
		pc.setExpRes(0);
		pc.setPartnerId(0);
		pc.setOnlineStatus(0);
		pc.setHomeTownId(0);
		pc.setContribution(0);
		pc.setBanned(false);
		pc.setKarma(0);
		pc.setOriginalStr(pc.getStr());
		pc.setOriginalInt(pc.getInt());
		pc.setOriginalWis(pc.getWis());
		pc.setOriginalDex(pc.getDex());
		pc.setOriginalCon(pc.getCon());
		pc.setOriginalCha(pc.getCha());
		pc.setBlessOfAin(0);

		if (pc.isWizard()) { // WIZ
			pc.sendPackets(
					new S_AddSkill(3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
			int object_id = pc.getId();
			L1Skill l1skills = SkillTable.getInstance().findBySkillId(4); // EB
			String skill_name = l1skills.getName();
			int skill_id = l1skills.getSkillId();
			SkillTable.getInstance().spellMastery(object_id, skill_id, skill_name, 0, 0);
		}
		Beginner.getInstance().GiveItem(pc);
		pc.setAccountName(client.getAccountName());
		CharacterTable.getInstance().storeNewCharacter(pc);
		S_NewCharPacket s_newcharpacket = new S_NewCharPacket(pc);
		client.sendPacket(s_newcharpacket);
		CharacterTable.saveCharStatus(pc);

		pc.refresh();
	}

	private static boolean isAlphaNumeric(String s) {
		for (char character : s.toCharArray())
			if (!Character.isLetterOrDigit(character))
				return false;
		return true;
	}

	private static boolean isInvalidName(String name) {
		int numOfNameBytes = 0;
		try {
			numOfNameBytes = name.getBytes("UTF-8").length;
		} catch (UnsupportedEncodingException e) {
			_log.error(e.getLocalizedMessage(), e);
			return false;
		}
		if (isAlphaNumeric(name) || 5 < (numOfNameBytes - name.length()) || 12 < numOfNameBytes || BadNamesList.getInstance().isBadName(name)) {
			return false;
		}
		return true;
	}

	@Override
	public String getType() {
		return C_CREATE_CHAR;
	}
}
