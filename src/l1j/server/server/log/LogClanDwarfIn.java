/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * http://www.gnu.org/copyleft/gpl.html
 */
package l1j.server.server.log;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l1j.server.L1DatabaseFactory;
import l1j.server.server.model.Instance.L1ItemInstance;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.utils.SQLUtil;

public class LogClanDwarfIn {
	private static Logger _log = LoggerFactory.getLogger(LogClanDwarfIn.class.getName());

	public void storeLogClanDwarfIn(L1PcInstance pc, L1ItemInstance item, int item_count_before, int item_count_after,
			int item_in_count) {
		Connection con = null;
		PreparedStatement pstm = null;
		try {
			con = L1DatabaseFactory.getInstance().getConnection();
			pstm = con.prepareStatement(
					"INSERT INTO logclanwarehousein VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);");
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			pstm.setString(1, formatter.format(new Date().getTime()));
			pstm.setString(2, pc.getNetConnection().getIp());
			pstm.setString(3, pc.getAccountName());
			pstm.setInt(4, pc.getId());
			pstm.setString(5, pc.getName());
			pstm.setInt(6, pc.getClanid());
			pstm.setString(7, pc.getClanname());
			pstm.setInt(8, item.getId());
			pstm.setString(9, item.getItem().getName());
			pstm.setInt(10, item.getEnchantLevel());
			pstm.setInt(11, item_count_before);
			pstm.setInt(12, item_count_after);
			int item_count_diff = item_count_before - item_count_after;
			if (item_count_diff < 0) {
				item_count_diff = -item_count_diff;
			}
			pstm.setInt(13, item_count_diff);
			pstm.setInt(14, item.getCount());
			pstm.setInt(15, item_in_count);
			int count_diff = item_in_count - item_count_diff;
			pstm.setInt(16, count_diff);
			pstm.execute();
		} catch (SQLException e) {
			_log.error(e.getLocalizedMessage(), e);
		} finally {
			SQLUtil.close(pstm);
			SQLUtil.close(con);
		}
	}
}
