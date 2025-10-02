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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l1j.server.L1DatabaseFactory;
import l1j.server.server.model.Instance.L1ItemInstance;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.model.item.L1ItemId;
import l1j.server.server.utils.SQLUtil;

public class LogPrivateShopBuy {
	private static Logger _log = LoggerFactory.getLogger(LogPrivateShopBuy.class.getName());

	public long storeLogPrivateShopBuy(String transactionId, L1PcInstance pc, L1PcInstance target, L1ItemInstance item,
			int itembefore, int buycount) {
		Connection con = null;
		PreparedStatement pstm = null;
		long generatedKey = -1;

		try {
			con = L1DatabaseFactory.getInstance().getConnection();
			pstm = con.prepareStatement(
					"INSERT INTO logprivateshopbuy (Time, Ip, Account, CharId, CharName, TargetIp, TargetAccount, TargetCharId, "
							+ "TargetCharName, ObjectId, ItemName, EnchantLevel, ItemCount, ItemBefore, BuyCount, Transaction_Id) "
							+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);",
					java.sql.Statement.RETURN_GENERATED_KEYS);
			Date time = new Date();
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			String fm = formatter.format(time.getTime());
			pstm.setString(1, fm);
			pstm.setString(2, pc.getNetConnection().getIp());
			pstm.setString(3, pc.getAccountName());
			pstm.setInt(4, pc.getId());
			pstm.setString(5, pc.getName());
			pstm.setString(6, target.getNetConnection().getIp());
			pstm.setString(7, target.getAccountName());
			pstm.setInt(8, target.getId());
			pstm.setString(9, target.getName());
			pstm.setInt(10, item.getId());
			pstm.setString(11, item.getItem().getName());
			pstm.setInt(12, item.getEnchantLevel());
			pstm.setInt(13, item.getItemId() == L1ItemId.ADENA ? -1 : item.getCount()); // if we are logging the adena,
																						// just put -1
			pstm.setInt(14, itembefore);
			pstm.setInt(15, buycount);
			pstm.setString(16, transactionId);
			pstm.execute();

			ResultSet generatedKeys = pstm.getGeneratedKeys();
			generatedKeys.next();

			generatedKey = generatedKeys.getLong(1);
		} catch (SQLException e) {
			_log.error(e.getLocalizedMessage(), e);
		} finally {
			SQLUtil.close(pstm);
			SQLUtil.close(con);
		}

		if (generatedKey == -1) {
			_log.warn(String.format(
					"Failed to insert log for private shop buy! TransactionId: %s Buyer: %s, Shop: %s, "
							+ "Item: %s, ItemId: %d, EnchantLevel: %d, Count: %d",
					transactionId, pc.getName(), target.getName(), item.getName(), item.getId(), item.getEnchantLevel(),
					buycount));
		}

		return generatedKey;
	}

	public void completeTransaction(long id, int item_before_count, int item_after_count) {
		Connection con = null;
		PreparedStatement pstm = null;

		try {
			con = L1DatabaseFactory.getInstance().getConnection();
			pstm = con.prepareStatement(
					"UPDATE LogPrivateShopBuy SET `Completed` = '1', `ItemAfter` = ?, `ItemDiff` = ? WHERE Id = ?");

			pstm.setInt(1, item_after_count);
			int itemdiff = item_before_count - item_after_count;
			if (itemdiff < 0) {
				itemdiff = -itemdiff;
			}

			pstm.setInt(2, itemdiff);
			pstm.setLong(3, id);

			pstm.execute();
		} catch (SQLException e) {
			_log.error(e.getLocalizedMessage(), e);
		} finally {
			SQLUtil.close(pstm);
			SQLUtil.close(con);
		}
	}
}
