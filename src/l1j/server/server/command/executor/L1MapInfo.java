package l1j.server.server.command.executor;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l1j.server.Config;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.model.map.CachedMapReader;
import l1j.server.server.model.map.L1WorldMap;
import l1j.server.server.serverpackets.S_SystemMessage;
import l1j.server.server.utils.FileUtil;

public class L1MapInfo implements L1CommandExecutor {
	/*
	 * Map IDs 0 = ???? 15 = ???? 16 = fishing zone 21 = safety 47 = combat zone
	 *
	 */
	private static Logger _log = LoggerFactory.getLogger(L1MapInfo.class.getName());

	public static L1CommandExecutor getInstance() {
		return new L1MapInfo();
	}

	@Override
	public void execute(L1PcInstance pc, String cmdName, String arg) {
		ArrayList<String> acceptedCommands = new ArrayList<>(Arrays.asList("info", "update", "revert", "save"));

		arg = arg.toLowerCase().trim();

		if (arg.length() == 0) {
			arg = "info";
		}

		try {
			String[] args = arg.split(" ");
			String command = args[0];

			if (!acceptedCommands.contains(command)) {
				throw new Exception();
			}

			switch (command) {
			case "info":
				int currentTile = pc.getMap().getOriginalTile(pc.getX(), pc.getY());
				pc.sendPackets(new S_SystemMessage("Current Tile: " + currentTile));
				break;
			case "update":
				short newValue = Byte.parseByte(args[1]);
				pc.getMap().setOriginalTile(pc.getX(), pc.getY(), newValue);
				pc.sendPackets(new S_SystemMessage("Tile updated to: " + newValue));
				break;
                        case "save":
                                int mapId = pc.getMapId();
                                pc.sendPackets(new S_SystemMessage("Starting save.. this may take a second..."));
                                String currentMapFilename = "./maps/" + mapId + ".txt";
                                String backupMapFilename = "./maps/" + mapId + ".txt.bak";

                                FileUtil.copyFileUsingStream(new File(currentMapFilename), new File(backupMapFilename));

                                // update the values for the current map
                                FileUtil.writeFile(currentMapFilename, pc.getMap().toCsv());

                                pc.sendPackets(new S_SystemMessage("Map " + mapId + " new values saved."));
                                pc.sendPackets(new S_SystemMessage("Backup created in the maps directory as " + backupMapFilename));

                                if (Config.CACHE_MAP_FILES) {
                                        pc.sendPackets(new S_SystemMessage("Regenerating map cache for map " + mapId + "..."));
                                        try {
                                                CachedMapReader.rebuildCache(mapId);
                                                pc.sendPackets(new S_SystemMessage("Map cache regeneration complete."));
                                        } catch (Exception e) {
                                                _log.error(e.getLocalizedMessage(), e);
                                                pc.sendPackets(new S_SystemMessage("Failed to regenerate map cache: " + e.getMessage()));
                                        }
                                }

                                pc.sendPackets(new S_SystemMessage("Reloading world map " + mapId + "..."));
                                try {
                                        L1WorldMap.getInstance().reloadMap(mapId);
                                        pc.sendPackets(new S_SystemMessage("World map reload complete."));
                                } catch (Exception e) {
                                        _log.error(e.getLocalizedMessage(), e);
                                        pc.sendPackets(new S_SystemMessage("Failed to reload world map: " + e.getMessage()));
                                }
                                break;
                        }

                } catch (Exception ex) {
			_log.warn(ex.getLocalizedMessage(), ex);
			pc.sendPackets(new S_SystemMessage(".map [info|update|save] [newTileValue]"));
		}
	}
}
