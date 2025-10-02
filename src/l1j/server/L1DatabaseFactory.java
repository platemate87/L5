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
package l1j.server;

import java.sql.Connection;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jolbox.bonecp.BoneCPDataSource;

public class L1DatabaseFactory {
	BoneCPDataSource _source;

	private static L1DatabaseFactory _instance;
	// private ComboPooledDataSource _source;
	private static Logger _log = LoggerFactory.getLogger(L1DatabaseFactory.class);
	private static String _driver;
	private static String _url;
	private static String _user;
	private static String _password;

	public static void setDatabaseSettings(final String driver, final String url, final String user,
			final String password) {
		_driver = driver;
		_url = url;
		_user = user;
		_password = password;
	}

	public L1DatabaseFactory() throws SQLException {
		try {
			// DatabaseFactory
			_source = new BoneCPDataSource();
			_source.setDriverClass(_driver);
			_source.setJdbcUrl(_url);
			_source.setUser(_user);
			_source.setPassword(_password);
			_source.setPartitionCount(3);
			// The following is useful for debugging only
			// this seems to cause a thread leak otherwise
			// _source.setCloseConnectionWatch(true);
			// _source.setLogStatementsEnabled(true);
			_source.setIdleConnectionTestPeriodInSeconds(60);
			// This should keep idle connections from timing out
			_source.setConnectionTestStatement("/* ping */ SELECT 1");
			_source.setTransactionRecoveryEnabled(true);
			// Disable bugged BoneCP 0.8.0 unclosed connection tracking
			_source.setDisableConnectionTracking(true);
			/* Test the connection */
			// _source.getConnection().close();
		} catch (Exception e) {
			_log.error("Database Connection FAILED");
			throw new SQLException("could not init DB connection:" + e);
		}
	}

	public void shutdown() {
		try {
			_source.close();
		} catch (Exception e) {
			_log.error("", e);
		}
		try {
			_source = null;
		} catch (Exception e) {
			_log.error("", e);
		}
	}

	public static L1DatabaseFactory getInstance() throws SQLException {
		if (_instance == null) {
			_instance = new L1DatabaseFactory();
		}
		return _instance;
	}

	public Connection getConnection() {
		Connection con = null;
		while (con == null) {
			try {
				con = _source.getConnection();
			} catch (SQLException e) {
				_log.warn("L1DatabaseFactory: getConnection() failed, trying again " + e);
			}
		}
		return con;
	}
}
