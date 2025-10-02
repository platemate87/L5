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
package l1j.server.server.serverpackets;

import java.util.ArrayList;

import l1j.server.server.datatables.MailTable;
import l1j.server.server.encryptions.Opcodes;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.templates.L1Mail;

// Referenced classes of package l1j.server.server.serverpackets:
// ServerBasePacket
public class S_Mail extends ServerBasePacket {
//	private static Logger _log = LoggerFactory.getLogger(S_WhoAmount.class.getName());
	private static final String S_MAIL = "[S] S_Mail";
	private byte[] _byte = null;

	public S_Mail(L1PcInstance pc, int type) {
		ArrayList<L1Mail> mails = new ArrayList<>();
		for (L1Mail mail : MailTable.getAllMail()) {
			if (mail.getInBoxId() == pc.getId() && mail.getType() == type) {
				mails.add(mail);
			}
		}

		writeC(Opcodes.S_OPCODE_MAIL); // Sempre escreve o cabeçalho, mesmo se vazio
		writeC(type);
		writeH(mails.size()); // 0 se estiver vazio

		for (L1Mail mail : mails) {
			writeD(mail.getId());
			writeC(mail.getReadStatus());
			writeD((int) (mail.getDate().getTime() / 1000));
			writeC(mail.getSenderName().equalsIgnoreCase(pc.getName()) ? 1 : 0);
			writeS(mail.getSenderName().equalsIgnoreCase(pc.getName()) ? mail.getReceiverName() : mail.getSenderName());
			writeBytes(mail.getSubject());
		}
	}

	/**
	 * //Unable to send letter [Server] opcode = 48 0000: 30 20 00 45 54 fa 00 b5
	 */
	public S_Mail(int type, boolean isDelivered) { // Mail notification to recipients
		writeC(Opcodes.S_OPCODE_MAIL);
		writeC(type);
		writeC(isDelivered ? 1 : 0);
	}

	public S_Mail(L1PcInstance pc, int mailId, boolean isDraft) {
		MailTable.getInstance();
		L1Mail mail = MailTable.getMail(mailId);
		writeC(Opcodes.S_OPCODE_MAIL);
		writeC(0x50);
		writeD(mailId);
		writeC(isDraft ? 1 : 0);
		writeS(pc.getName());
		writeBytes(mail.getSubject());
	}

	/**
	 * //Read general letters [Server] opcode = 48 0000: [30] [10] [29 00 00 00] [32
	 * 00] 00 00 a4 cb 00 03 08 00 0.)...2.........
	 *
	 * //Mail to the safe deposit box [Server] opcode = 48 0000: [30] [40] [2b 00 00
	 * 00] [01] 95
	 *
	 */
	public S_Mail(int mailId, int type) {
		// Delete letter
		// 0x30: Delete general 0x31:Delete Blood Pledge 0x32:Generally stored in a safe
		// deposit box 0x40:Delete the safe deposit box
		if (type == 0x30 || type == 0x31 || type == 0x32 || type == 0x40) {
			writeC(Opcodes.S_OPCODE_MAIL);
			writeC(type);
			writeD(mailId);
			writeC(1);
			return;
		}
		L1Mail mail = MailTable.getMail(mailId);
		writeC(Opcodes.S_OPCODE_MAIL);
		writeC(type);
		writeD(mailId);

		if (mail != null) {
			writeBytes(mail.getContent());
		} else {
			writeC(0); // ou algo indicando que o conteúdo está ausente
		}
	}

	@Override
	public byte[] getContent() {
		if (_byte == null) {
			_byte = getBytes();
		}
		return _byte;
	}

	@Override
	public String getType() {
		return S_MAIL;
	}
}
