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

import l1j.server.Config;
import l1j.server.server.controllers.TargetLockController;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.network.Client;

public class C_TargetLock extends ClientBasePacket {

	private static final String C_TARGET_LOCK = "[C] C_TargetLock";

	public C_TargetLock(byte[] decrypt, Client client) {
		super(decrypt);

		if (!Config.ENABLE_TARGET_LOCK_ASSIST) {
			return;
		}

		L1PcInstance pc = client.getActiveChar();
		if (pc == null) {
			return;
		}

		int targetId = 0;
		if (decrypt.length >= 5) {
			targetId = readD();
		}

		TargetLockController.getInstance().handleLockRequest(pc, targetId);
	}

	@Override
	public String getType() {
		return C_TARGET_LOCK;
	}
}
