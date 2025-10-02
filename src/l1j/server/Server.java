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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.LogManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;
import l1j.server.server.GameServer;
import l1j.server.ssh.SSHServer;
import l1j.server.telnet.TelnetServer;

/**
 * l1j-En
 */
public class Server {
	private static Logger _log = LoggerFactory.getLogger(Server.class.getName());
	private static final String LOG_PROP = "./config/log.properties";

	public static void main(final String[] args) throws Exception {

		File logFolder = new File("log");
		logFolder.mkdir();

		try {
			InputStream is = new BufferedInputStream(new FileInputStream(LOG_PROP));
			LogManager.getLogManager().readConfiguration(is);
			is.close();
		} catch (IOException e) {
			_log.error("Failed to load " + LOG_PROP + " file.", e);
			System.exit(0);
		}
		InternalLoggerFactory.setDefaultFactory(Slf4JLoggerFactory.INSTANCE);

		try {
			Config.load();
		} catch (Exception e) {
			_log.error(e.getLocalizedMessage(), e);
			System.exit(0);
		}

		// L1DatabaseFactory
		L1DatabaseFactory.setDatabaseSettings(Config.DB_DRIVER, Config.DB_URL, Config.DB_LOGIN, Config.DB_PASSWORD);
		L1DatabaseFactory.getInstance();

		// Intialize GameServer
		GameServer.getInstance().initialize();

		// Telent Server
		if (Config.TELNET_SERVER) {
			TelnetServer.getInstance().start();
			_log.info("Telnet server initialized.");
		} else {
			_log.info("Telnet server is currently disabled.");
		}

		if (Config.SSH_SERVER) {
			SSHServer.getInstance().start();
			_log.info("SSH server initialized on port " + Config.SSH_PORT + " with "
					+ Config.SSH_ALLOWED_USERNAMES.length + " users.");
		}
	}
}