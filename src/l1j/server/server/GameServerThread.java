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
package l1j.server.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l1j.server.Config;
import l1j.server.server.controllers.AuctionTimeController;
import l1j.server.server.controllers.BossEventController;
import l1j.server.server.controllers.CrackOfTimeController;
import l1j.server.server.controllers.FishingTimeController;
import l1j.server.server.controllers.HomeTownTimeController;
import l1j.server.server.controllers.HouseTaxTimeController;
import l1j.server.server.controllers.JailController;
import l1j.server.server.controllers.LightTimeController;
import l1j.server.server.controllers.LoginController;
import l1j.server.server.controllers.MapTimeController;
import l1j.server.server.controllers.NpcChatTimeController;
import l1j.server.server.controllers.RankingsController;
import l1j.server.server.controllers.UbTimeController;
import l1j.server.server.controllers.WarTimeController;
import l1j.server.server.datatables.AccessLevelTable;
import l1j.server.server.datatables.CastleTable;
import l1j.server.server.datatables.CharacterTable;
import l1j.server.server.datatables.ChatLogTable;
import l1j.server.server.datatables.ClanTable;
import l1j.server.server.datatables.DoorTable;
import l1j.server.server.datatables.DropItemTable;
import l1j.server.server.datatables.DropTable;
import l1j.server.server.datatables.FurnitureSpawnTable;
import l1j.server.server.datatables.GetBackRestartTable;
import l1j.server.server.datatables.GetBackTable;
import l1j.server.server.datatables.IpTable;
import l1j.server.server.datatables.ItemTable;
import l1j.server.server.datatables.LightSpawnTable;
import l1j.server.server.datatables.MailTable;
import l1j.server.server.datatables.MapsTable;
import l1j.server.server.datatables.MobGroupTable;
import l1j.server.server.datatables.NPCTalkDataTable;
import l1j.server.server.datatables.NpcActionTable;
import l1j.server.server.datatables.NpcChatTable;
import l1j.server.server.datatables.NpcSpawnTable;
import l1j.server.server.datatables.NpcTable;
import l1j.server.server.datatables.PetTable;
import l1j.server.server.datatables.PetTypeTable;
import l1j.server.server.datatables.PolyTable;
import l1j.server.server.datatables.ResolventTable;
import l1j.server.server.datatables.ShopTable;
import l1j.server.server.datatables.SkillTable;
import l1j.server.server.datatables.SpawnTable;
import l1j.server.server.datatables.SprTable;
import l1j.server.server.datatables.UBSpawnTable;
import l1j.server.server.datatables.WeaponSkillTable;
import l1j.server.server.encryptions.IdFactory;
import l1j.server.server.model.Dungeon;
import l1j.server.server.model.ElementalStoneGenerator;
import l1j.server.server.model.Getback;
import l1j.server.server.model.L1BossCycle;
import l1j.server.server.model.L1CastleLocation;
import l1j.server.server.model.L1DeleteItemOnGround;
import l1j.server.server.model.L1World;
import l1j.server.server.model.gametime.L1GameTimeClock;
import l1j.server.server.model.item.L1TreasureBox;
import l1j.server.server.model.map.L1MapLimiter;
import l1j.server.server.model.map.L1WorldMap;
import l1j.server.server.model.trap.L1WorldTraps;

public class GameServerThread {

	private static Logger _log = LoggerFactory.getLogger(GameServerThread.class.getName());

	private static GameServerThread _instance;
	private LoginController _loginController;
	private int chatlvl;

	public static GameServerThread getInstance() throws Exception {
		if (_instance == null) {
			_instance = new GameServerThread();
		}
		return _instance;
	}

	public GameServerThread() throws Exception {
		double rateXp = Config.RATE_XP;
		double LA = Config.RATE_LA;
		double rateKarma = Config.RATE_KARMA;
		double rateDropItems = Config.RATE_DROP_ITEMS;
		double rateDropAdena = Config.RATE_DROP_ADENA;
		double hpregen = Config.RATE_HP_REGEN;
		double mpregen = Config.RATE_MP_REGEN;
		double castlehp = Config.RATE_HP_CASTLE;
		double househp = Config.RATE_HP_HOUSE;
		double hotelhp = Config.RATE_HP_HOTEL;
		double motherhp = Config.RATE_HP_MOTHERTREE;
		double hpill = Config.RATE_HP_ILLUSIONISTTOWN;
		double hpdrgn = Config.RATE_HP_DRAGONKNIGHTTOWN;
		double mpcastle = Config.RATE_MP_CASTLE;
		double mphouse = Config.RATE_MP_HOUSE;
		double mphotel = Config.RATE_MP_HOTEL;
		double mpmother = Config.RATE_MP_MOTHERTREE;
		double mpill = Config.RATE_MP_ILLUSIONISTTOWN;
		double mpdrgn = Config.RATE_MP_DRAGONKNIGHTTOWN;
		chatlvl = Config.GLOBAL_CHAT_LEVEL;

		_log.info("HP-REGEN RATE              = " + hpregen);
		_log.info("MP-REGEN RATE              = " + mpregen);
		_log.info("HP-CASTLE RATE             = " + castlehp);
		_log.info("MP-CASTLE RATE             = " + mpcastle);
		_log.info("HP-HOUSE RATE              = " + househp);
		_log.info("MP-HOUSE RATE              = " + mphouse);
		_log.info("HP-HOTEL RATE              = " + hotelhp);
		_log.info("MP-HOTEL RATE              = " + mphotel);
		_log.info("HP-MOTHERTREE RATE         = " + motherhp);
		_log.info("MP-MOTHERTREE RATE         = " + mpmother);
		_log.info("HP-ILLUSIONISTTOWN RATE    = " + hpill);
		_log.info("MP-ILLUSIONISTTOWN RATE    = " + mpill);
		_log.info("HP-DRAGONKNIGHTTOWN RATE   = " + hpdrgn);
		_log.info("MP-DRAGONKNIGHTTOWN RATE   = " + mpdrgn);
		_log.info("PartyExp RATE              = " + (Config.PARTYEXP_RATE));
		_log.info("PetExp RATE                = " + (Config.PETEXP_RATE));
		_log.info("XP RATE                    = " + rateXp);
		_log.info("Lawful RATE                = " + LA);
		_log.info("Karma RATE                 = " + rateKarma);
		_log.info("Drop RATE                  = " + rateDropItems);
		_log.info("Adena RATE                 = " + rateDropAdena);
		_log.info("EnchantWeapon Change       = " + (Config.ENCHANT_CHANCE_WEAPON) + "%");
		_log.info("EnchantArmor Change        = " + (Config.ENCHANT_CHANCE_ARMOR) + "%");
		_log.info("AttrEnchant Change         = " + (Config.ATTR_ENCHANT_CHANCE) + "%");
		_log.info("Global Chat LvL            = " + (chatlvl));
		_log.info("Whisper Chat LvL           = " + (Config.WHISPER_CHAT_LEVEL));

		if (Config.ALT_NONPVP) { // Non-PvP Setting
			_log.info("PvP                        = On");
		} else {
			_log.info("PvP                        = Off");
		}

		// Announce Chat Cycle
		Announcecycle.getInstance();

		if (Config.Use_Show_INGAMENEWS_Time) {
			_log.info("IngameNews                 = On");
		} else {
			_log.info("InGameNews                 = Off");
		}

		System.gc();

		int maxOnlineUsers = Config.MAX_ONLINE_USERS;
		_log.info("Max online users           = " + (maxOnlineUsers));

		IdFactory.getInstance();

		L1WorldMap.getInstance();

		_loginController = LoginController.getInstance();
		_loginController.setMaxAllowedOnlinePlayers(maxOnlineUsers);

		// Access Levels
		AccessLevelTable.getInstance();

		// CharacterTable
		CharacterTable.getInstance().loadAllCharName();

		// Reset status online
		CharacterTable.clearOnlineStatus();

		// Game Time Clock
		L1GameTimeClock.init();

		// UB Time Controllers
		UbTimeController ubTimeContoroller = UbTimeController.getInstance();
		GeneralThreadPool.getInstance().execute(ubTimeContoroller);

		// Controllers time of war
		WarTimeController warTimeController = WarTimeController.getInstance();
		GeneralThreadPool.getInstance().execute(warTimeController);

		// Elemental stone spawn
		if (Config.ELEMENTAL_STONE_AMOUNT > 0) {
			ElementalStoneGenerator elementalStoneGenerator = ElementalStoneGenerator.getInstance();
			GeneralThreadPool.getInstance().execute(elementalStoneGenerator);
		}

		// Hometown
		HomeTownTimeController.getInstance();

		// Controllers auction time Hideout
		AuctionTimeController auctionTimeController = AuctionTimeController.getInstance();
		GeneralThreadPool.getInstance().execute(auctionTimeController);

		// HouseTax Update Controller
		HouseTaxTimeController houseTaxTimeController = HouseTaxTimeController.getInstance();
		GeneralThreadPool.getInstance().execute(houseTaxTimeController);

		// Fishing Time Controller
		FishingTimeController fishingTimeController = FishingTimeController.getInstance();
		GeneralThreadPool.getInstance().execute(fishingTimeController);

		// Npc And Monster Chat Controller
		NpcChatTimeController npcChatTimeController = NpcChatTimeController.getInstance();
		GeneralThreadPool.getInstance().execute(npcChatTimeController);

		// LightTime Controller
		LightTimeController lightTimeController = LightTimeController.getInstance();
		GeneralThreadPool.getInstance().execute(lightTimeController);

		// Rankings Board Controller
		RankingsController rankingsController = RankingsController.getInstance();
		GeneralThreadPool.getInstance().execute(rankingsController);

		// Unjail controller
		JailController jailController = JailController.getInstance();
		GeneralThreadPool.getInstance().scheduleAtFixedRate(jailController, 0, 60000);

		CrackOfTimeController.getStart();

		// register all dungeons with a time limit
		MapTimeController mapTimeController = MapTimeController.getInstance();
		mapTimeController.load();

		if (Config.ALT_BOSS_EVENT) {
			BossEventController bossEventController = BossEventController.getInstance();
			GeneralThreadPool.getInstance().execute(bossEventController);
		}

		// AnnounceMents
		Announcements.getInstance();

		// Npc Table
		NpcTable.getInstance();

		// Delete Items On Ground
		L1DeleteItemOnGround deleteitem = new L1DeleteItemOnGround();
		deleteitem.initialize();

		if (!NpcTable.getInstance().isInitialized()) {
			throw new Exception("Could not initialize the npc table");
		}
		SpawnTable.getInstance();
		MobGroupTable.getInstance();
		SkillTable.getInstance();
		PolyTable.getInstance();
		ItemTable.getInstance();
		DropTable.getInstance();
		DropItemTable.getInstance();
		ShopTable.getInstance();
		NPCTalkDataTable.getInstance();
		L1World.getInstance();
		L1WorldTraps.getInstance();
		Dungeon.getInstance();
		NpcSpawnTable.getInstance();
		IpTable.getInstance();
		MapsTable.getInstance();
		UBSpawnTable.getInstance();
		PetTable.getInstance();
		ClanTable.getInstance();
		CastleTable.getInstance();
		L1CastleLocation.setCastleTaxRate(); // This must be after the initial
												// CastleTable
		GetBackRestartTable.getInstance();
		GetBackTable.getInstance();
		DoorTable.initialize();
		GeneralThreadPool.getInstance();
		ChatLogTable.getInstance();
		WeaponSkillTable.getInstance();
		NpcActionTable.load();
		GMCommandsConfig.load();
		Getback.loadGetBack();
		PetTypeTable.load();
		L1BossCycle.load();
		L1TreasureBox.load();
		SprTable.getInstance();
		ResolventTable.getInstance();
		FurnitureSpawnTable.getInstance();
		NpcChatTable.getInstance();
		LightSpawnTable.getInstance();
		MailTable.getInstance();
		L1MapLimiter.load();

		_log.info("Database tables loaded successfully!");
	}
}