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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l1j.server.server.utils.IntRange;

public final class Config {
	private static Logger _log = LoggerFactory.getLogger(Config.class);

	/** Debug/release mode */
	public static final boolean DEBUG = false;

	/** Thread pools size */
	public static int THREAD_P_EFFECTS;

	public static int THREAD_P_GENERAL;

	public static int AI_MAX_THREAD;

	public static int THREAD_P_TYPE_GENERAL;
	
	public static int GAME_TIME_SHIFT;

	public static int THREAD_P_SIZE_GENERAL;

	/** Server control */
	public static String GAME_SERVER_HOST_NAME;

	public static int GAME_SERVER_PORT;

	public static String DB_DRIVER;

	public static String DB_URL;

	public static String DB_LOGIN;

	public static String DB_PASSWORD;

	public static String PASSWORD_SALT;

	public static String TIME_ZONE;

	public static int CLIENT_LANGUAGE;

	public static boolean HOSTNAME_LOOKUPS;

	public static int AUTOMATIC_KICK;

	public static boolean AUTO_CREATE_ACCOUNTS;

	public static short MAX_ONLINE_USERS;

	public static boolean CACHE_MAP_FILES;

	public static boolean CHECK_MOVE_INTERVAL;

	public static boolean CHECK_ATTACK_INTERVAL;

	public static boolean CHECK_SPELL_INTERVAL;

	public static short INJUSTICE_COUNT;

	public static int JUSTICE_COUNT;

	public static int CHECK_STRICTNESS;

	public static byte LOGGING_WEAPON_ENCHANT;

	public static byte LOGGING_ENCHANT_FAIL;

	public static byte LOGGING_ARMOR_ENCHANT;

	public static boolean LOGGING_CHAT_NORMAL;

	public static boolean LOGGING_CHAT_WHISPER;

	public static boolean LOGGING_CHAT_SHOUT;

	public static boolean LOGGING_CHAT_WORLD;

	public static boolean LOGGING_CHAT_CLAN;

	public static boolean LOGGING_CHAT_PARTY;

	public static boolean LOGGING_CHAT_COMBINED;

	public static boolean LOGGING_CHAT_CHAT_PARTY;

	public static boolean LOGGING_INCOMING_PACKETS;

	public static int AUTOSAVE_INTERVAL;

	public static int AUTOSAVE_INTERVAL_INVENTORY;

	public static int SKILLTIMER_IMPLTYPE;

	public static int CLIENT_HISTORICAL_PACKETS;

	public static int NPCAI_IMPLTYPE;

	public static boolean TELNET_SERVER;

	public static int TELNET_SERVER_PORT;

	public static boolean TELNET_LOCALHOST_ONLY;

	public static boolean SSH_SERVER;

	public static int SSH_PORT;

	public static String SSH_KEY_FILES_DIRECTORY;

	public static String SSH_HOST_KEY;

	public static String[] SSH_ALLOWED_USERNAMES;

	public static int PC_RECOGNIZE_RANGE;

	public static int NPC_PATHING_RANGE;

	public static int NPC_ACTIVE_RANGE;

	public static boolean CHARACTER_CONFIG_IN_SERVER_SIDE;

	public static boolean ALLOW_2PC;

	public static int LEVEL_DOWN_RANGE;

        public static boolean SEND_PACKET_BEFORE_TELEPORT;
        public static boolean ENABLE_TOI_RANDOM_TELEPORT;

	public static boolean DETECT_DB_RESOURCE_LEAKS;

        public static boolean DUAL_PINK;

	public static boolean CHAO_PINK;

	public static boolean AUTO_BAN;

	public static int ANIMATION_SPEED;

	public static int REPORT_HOURS_RESET;

	public static int REPORT_TIME_MINUTES;

	public static boolean RESET_DUNGEONS_DAILY;

	public static String DUNGEON_RESET_TIME;
	
	public static boolean ENABLE_AUTO_QUEST_LEVEL15;
	
	public static boolean ENABLE_AUTO_QUEST_LEVEL30;
	
	public static boolean ENABLE_AUTO_QUEST_LEVEL50;

	public static boolean RESTRICT_ACCOUNT_IPS;

	public static String RESTRICT_ACCOUNT_IPS_MESSAGE;

	/** Rate control */
	public static int RATE_HP_REGEN;

	public static int RATE_MP_REGEN;

	public static int RATE_HP_CASTLE;

	public static int RATE_HP_HOUSE;

	public static int RATE_HP_HOTEL;

	public static int RATE_HP_MOTHERTREE;

	public static int RATE_HP_DRAGONKNIGHTTOWN;

	public static int RATE_HP_ILLUSIONISTTOWN;

	public static int RATE_MP_CASTLE;

	public static int RATE_MP_HOUSE;

	public static int RATE_MP_HOTEL;

	public static int RATE_MP_MOTHERTREE;

	public static int RATE_MP_DRAGONKNIGHTTOWN;

	public static int RATE_MP_ILLUSIONISTTOWN;

	public static double PETEXP_RATE;

	public static double SUMMON_STEAL_RATE;

	public static double PET_STEAL_RATE;

	public static double PARTYEXP_RATE;

	public static double RATE_XP;

	public static double RATE_LA;

	public static double RATE_CHAO_LA;

	public static double RATE_KARMA;

	public static double RATE_DROP_ADENA;

	public static double RATE_DROP_ITEMS;

	public static int ENCHANT_CHANCE_WEAPON;

	public static int ENCHANT_CHANCE_ARMOR;

	public static int ATTR_ENCHANT_CHANCE;

	public static double RATE_WEIGHT_LIMIT;

	public static double RATE_WEIGHT_LIMIT_PET;

	public static double RATE_SHOP_SELLING_PRICE;

	public static double RATE_SHOP_PURCHASING_PRICE;

	/* Original settings to enable on-the-fly changes and reset of the rates */
	public static double RATE_LA_ORG;

	public static double RATE_KARMA_ORG;

	public static double RATE_XP_ORG;

	public static double RATE_DROP_ADENA_ORG;

	public static double RATE_DROP_ITEMS_ORG;

	public static double RATE_WEIGHT_LIMIT_ORG;
	/* Org Rates End */

	public static int CREATE_CHANCE_DIARY;

	public static int CREATE_CHANCE_RECOLLECTION;

	public static int CREATE_CHANCE_MYSTERIOUS;

	public static int CREATE_CHANCE_PROCESSING;

	public static int CREATE_CHANCE_PROCESSING_DIAMOND;

	public static int CREATE_CHANCE_DANTES;

	public static int CREATE_CHANCE_ANCIENT_AMULET;

	public static int CREATE_CHANCE_HISTORY_BOOK;

	/** AltSettings control */
	public static short MIN_GM_ACCESS_LEVEL;

	public static short GLOBAL_CHAT_LEVEL;

	public static short WHISPER_CHAT_LEVEL;

	public static byte AUTO_LOOT;

	public static int LOOTING_RANGE;

	public static boolean ALT_NONPVP;

	public static boolean ALT_ATKMSG;

	public static boolean CHANGE_TITLE_BY_ONESELF;

	public static int MAX_CLAN_MEMBER;

	public static boolean CLAN_ALLIANCE;

	public static int PET_RACE_MIN_PLAYER = 2;

	public static int PET_RACE_MAX_LAP = 3;

	public static int DEATH_MATCH_MIN_PLAYER = 2;

	public static boolean USE_TOI_CHARM_ANYWHERE;

	public static boolean ELEMENTAL_ENCHANTING;

	public static int ELEMENTAL_ENCHANT_LIMIT;

	public static boolean ACCESSORY_ENCHANTING;

	public static int ACCESSORY_ENCHANT_LIMIT;

	public static boolean RANDOMIZE_BOSS_SPAWNS;
	public static double RANDOMIZED_BOSS_SPAWN_FACTOR;
	public static boolean ROYAL_LEVEL_DAMAGE;
	public static boolean USE_INT_PROCS;
	public static boolean AUTO_STONE;

	public static int MAX_PT;

	public static int MAX_CHAT_PT;

	public static boolean SIM_WAR_PENALTY;

	public static boolean GET_BACK;

	public static int NEWBIEMAPLEVELS;

	public static String ALT_ITEM_DELETION_TYPE;

	public static int ALT_ITEM_DELETION_TIME;

	public static int ALT_ITEM_DELETION_RANGE;

	public static boolean ALT_GMSHOP;

	public static int ALT_GMSHOP_MIN_ID;

	public static int ALT_GMSHOP_MAX_ID;

	public static boolean ALT_HALLOWEENEVENT;

	public static boolean ALT_HALLOWEENEVENTNPC;

	public static boolean ALT_POLYEVENT;

	public static boolean ALT_JPPRIVILEGED;

	public static boolean ALT_TALKINGSCROLLQUEST;

	public static boolean ALT_WHO_COMMAND;

	public static boolean ALT_REVIVAL_POTION;

	public static int ALT_WAR_TIME;

	public static int ALT_WAR_TIME_UNIT;

	public static int ALT_WAR_INTERVAL;

	public static int ALT_WAR_INTERVAL_UNIT;

	public static int ALT_RATE_OF_DUTY;

	public static boolean ALT_BOSS_EVENT;

	public static String ALT_BOSS_EVENT_BOSSES;

	public static boolean ALT_BOSS_EVENT_DAILY_RESET;

	public static String ALT_BOSS_EVENT_RESET_TIME;

	public static long ALT_DAYS_LIMIT_PLEDGE_JOIN;

	public static boolean INIT_BOSS_SPAWN;

	public static int ELEMENTAL_STONE_AMOUNT;

	public static int HAUNTEDHOUSETIME;

	public static int HOUSE_TAX_INTERVAL;

	public static int MAX_DOLL_COUNT;

	public static boolean RETURN_TO_NATURE;

	public static int MAX_NPC_ITEM;

	public static int MAX_PERSONAL_WAREHOUSE_ITEM;

	public static int MAX_CLAN_WAREHOUSE_ITEM;

	public static boolean DELETE_CHARACTER_AFTER_7DAYS;

	public static boolean MONITOR_COMMANDS;

	public static boolean WARP;

	public static boolean STACKING;

	public static boolean SKT_START;

	public static boolean SOFT_AC;

	public static boolean GHEY_MARRAIGE;

	public static int NPC_DELETION_TIME;

	public static int DEFAULT_CHARACTER_SLOT;

	public static int MONSTERPOTIONINTUSE;

	public static int COOKING_TIME;

	/** CharSettings control */
	public static int PRINCE_MAX_HP;

	public static int PRINCE_MAX_MP;

	public static int KNIGHT_MAX_HP;

	public static int KNIGHT_MAX_MP;

	public static int ELF_MAX_HP;

	public static int ELF_MAX_MP;

	public static int WIZARD_MAX_HP;

	public static int WIZARD_MAX_MP;

	public static int DARKELF_MAX_HP;

	public static int DARKELF_MAX_MP;

	public static int DRAGONKNIGHT_MAX_HP;

	public static int DRAGONKNIGHT_MAX_MP;

	public static int ILLUSIONIST_MAX_HP;

	public static int ILLUSIONIST_MAX_MP;

	public static int LV50_EXP;

	public static int LV51_EXP;

	public static int LV52_EXP;

	public static int LV53_EXP;

	public static int LV54_EXP;

	public static int LV55_EXP;

	public static int LV56_EXP;

	public static int LV57_EXP;

	public static int LV58_EXP;

	public static int LV59_EXP;

	public static int LV60_EXP;

	public static int LV61_EXP;

	public static int LV62_EXP;

	public static int LV63_EXP;

	public static int LV64_EXP;

	public static int LV65_EXP;

	public static int LV66_EXP;

	public static int LV67_EXP;

	public static int LV68_EXP;

	public static int LV69_EXP;

	public static int LV70_EXP;

	public static int LV71_EXP;

	public static int LV72_EXP;

	public static int LV73_EXP;

	public static int LV74_EXP;

	public static int LV75_EXP;

	public static int LV76_EXP;

	public static int LV77_EXP;

	public static int LV78_EXP;

	public static int LV79_EXP;

	public static int LV80_EXP;

	public static int LV81_EXP;

	public static int LV82_EXP;

	public static int LV83_EXP;

	public static int LV84_EXP;

	public static int LV85_EXP;

	public static int LV86_EXP;

	public static int LV87_EXP;

	public static int LV88_EXP;

	public static int LV89_EXP;

	public static int LV90_EXP;

	public static int LV91_EXP;

	public static int LV92_EXP;

	public static int LV93_EXP;

	public static int LV94_EXP;

	public static int LV95_EXP;

	public static int LV96_EXP;

	public static int LV97_EXP;

	public static int LV98_EXP;

	public static int LV99_EXP;

	public static boolean STOP_DROP;

	/** Player Command Settings */
	public static boolean PLAYER_COMMANDS;

	public static boolean PLAYER_BUFF;

	public static boolean POWER_BUFF;

	public static boolean DK_BUFF;

	/** Configuration files */
	public static final String SERVER_CONFIG_FILE = "./config/server.properties";

	public static final String RATES_CONFIG_FILE = "./config/rates.properties";

	public static final String ALT_SETTINGS_FILE = "./config/altsettings.properties";

	public static final String CHAR_SETTINGS_CONFIG_FILE = "./config/charsettings.properties";

	public static final String PCOMMANDS_SETTINGS_FILE = "./config/pcommands.properties";

	public static final int MANA_DRAIN_LIMIT_PER_NPC = 40;

	public static final int MANA_DRAIN_LIMIT_PER_SOM_ATTACK = 9;

	public static boolean Use_Show_INGAMENEWS_Time;

	public static int Show_INGAMENEWS_Time;

	public static boolean USE_PINE_IN_SAFETY;

	public static boolean MOVE_MACROABLE_NPCS;

	public static int MIN_ATONEMENT;

	public static int ATONEMENT_COST;

	public static int NUM_PKS_HELL;

	public static int NUM_PKS_HELL_WARNING;

	public static int DOT_RELOAD_WAIT_TIME;
	public static int DOT_RELOAD_PINK_WAIT_TIME;

	public static int CASTLE_WAR_MIN_PRINCE_LEVEL;
	public static int CASTLE_WAR_MIN_MEMBERS_ONLINE;
	public static int CASTLE_WAR_MIN_MEMBERS_LEVEL;

	public static int ALT_RANKING_OVERALL_TOP;
	public static int ALT_RANKING_CLASS_TOP;
	public static int ALT_RANKING_MIN_LEVEL;
	public static String ALT_RANKING_PENALTY_TYPES;

	public static boolean LIMIT_WEAPON_SWITCHING;

        public static int MAX_SERVANT_SUMMONS;
        public static boolean INVENTORY_CHA;

	public static boolean ALT_PET_HUNGER_STATUS_CHANGE;

	/** Security Settings **/
	public static int DELAY_DISCONNECT;
	public static int NON_AGGRO_LOGOUT_TIMER;
	public static int CONNECTIONS_PER_IP;

	public static void load() {
		_log.info("Loading GameServer config.");

		// server.properties
		try {
			Properties serverSettings = new Properties();
			InputStream is = new FileInputStream(new File(SERVER_CONFIG_FILE));
			serverSettings.load(is);
			is.close();
			GAME_SERVER_HOST_NAME = serverSettings.getProperty("GameserverHostname", "*");
			GAME_SERVER_PORT = Integer.parseInt(serverSettings.getProperty("GameserverPort", "2000"));
			DB_DRIVER = serverSettings.getProperty("Driver", "com.mysql.jdbc.Driver");
			DB_URL = serverSettings.getProperty("URL",
					"jdbc:mysql://localhost/l1jdb?useUnicode=True&characterEncoding=UTF-8");
			DB_LOGIN = serverSettings.getProperty("Login", "root");
			DB_PASSWORD = serverSettings.getProperty("Password", "");
			GAME_TIME_SHIFT = Integer.parseInt(serverSettings.getProperty("GameTimeShift", "0"));
			PASSWORD_SALT = serverSettings.getProperty("PasswordSalt", "");
			THREAD_P_TYPE_GENERAL = Integer.parseInt(serverSettings.getProperty("GeneralThreadPoolType", "0"), 10);
			THREAD_P_SIZE_GENERAL = Integer.parseInt(serverSettings.getProperty("GeneralThreadPoolSize", "0"), 10);
			TIME_ZONE = serverSettings.getProperty("TimeZone", "EST");
			CLIENT_LANGUAGE = Integer.parseInt(serverSettings.getProperty("ClientLanguage", "0"));
			HOSTNAME_LOOKUPS = Boolean.parseBoolean(serverSettings.getProperty("HostnameLookups", "False"));
			AUTOMATIC_KICK = Integer.parseInt(serverSettings.getProperty("AutomaticKick", "10"));
			AUTO_CREATE_ACCOUNTS = Boolean.parseBoolean(serverSettings.getProperty("AutoCreateAccounts", "True"));
			MAX_ONLINE_USERS = Short.parseShort(serverSettings.getProperty("MaximumOnlineUsers", "30"));
			CACHE_MAP_FILES = Boolean.parseBoolean(serverSettings.getProperty("CacheMapFiles", "False"));
			CHECK_MOVE_INTERVAL = Boolean.parseBoolean(serverSettings.getProperty("CheckMoveInterval", "False"));
			CHECK_ATTACK_INTERVAL = Boolean.parseBoolean(serverSettings.getProperty("CheckAttackInterval", "False"));
			CHECK_SPELL_INTERVAL = Boolean.parseBoolean(serverSettings.getProperty("CheckSpellInterval", "False"));
			Use_Show_INGAMENEWS_Time = Boolean.parseBoolean(serverSettings.getProperty("UseINGAMENEWS_Time", "False"));
			Show_INGAMENEWS_Time = Integer.parseInt(serverSettings.getProperty("ShowINGAMENEWS_Time", "30"));
			INJUSTICE_COUNT = Short.parseShort(serverSettings.getProperty("InjusticeCount", "10"));
			JUSTICE_COUNT = Integer.parseInt(serverSettings.getProperty("JusticeCount", "4"));
			CHECK_STRICTNESS = Integer.parseInt(serverSettings.getProperty("CheckStrictness", "102"));
			LOGGING_WEAPON_ENCHANT = Byte.parseByte(serverSettings.getProperty("LoggingWeaponEnchant", "0"));
			LOGGING_ARMOR_ENCHANT = Byte.parseByte(serverSettings.getProperty("LoggingArmorEnchant", "0"));
			LOGGING_ENCHANT_FAIL = Byte.parseByte(serverSettings.getProperty("LoggingEnchantFails", "0"));
			LOGGING_CHAT_NORMAL = Boolean.parseBoolean(serverSettings.getProperty("LoggingChatNormal", "False"));
			LOGGING_CHAT_WHISPER = Boolean.parseBoolean(serverSettings.getProperty("LoggingChatWhisper", "False"));
			LOGGING_CHAT_SHOUT = Boolean.parseBoolean(serverSettings.getProperty("LoggingChatShout", "False"));
			LOGGING_CHAT_WORLD = Boolean.parseBoolean(serverSettings.getProperty("LoggingChatWorld", "False"));
			LOGGING_CHAT_CLAN = Boolean.parseBoolean(serverSettings.getProperty("LoggingChatClan", "False"));
			LOGGING_CHAT_PARTY = Boolean.parseBoolean(serverSettings.getProperty("LoggingChatParty", "False"));
			LOGGING_CHAT_COMBINED = Boolean.parseBoolean(serverSettings.getProperty("LoggingChatCombined", "False"));
			LOGGING_CHAT_CHAT_PARTY = Boolean.parseBoolean(serverSettings.getProperty("LoggingChatChatParty", "False"));
			LOGGING_INCOMING_PACKETS = Boolean
					.parseBoolean(serverSettings.getProperty("LoggingIncomingPackets", "False"));
			AUTOSAVE_INTERVAL = Integer.parseInt(serverSettings.getProperty("AutosaveInterval", "1200"), 10);
			AUTOSAVE_INTERVAL_INVENTORY = Integer
					.parseInt(serverSettings.getProperty("AutosaveIntervalOfInventory", "300"), 10);
			SKILLTIMER_IMPLTYPE = Integer.parseInt(serverSettings.getProperty("SkillTimerImplType", "1"));

			CLIENT_HISTORICAL_PACKETS = Integer
					.parseInt(serverSettings.getProperty("ClientHistoricalPacketCount", "500"));

			NPCAI_IMPLTYPE = Integer.parseInt(serverSettings.getProperty("NpcAIImplType", "1"));
			TELNET_SERVER = Boolean.parseBoolean(serverSettings.getProperty("TelnetServer", "False"));
			TELNET_SERVER_PORT = Integer.parseInt(serverSettings.getProperty("TelnetServerPort", "23"));
			TELNET_LOCALHOST_ONLY = Boolean.parseBoolean(serverSettings.getProperty("TelnetLocalhostOnly", "True"));

			SSH_SERVER = Boolean.parseBoolean(serverSettings.getProperty("SSHServer", "True"));

			SSH_PORT = Integer.parseInt(serverSettings.getProperty("SSHPort", "16483"));

			SSH_KEY_FILES_DIRECTORY = serverSettings.getProperty("KeyFilesDirectory", "/l1j/keys/");

			SSH_HOST_KEY = serverSettings.getProperty("HostKey", "/l1j/keys/l1j_host_key");

			SSH_ALLOWED_USERNAMES = serverSettings.getProperty("AllowedUsernames", "").replace(" ", "").split(",");

			PC_RECOGNIZE_RANGE = Integer.parseInt(serverSettings.getProperty("PcRecognizeRange", "23"));
			NPC_PATHING_RANGE = Integer.parseInt(serverSettings.getProperty("NpcPathingRange", "18"));
			NPC_ACTIVE_RANGE = Integer.parseInt(serverSettings.getProperty("NpcActiveRange", "-1"));
			CHARACTER_CONFIG_IN_SERVER_SIDE = Boolean
					.parseBoolean(serverSettings.getProperty("CharacterConfigInServerSide", "True"));
			CONNECTIONS_PER_IP = Integer.parseInt(serverSettings.getProperty("ConnectionsPerIp", "2"));
			LEVEL_DOWN_RANGE = Integer.parseInt(serverSettings.getProperty("LevelDownRange", "0"));
                        SEND_PACKET_BEFORE_TELEPORT = Boolean
                                        .parseBoolean(serverSettings.getProperty("SendPacketBeforeTeleport", "False"));
                        ENABLE_TOI_RANDOM_TELEPORT = Boolean
                                        .parseBoolean(serverSettings.getProperty("EnableToiRandomTeleport", "True"));
			DETECT_DB_RESOURCE_LEAKS = Boolean
					.parseBoolean(serverSettings.getProperty("EnableDatabaseResourceLeaksDetection", "False"));
			DELAY_DISCONNECT = Integer.parseInt(serverSettings.getProperty("DelayDisconnect", "0"));
			NON_AGGRO_LOGOUT_TIMER = Integer.parseInt(serverSettings.getProperty("NonAggroLogoutTimer", "10000"));
			DUAL_PINK = Boolean.parseBoolean(serverSettings.getProperty("DualPink", "False"));
			CHAO_PINK = Boolean.parseBoolean(serverSettings.getProperty("ChaoPink", "False"));
			AUTO_BAN = Boolean.parseBoolean(serverSettings.getProperty("AutoBan", "False"));
			STOP_DROP = Boolean.parseBoolean(serverSettings.getProperty("StopDrop", "False"));
			ANIMATION_SPEED = Integer.parseInt(serverSettings.getProperty("DefaultAnimationSpeed", "720"));
                        MAX_SERVANT_SUMMONS = Integer.parseInt(serverSettings.getProperty("MaxServantSummons", "100"));
                        INVENTORY_CHA = Boolean.parseBoolean(serverSettings.getProperty("inventorycha", "True"));

			REPORT_HOURS_RESET = Integer.parseInt(serverSettings.getProperty("ReportHoursReset", "24"));
			REPORT_TIME_MINUTES = Integer.parseInt(serverSettings.getProperty("ReportTimeMinutes", "10"));

			RESET_DUNGEONS_DAILY = Boolean.parseBoolean(serverSettings.getProperty("ResetDungeonsDaily", "True"));
			DUNGEON_RESET_TIME = serverSettings.getProperty("DungeonResetTime", "00:00");
			RESTRICT_ACCOUNT_IPS = Boolean.parseBoolean(serverSettings.getProperty("RestrictAccountIps", "False"));
			RESTRICT_ACCOUNT_IPS_MESSAGE = serverSettings.getProperty("RestrictAccountIpsMessage");

		} catch (Exception e) {
			_log.error(e.getLocalizedMessage(), e);
			throw new Error("Failed to load " + SERVER_CONFIG_FILE + " file.");
		}

		// rates.properties
		_log.info("Loading Rates config.");
		try {
			Properties rateSettings = new Properties();
			InputStream is = new FileInputStream(new File(RATES_CONFIG_FILE));
			rateSettings.load(is);
			is.close();
			RATE_HP_REGEN = Integer.parseInt(rateSettings.getProperty("RateHpRegen", "2"));
			RATE_MP_REGEN = Integer.parseInt(rateSettings.getProperty("RateMpRegen", "1"));
			RATE_HP_CASTLE = Integer.parseInt(rateSettings.getProperty("RateHpCastle", "5"));
			RATE_MP_CASTLE = Integer.parseInt(rateSettings.getProperty("RateMpCastle", "3"));
			RATE_HP_HOUSE = Integer.parseInt(rateSettings.getProperty("RateHpHouse", "5"));
			RATE_MP_HOUSE = Integer.parseInt(rateSettings.getProperty("RateMpHouse", "3"));
			RATE_HP_HOTEL = Integer.parseInt(rateSettings.getProperty("RateHpHotel", "5"));
			RATE_MP_HOTEL = Integer.parseInt(rateSettings.getProperty("RateMpHotel", "3"));
			RATE_HP_MOTHERTREE = Integer.parseInt(rateSettings.getProperty("RateHpMotherTree", "5"));
			RATE_MP_MOTHERTREE = Integer.parseInt(rateSettings.getProperty("RateMpMotherTree", "3"));

			RATE_HP_ILLUSIONISTTOWN = Integer.parseInt(rateSettings.getProperty("RateHpIllusionisttown", "5"));
			RATE_MP_ILLUSIONISTTOWN = Integer.parseInt(rateSettings.getProperty("RateMpIllusionisttown", "3"));
			RATE_HP_DRAGONKNIGHTTOWN = Integer.parseInt(rateSettings.getProperty("RateHpDragonknighttown", "5"));
			RATE_MP_DRAGONKNIGHTTOWN = Integer.parseInt(rateSettings.getProperty("RateMpDragonknighttown", "3"));

			PETEXP_RATE = Double.parseDouble(rateSettings.getProperty("PetExp", "1.0"));
			SUMMON_STEAL_RATE = Double.parseDouble(rateSettings.getProperty("SummonSteal", "1.0"));
			PET_STEAL_RATE = Double.parseDouble(rateSettings.getProperty("PetSteal", "1.0"));
			PARTYEXP_RATE = Double.parseDouble(rateSettings.getProperty("PartyExp", "1.0"));
			RATE_XP = Double.parseDouble(rateSettings.getProperty("RateXp", "1.0"));
			RATE_LA = Double.parseDouble(rateSettings.getProperty("RateLawful", "1.0"));
			RATE_CHAO_LA = Double.parseDouble(rateSettings.getProperty("RateChaoLawful", "1.0"));
			RATE_KARMA = Double.parseDouble(rateSettings.getProperty("RateKarma", "1.0"));
			RATE_DROP_ADENA = Double.parseDouble(rateSettings.getProperty("RateDropAdena", "1.0"));
			RATE_DROP_ITEMS = Double.parseDouble(rateSettings.getProperty("RateDropItems", "1.0"));
			ENCHANT_CHANCE_WEAPON = Integer.parseInt(rateSettings.getProperty("EnchantChanceWeapon", "68"));
			ENCHANT_CHANCE_ARMOR = Integer.parseInt(rateSettings.getProperty("EnchantChanceArmor", "52"));
			ATTR_ENCHANT_CHANCE = Integer.parseInt(rateSettings.getProperty("AttrEnchantChance", "10"));
			RATE_WEIGHT_LIMIT = Double.parseDouble(rateSettings.getProperty("RateWeightLimit", "1"));
			RATE_WEIGHT_LIMIT_PET = Double.parseDouble(rateSettings.getProperty("RateWeightLimitforPet", "1"));
			RATE_SHOP_SELLING_PRICE = Double.parseDouble(rateSettings.getProperty("RateShopSellingPrice", "1.0"));
			RATE_SHOP_PURCHASING_PRICE = Double.parseDouble(rateSettings.getProperty("RateShopPurchasingPrice", "1.0"));
			CREATE_CHANCE_DIARY = Integer.parseInt(rateSettings.getProperty("CreateChanceDiary", "33"));
			CREATE_CHANCE_RECOLLECTION = Integer.parseInt(rateSettings.getProperty("CreateChanceRecollection", "90"));
			CREATE_CHANCE_MYSTERIOUS = Integer.parseInt(rateSettings.getProperty("CreateChanceMysterious", "90"));
			CREATE_CHANCE_PROCESSING = Integer.parseInt(rateSettings.getProperty("CreateChanceProcessing", "90"));
			CREATE_CHANCE_PROCESSING_DIAMOND = Integer
					.parseInt(rateSettings.getProperty("CreateChanceProcessingDiamond", "90"));
			CREATE_CHANCE_DANTES = Integer.parseInt(rateSettings.getProperty("CreateChanceDantes", "50"));
			CREATE_CHANCE_ANCIENT_AMULET = Integer
					.parseInt(rateSettings.getProperty("CreateChanceAncientAmulet", "90"));
			CREATE_CHANCE_HISTORY_BOOK = Integer.parseInt(rateSettings.getProperty("CreateChanceHistoryBook", "50"));
			RATE_LA_ORG = RATE_LA;
			RATE_KARMA_ORG = RATE_KARMA;
			RATE_XP_ORG = RATE_XP;
			RATE_DROP_ADENA_ORG = RATE_DROP_ADENA;
			RATE_DROP_ITEMS_ORG = RATE_DROP_ITEMS;
			RATE_WEIGHT_LIMIT_ORG = RATE_WEIGHT_LIMIT;
		} catch (Exception e) {
			_log.error(e.getLocalizedMessage(), e);
			throw new Error("Failed to load " + RATES_CONFIG_FILE + " file.");
		}

		// altsettings.properties
		_log.info("Loading AltSettings config.");
		try {
			Properties altSettings = new Properties();
			InputStream is = new FileInputStream(new File(ALT_SETTINGS_FILE));
			altSettings.load(is);
			is.close();
			MIN_GM_ACCESS_LEVEL = Short.parseShort(altSettings.getProperty("GmAccessLevel", "100"));
			GLOBAL_CHAT_LEVEL = Short.parseShort(altSettings.getProperty("GlobalChatLevel", "30"));
			WHISPER_CHAT_LEVEL = Short.parseShort(altSettings.getProperty("WhisperChatLevel", "5"));
			AUTO_LOOT = Byte.parseByte(altSettings.getProperty("AutoLoot", "2"));
			LOOTING_RANGE = Integer.parseInt(altSettings.getProperty("LootingRange", "3"));
			NEWBIEMAPLEVELS = Integer.parseInt(altSettings.getProperty("Newbiemaplevel", "15"));
			ALT_NONPVP = Boolean.parseBoolean(altSettings.getProperty("NonPvP", "True"));
			ALT_ATKMSG = Boolean.parseBoolean(altSettings.getProperty("AttackMessageOn", "False"));
			CHANGE_TITLE_BY_ONESELF = Boolean.parseBoolean(altSettings.getProperty("ChangeTitleByOneself", "False"));
			MAX_CLAN_MEMBER = Integer.parseInt(altSettings.getProperty("MaxClanMember", "0"));
			CLAN_ALLIANCE = Boolean.parseBoolean(altSettings.getProperty("ClanAlliance", "True"));
			MAX_PT = Integer.parseInt(altSettings.getProperty("MaxPT", "8"));
			MAX_CHAT_PT = Integer.parseInt(altSettings.getProperty("MaxChatPT", "8"));
			SIM_WAR_PENALTY = Boolean.parseBoolean(altSettings.getProperty("SimWarPenalty", "True"));
			GET_BACK = Boolean.parseBoolean(altSettings.getProperty("GetBack", "False"));
			ALT_ITEM_DELETION_TYPE = altSettings.getProperty("ItemDeletionType", "auto");
			ALT_ITEM_DELETION_TIME = Integer.parseInt(altSettings.getProperty("ItemDeletionTime", "10"));
			ALT_ITEM_DELETION_RANGE = Integer.parseInt(altSettings.getProperty("ItemDeletionRange", "5"));
			ALT_GMSHOP = Boolean.parseBoolean(altSettings.getProperty("GMshop", "False"));
			ALT_GMSHOP_MIN_ID = Integer.parseInt(altSettings.getProperty("GMshopMinID", "0xffffffff"));
			ALT_GMSHOP_MAX_ID = Integer.parseInt(altSettings.getProperty("GMshopMaxID", "0xffffffff"));
			ALT_HALLOWEENEVENT = Boolean.parseBoolean(altSettings.getProperty("HalloweenEvent", "False"));
			ALT_HALLOWEENEVENTNPC = Boolean.parseBoolean(altSettings.getProperty("HalloweenEventNpc", "False"));
			ALT_POLYEVENT = Boolean.parseBoolean(altSettings.getProperty("PolyEvent", "False"));
			ALT_BOSS_EVENT = Boolean.parseBoolean(altSettings.getProperty("BossEvent", "False"));
			ALT_BOSS_EVENT_BOSSES = altSettings.getProperty("BossEventBosses", "");
			ALT_BOSS_EVENT_DAILY_RESET = Boolean.parseBoolean(altSettings.getProperty("BossEventDailyReset", "False"));
			ALT_BOSS_EVENT_RESET_TIME = altSettings.getProperty("BossEventDailyResetTime", "00:00");
			ENABLE_AUTO_QUEST_LEVEL15 = Boolean.parseBoolean(altSettings.getProperty("EnableAutoQuestLevel15", "True"));
			ENABLE_AUTO_QUEST_LEVEL30 = Boolean.parseBoolean(altSettings.getProperty("EnableAutoQuestLevel30", "True"));
			ENABLE_AUTO_QUEST_LEVEL50 = Boolean.parseBoolean(altSettings.getProperty("EnableAutoQuestLevel50", "True"));

			
			ALT_DAYS_LIMIT_PLEDGE_JOIN = TimeUnit.DAYS
					.toMillis(Integer.parseInt(altSettings.getProperty("DaysLimitPledgeJoin", "0")));

			ALT_JPPRIVILEGED = Boolean.parseBoolean(altSettings.getProperty("JpPrivileged", "False"));
			ALT_TALKINGSCROLLQUEST = Boolean.parseBoolean(altSettings.getProperty("TalkingScrollQuest", "False"));
			ALT_WHO_COMMAND = Boolean.parseBoolean(altSettings.getProperty("WhoCommand", "False"));
			ALT_REVIVAL_POTION = Boolean.parseBoolean(altSettings.getProperty("RevivalPotion", "False"));
			MONSTERPOTIONINTUSE = Integer.parseInt(altSettings.getProperty("MonsterIntPotions", "13"));
			COOKING_TIME = Integer.parseInt(altSettings.getProperty("CookingTime", "3"));
			HAUNTEDHOUSETIME = Integer.parseInt(altSettings.getProperty("HauntedHouseTime", "90000"));
			RANDOMIZE_BOSS_SPAWNS = Boolean.parseBoolean(altSettings.getProperty("RandomizeBossSpawns", "False"));
			RANDOMIZED_BOSS_SPAWN_FACTOR = Double
					.parseDouble(altSettings.getProperty("RandomizedBossSpawnFactor", ".5"));
			ROYAL_LEVEL_DAMAGE = Boolean.parseBoolean(altSettings.getProperty("RoyalLevelDamage", "False"));
			USE_INT_PROCS = Boolean.parseBoolean(altSettings.getProperty("UseIntProcs", "False"));
			AUTO_STONE = Boolean.parseBoolean(altSettings.getProperty("UseAutoStone", "False"));
			USE_PINE_IN_SAFETY = Boolean.parseBoolean(altSettings.getProperty("UsePineInSafety", "True"));
                        MOVE_MACROABLE_NPCS = Boolean.parseBoolean(altSettings.getProperty("MoveMacroableNpcs", "False"));
                        NUM_PKS_HELL = Integer.parseInt(altSettings.getProperty("PKsForHell", "10"));
			NUM_PKS_HELL_WARNING = Integer.parseInt(altSettings.getProperty("PKsForHellWarning", "5"));
			MIN_ATONEMENT = Integer.parseInt(altSettings.getProperty("MinPksForAtonement", "5"));
			ATONEMENT_COST = Integer.parseInt(altSettings.getProperty("CostOfAtonement", "700000"));
			DOT_RELOAD_WAIT_TIME = Integer.parseInt(altSettings.getProperty("DotReloadWaitTime", "0"));
			DOT_RELOAD_PINK_WAIT_TIME = Integer.parseInt(altSettings.getProperty("DotReloadPinkWaitTime", "0"));
			CASTLE_WAR_MIN_PRINCE_LEVEL = Integer.parseInt(altSettings.getProperty("CastleWarMinPrinceLevel", "1"));
			CASTLE_WAR_MIN_MEMBERS_ONLINE = Integer.parseInt(altSettings.getProperty("CastleWarMinMembersOnline", "0"));
			CASTLE_WAR_MIN_MEMBERS_LEVEL = Integer.parseInt(altSettings.getProperty("CastleWarMinMembersLevel", "1"));
			ALT_RANKING_OVERALL_TOP = Integer.parseInt(altSettings.getProperty("RankingOverallTop", "25"));
			ALT_RANKING_CLASS_TOP = Integer.parseInt(altSettings.getProperty("RankingClassTop", "10"));

			ALT_RANKING_MIN_LEVEL = Integer.parseInt(altSettings.getProperty("RankingMinLevel", "70"));
			ALT_RANKING_PENALTY_TYPES = altSettings.getProperty("RankingPenaltyTypes", "5,8,9");

			LIMIT_WEAPON_SWITCHING = Boolean.parseBoolean(altSettings.getProperty("LimitWeaponSwitching", "False"));
			String strWar;
			strWar = altSettings.getProperty("WarTime", "2h");
			if (strWar.indexOf("d") >= 0) {
				ALT_WAR_TIME_UNIT = Calendar.DATE;
				strWar = strWar.replace("d", "");
			} else if (strWar.indexOf("h") >= 0) {
				ALT_WAR_TIME_UNIT = Calendar.HOUR_OF_DAY;
				strWar = strWar.replace("h", "");
			} else if (strWar.indexOf("m") >= 0) {
				ALT_WAR_TIME_UNIT = Calendar.MINUTE;
				strWar = strWar.replace("m", "");
			}
			ALT_WAR_TIME = Integer.parseInt(strWar);
			strWar = altSettings.getProperty("WarInterval", "4d");
			if (strWar.indexOf("d") >= 0) {
				ALT_WAR_INTERVAL_UNIT = Calendar.DATE;
				strWar = strWar.replace("d", "");
			} else if (strWar.indexOf("h") >= 0) {
				ALT_WAR_INTERVAL_UNIT = Calendar.HOUR_OF_DAY;
				strWar = strWar.replace("h", "");
			} else if (strWar.indexOf("m") >= 0) {
				ALT_WAR_INTERVAL_UNIT = Calendar.MINUTE;
				strWar = strWar.replace("m", "");
			}
			ALT_WAR_INTERVAL = Integer.parseInt(strWar);
			INIT_BOSS_SPAWN = Boolean.parseBoolean(altSettings.getProperty("InitBossSpawn", "True"));
			ELEMENTAL_STONE_AMOUNT = Integer.parseInt(altSettings.getProperty("ElementalStoneAmount", "300"));
			HOUSE_TAX_INTERVAL = Integer.parseInt(altSettings.getProperty("HouseTaxInterval", "10"));
			MAX_DOLL_COUNT = Integer.parseInt(altSettings.getProperty("MaxDollCount", "1"));
			MONITOR_COMMANDS = Boolean.parseBoolean(altSettings.getProperty("MonitorCommands", "True"));
			WARP = Boolean.parseBoolean(altSettings.getProperty("Warp", "True"));
			STACKING = Boolean.parseBoolean(altSettings.getProperty("Stacking", "True"));
			SKT_START = Boolean.parseBoolean(altSettings.getProperty("SKTStart", "False"));
			SOFT_AC = Boolean.parseBoolean(altSettings.getProperty("SoftAC", "True"));
			GHEY_MARRAIGE = Boolean.parseBoolean(altSettings.getProperty("GheyMarraige", "False"));
			MAX_CLAN_MEMBER = Integer.parseInt(altSettings.getProperty("MaxClanMember", "0"));
			CLAN_ALLIANCE = Boolean.parseBoolean(altSettings.getProperty("ClanAlliance", "True"));
			RETURN_TO_NATURE = Boolean.parseBoolean(altSettings.getProperty("ReturnToNature", "False"));
			MAX_NPC_ITEM = Integer.parseInt(altSettings.getProperty("MaxNpcItem", "20"));
			MAX_PERSONAL_WAREHOUSE_ITEM = Integer.parseInt(altSettings.getProperty("MaxPersonalWarehouseItem", "100"));
			MAX_CLAN_WAREHOUSE_ITEM = Integer.parseInt(altSettings.getProperty("MaxClanWarehouseItem", "200"));
			DELETE_CHARACTER_AFTER_7DAYS = Boolean
					.parseBoolean(altSettings.getProperty("DeleteCharacterAfter7Days", "True"));
			NPC_DELETION_TIME = Integer.parseInt(altSettings.getProperty("NpcDeletionTime", "10"));
			DEFAULT_CHARACTER_SLOT = Integer.parseInt(altSettings.getProperty("DefaultCharacterSlot", "6"));
			PET_RACE_MIN_PLAYER = Integer.parseInt(altSettings.getProperty("RaceMinPlayer", "2"));
			PET_RACE_MAX_LAP = Integer.parseInt(altSettings.getProperty("RaceMaxLap", "3"));
			DEATH_MATCH_MIN_PLAYER = Integer.parseInt(altSettings.getProperty("DeathMatchMinPlayer", "2"));
			USE_TOI_CHARM_ANYWHERE = Boolean.parseBoolean(altSettings.getProperty("UseToiCharmsAnywhere", "False"));
			ELEMENTAL_ENCHANTING = Boolean.parseBoolean(altSettings.getProperty("ElementalEnchanting", "False"));
			ELEMENTAL_ENCHANT_LIMIT = Integer.parseInt(altSettings.getProperty("ElementalEnchantLimit", "3"));
			ACCESSORY_ENCHANTING = Boolean.parseBoolean(altSettings.getProperty("AccessoryEnchanting", "False"));
			ACCESSORY_ENCHANT_LIMIT = Integer.parseInt(altSettings.getProperty("AccessoryEnchantLimit", "10"));
			ALT_PET_HUNGER_STATUS_CHANGE = Boolean
					.parseBoolean(altSettings.getProperty("PetHungerStatusChange", "True"));
		} catch (Exception e) {
			_log.error(e.getLocalizedMessage(), e);
			throw new Error("Failed to load " + ALT_SETTINGS_FILE + " file.");
		}

		// charsettings.properties
		_log.info("Loading CharSettings config.");
		try {
			Properties charSettings = new Properties();
			InputStream is = new FileInputStream(new File(CHAR_SETTINGS_CONFIG_FILE));
			charSettings.load(is);
			is.close();

			PRINCE_MAX_HP = Integer.parseInt(charSettings.getProperty("PrinceMaxHP", "1000"));
			PRINCE_MAX_MP = Integer.parseInt(charSettings.getProperty("PrinceMaxMP", "800"));
			KNIGHT_MAX_HP = Integer.parseInt(charSettings.getProperty("KnightMaxHP", "1400"));
			KNIGHT_MAX_MP = Integer.parseInt(charSettings.getProperty("KnightMaxMP", "600"));
			ELF_MAX_HP = Integer.parseInt(charSettings.getProperty("ElfMaxHP", "1000"));
			ELF_MAX_MP = Integer.parseInt(charSettings.getProperty("ElfMaxMP", "900"));
			WIZARD_MAX_HP = Integer.parseInt(charSettings.getProperty("WizardMaxHP", "800"));
			WIZARD_MAX_MP = Integer.parseInt(charSettings.getProperty("WizardMaxMP", "1200"));
			DARKELF_MAX_HP = Integer.parseInt(charSettings.getProperty("DarkelfMaxHP", "1000"));
			DARKELF_MAX_MP = Integer.parseInt(charSettings.getProperty("DarkelfMaxMP", "900"));
			DRAGONKNIGHT_MAX_HP = Integer.parseInt(charSettings.getProperty("DragonKnightMaxHP", "1400"));
			DRAGONKNIGHT_MAX_MP = Integer.parseInt(charSettings.getProperty("DragonKnightMaxMP", "600"));
			ILLUSIONIST_MAX_HP = Integer.parseInt(charSettings.getProperty("IllusionistMaxHP", "900"));
			ILLUSIONIST_MAX_MP = Integer.parseInt(charSettings.getProperty("IllusionistMaxMP", "1100"));
			LV50_EXP = Integer.parseInt(charSettings.getProperty("Lv50Exp", "1"));
			LV51_EXP = Integer.parseInt(charSettings.getProperty("Lv51Exp", "1"));
			LV52_EXP = Integer.parseInt(charSettings.getProperty("Lv52Exp", "1"));
			LV53_EXP = Integer.parseInt(charSettings.getProperty("Lv53Exp", "1"));
			LV54_EXP = Integer.parseInt(charSettings.getProperty("Lv54Exp", "1"));
			LV55_EXP = Integer.parseInt(charSettings.getProperty("Lv55Exp", "1"));
			LV56_EXP = Integer.parseInt(charSettings.getProperty("Lv56Exp", "1"));
			LV57_EXP = Integer.parseInt(charSettings.getProperty("Lv57Exp", "1"));
			LV58_EXP = Integer.parseInt(charSettings.getProperty("Lv58Exp", "1"));
			LV59_EXP = Integer.parseInt(charSettings.getProperty("Lv59Exp", "1"));
			LV60_EXP = Integer.parseInt(charSettings.getProperty("Lv60Exp", "1"));
			LV61_EXP = Integer.parseInt(charSettings.getProperty("Lv61Exp", "1"));
			LV62_EXP = Integer.parseInt(charSettings.getProperty("Lv62Exp", "1"));
			LV63_EXP = Integer.parseInt(charSettings.getProperty("Lv63Exp", "1"));
			LV64_EXP = Integer.parseInt(charSettings.getProperty("Lv64Exp", "1"));
			LV65_EXP = Integer.parseInt(charSettings.getProperty("Lv65Exp", "2"));
			LV66_EXP = Integer.parseInt(charSettings.getProperty("Lv66Exp", "2"));
			LV67_EXP = Integer.parseInt(charSettings.getProperty("Lv67Exp", "2"));
			LV68_EXP = Integer.parseInt(charSettings.getProperty("Lv68Exp", "2"));
			LV69_EXP = Integer.parseInt(charSettings.getProperty("Lv69Exp", "2"));
			LV70_EXP = Integer.parseInt(charSettings.getProperty("Lv70Exp", "4"));
			LV71_EXP = Integer.parseInt(charSettings.getProperty("Lv71Exp", "4"));
			LV72_EXP = Integer.parseInt(charSettings.getProperty("Lv72Exp", "4"));
			LV73_EXP = Integer.parseInt(charSettings.getProperty("Lv73Exp", "4"));
			LV74_EXP = Integer.parseInt(charSettings.getProperty("Lv74Exp", "4"));
			LV75_EXP = Integer.parseInt(charSettings.getProperty("Lv75Exp", "8"));
			LV76_EXP = Integer.parseInt(charSettings.getProperty("Lv76Exp", "8"));
			LV77_EXP = Integer.parseInt(charSettings.getProperty("Lv77Exp", "8"));
			LV78_EXP = Integer.parseInt(charSettings.getProperty("Lv78Exp", "8"));
			LV79_EXP = Integer.parseInt(charSettings.getProperty("Lv79Exp", "16"));
			LV80_EXP = Integer.parseInt(charSettings.getProperty("Lv80Exp", "32"));
			LV81_EXP = Integer.parseInt(charSettings.getProperty("Lv81Exp", "64"));
			LV82_EXP = Integer.parseInt(charSettings.getProperty("Lv82Exp", "128"));
			LV83_EXP = Integer.parseInt(charSettings.getProperty("Lv83Exp", "256"));
			LV84_EXP = Integer.parseInt(charSettings.getProperty("Lv84Exp", "512"));
			LV85_EXP = Integer.parseInt(charSettings.getProperty("Lv85Exp", "1024"));
			LV86_EXP = Integer.parseInt(charSettings.getProperty("Lv86Exp", "2048"));
			LV87_EXP = Integer.parseInt(charSettings.getProperty("Lv87Exp", "4096"));
			LV88_EXP = Integer.parseInt(charSettings.getProperty("Lv88Exp", "8192"));
			LV89_EXP = Integer.parseInt(charSettings.getProperty("Lv89Exp", "16384"));
			LV90_EXP = Integer.parseInt(charSettings.getProperty("Lv90Exp", "32768"));
			LV91_EXP = Integer.parseInt(charSettings.getProperty("Lv91Exp", "65536"));
			LV92_EXP = Integer.parseInt(charSettings.getProperty("Lv92Exp", "131072"));
			LV93_EXP = Integer.parseInt(charSettings.getProperty("Lv93Exp", "262144"));
			LV94_EXP = Integer.parseInt(charSettings.getProperty("Lv94Exp", "524288"));
			LV95_EXP = Integer.parseInt(charSettings.getProperty("Lv95Exp", "1048576"));
			LV96_EXP = Integer.parseInt(charSettings.getProperty("Lv96Exp", "2097152"));
			LV97_EXP = Integer.parseInt(charSettings.getProperty("Lv97Exp", "4194304"));
			LV98_EXP = Integer.parseInt(charSettings.getProperty("Lv98Exp", "8388608"));
			LV99_EXP = Integer.parseInt(charSettings.getProperty("Lv99Exp", "16777216"));
		} catch (Exception e) {
			_log.error(e.getLocalizedMessage(), e);
			throw new Error("Failed to load " + CHAR_SETTINGS_CONFIG_FILE + " file.");
		}
		_log.info("Loading PcCommandSettings config.");
		try {
			Properties pcommandSettings = new Properties();
			InputStream is = new FileInputStream(new File(PCOMMANDS_SETTINGS_FILE));
			pcommandSettings.load(is);
			is.close();

			PLAYER_COMMANDS = Boolean.parseBoolean(pcommandSettings.getProperty("PlayerCommands", "True"));
			PLAYER_BUFF = Boolean.parseBoolean(pcommandSettings.getProperty("PlayerBuff", "True"));
			POWER_BUFF = Boolean.parseBoolean(pcommandSettings.getProperty("PowerBuff", "False"));
			DK_BUFF = Boolean.parseBoolean(pcommandSettings.getProperty("DkBuff", "False"));

		} catch (Exception e) {
			_log.error(e.getLocalizedMessage(), e);
			throw new Error("Failed to load " + PCOMMANDS_SETTINGS_FILE + " file.");
		}
		validate();
	}

	private static void validate() {
		if (!IntRange.includes(Config.ALT_ITEM_DELETION_RANGE, 0, 5)) {
			throw new IllegalStateException("ItemDeletionRangel");
		}

		if (!IntRange.includes(Config.ALT_ITEM_DELETION_TIME, 1, 35791)) {
			throw new IllegalStateException("ItemDeletionTimel");
		}
	}

	public static boolean setParameterValue(String pName, String pValue) {
		// server.properties
		if (pName.equalsIgnoreCase("GameserverHostname")) {
			GAME_SERVER_HOST_NAME = pValue;
		} else if (pName.equalsIgnoreCase("GameserverPort")) {
			GAME_SERVER_PORT = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("Driver")) {
			DB_DRIVER = pValue;
		} else if (pName.equalsIgnoreCase("URL")) {
			DB_URL = pValue;
		} else if (pName.equalsIgnoreCase("Login")) {
			DB_LOGIN = pValue;
		} else if (pName.equalsIgnoreCase("Password")) {
			DB_PASSWORD = pValue;
		} else if (pName.equalsIgnoreCase("ClientLanguage")) {
			CLIENT_LANGUAGE = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("TimeZone")) {
			TIME_ZONE = pValue;
		} else if (pName.equalsIgnoreCase("AutomaticKick")) {
			AUTOMATIC_KICK = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("AutoCreateAccounts")) {
			AUTO_CREATE_ACCOUNTS = Boolean.parseBoolean(pValue);
		} else if (pName.equalsIgnoreCase("MaximumOnlineUsers")) {
			MAX_ONLINE_USERS = Short.parseShort(pValue);
		} else if (pName.equalsIgnoreCase("LoggingWeaponEnchant")) {
			LOGGING_WEAPON_ENCHANT = Byte.parseByte(pValue);
		} else if (pName.equalsIgnoreCase("LoggingArmorEnchant")) {
			LOGGING_ARMOR_ENCHANT = Byte.parseByte(pValue);
		} else if (pName.equalsIgnoreCase("CharacterConfigInServerSide")) {
			CHARACTER_CONFIG_IN_SERVER_SIDE = Boolean.parseBoolean(pValue);
		} else if (pName.equalsIgnoreCase("Allow2PC")) {
			CONNECTIONS_PER_IP = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("LevelDownRange")) {
			LEVEL_DOWN_RANGE = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("SendPacketBeforeTeleport")) {
			SEND_PACKET_BEFORE_TELEPORT = Boolean.parseBoolean(pValue);
		} else if (pName.equalsIgnoreCase("DropItems")) {
			STOP_DROP = Boolean.parseBoolean(pValue);
		}
		// rates.properties
		else if (pName.equalsIgnoreCase("RateHpRegen")) {
			RATE_HP_REGEN = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("RateMpRegen")) {
			RATE_MP_REGEN = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("RateHpCastle")) {
			RATE_HP_CASTLE = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("RateMpCastle")) {
			RATE_MP_CASTLE = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("RateHpHouse")) {
			RATE_HP_HOUSE = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("RateMpHouse")) {
			RATE_MP_HOUSE = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("RateHpHotel")) {
			RATE_HP_HOTEL = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("RateMpHotel")) {
			RATE_MP_HOTEL = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("RateHpMotherTree")) {
			RATE_HP_MOTHERTREE = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("RateMpMotherTree")) {
			RATE_MP_MOTHERTREE = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("PetExp")) {
			PETEXP_RATE = Double.parseDouble(pValue);
		} else if (pName.equalsIgnoreCase("PartyExp")) {
			PARTYEXP_RATE = Double.parseDouble(pValue);
		} else if (pName.equalsIgnoreCase("RateXp")) {
			RATE_XP = Double.parseDouble(pValue);
		} else if (pName.equalsIgnoreCase("RateLawful")) {
			RATE_LA = Double.parseDouble(pValue);
		} else if (pName.equalsIgnoreCase("RateKarma")) {
			RATE_KARMA = Double.parseDouble(pValue);
		} else if (pName.equalsIgnoreCase("RateDropAdena")) {
			RATE_DROP_ADENA = Double.parseDouble(pValue);
		} else if (pName.equalsIgnoreCase("RateDropItems")) {
			RATE_DROP_ITEMS = Double.parseDouble(pValue);
		} else if (pName.equalsIgnoreCase("EnchantChanceWeapon")) {
			ENCHANT_CHANCE_WEAPON = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("EnchantChanceArmor")) {
			ENCHANT_CHANCE_ARMOR = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("AttrEnchantChance")) {
			ATTR_ENCHANT_CHANCE = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("Weightrate")) {
			RATE_WEIGHT_LIMIT = Byte.parseByte(pValue);
		}
		// altsettings.properties
		else if (pName.equalsIgnoreCase("GlobalChatLevel")) {
			GLOBAL_CHAT_LEVEL = Short.parseShort(pValue);
		} else if (pName.equalsIgnoreCase("WhisperChatLevel")) {
			WHISPER_CHAT_LEVEL = Short.parseShort(pValue);
		} else if (pName.equalsIgnoreCase("AutoLoot")) {
			AUTO_LOOT = Byte.parseByte(pValue);
		} else if (pName.equalsIgnoreCase("LOOTING_RANGE")) {
			LOOTING_RANGE = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("AltNonPvP")) {
			ALT_NONPVP = Boolean.valueOf(pValue);
		} else if (pName.equalsIgnoreCase("AttackMessageOn")) {
			ALT_ATKMSG = Boolean.valueOf(pValue);
		} else if (pName.equalsIgnoreCase("ChangeTitleByOneself")) {
			CHANGE_TITLE_BY_ONESELF = Boolean.valueOf(pValue);
		} else if (pName.equalsIgnoreCase("MaxClanMember")) {
			MAX_CLAN_MEMBER = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("ClanAlliance")) {
			CLAN_ALLIANCE = Boolean.valueOf(pValue);
		} else if (pName.equalsIgnoreCase("MaxPT")) {
			MAX_PT = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("MaxChatPT")) {
			MAX_CHAT_PT = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("SimWarPenalty")) {
			SIM_WAR_PENALTY = Boolean.valueOf(pValue);
		} else if (pName.equalsIgnoreCase("GetBack")) {
			GET_BACK = Boolean.valueOf(pValue);
		} else if (pName.equalsIgnoreCase("AutomaticItemDeletionTime")) {
			ALT_ITEM_DELETION_TIME = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("AutomaticItemDeletionRange")) {
			ALT_ITEM_DELETION_RANGE = Byte.parseByte(pValue);
		} else if (pName.equalsIgnoreCase("GMshop")) {
			ALT_GMSHOP = Boolean.valueOf(pValue);
		} else if (pName.equalsIgnoreCase("GMshopMinID")) {
			ALT_GMSHOP_MIN_ID = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("GMshopMaxID")) {
			ALT_GMSHOP_MAX_ID = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("HalloweenEvent")) {
			ALT_HALLOWEENEVENT = Boolean.valueOf(pValue);
		} else if (pName.equalsIgnoreCase("HalloweenEventNpc")) {
			ALT_HALLOWEENEVENTNPC = Boolean.valueOf(pValue);
		} else if (pName.equalsIgnoreCase("PolyEvent")) {
			ALT_POLYEVENT = Boolean.valueOf(pValue);
		} else if (pName.equalsIgnoreCase("JpPrivileged")) {
			ALT_JPPRIVILEGED = Boolean.valueOf(pValue);
		} else if (pName.equalsIgnoreCase("TalkingScrollQuest")) {
			ALT_TALKINGSCROLLQUEST = Boolean.valueOf(pValue);
		} else if (pName.equalsIgnoreCase("HouseTaxInterval")) {
			HOUSE_TAX_INTERVAL = Integer.valueOf(pValue);
		} else if (pName.equalsIgnoreCase("MaxDollCount")) {
			MAX_DOLL_COUNT = Integer.valueOf(pValue);
		} else if (pName.equalsIgnoreCase("ReturnToNature")) {
			RETURN_TO_NATURE = Boolean.valueOf(pValue);
		} else if (pName.equalsIgnoreCase("MaxNpcItem")) {
			MAX_NPC_ITEM = Integer.valueOf(pValue);
		} else if (pName.equalsIgnoreCase("MaxPersonalWarehouseItem")) {
			MAX_PERSONAL_WAREHOUSE_ITEM = Integer.valueOf(pValue);
		} else if (pName.equalsIgnoreCase("MaxClanWarehouseItem")) {
			MAX_CLAN_WAREHOUSE_ITEM = Integer.valueOf(pValue);
		} else if (pName.equalsIgnoreCase("DeleteCharacterAfter7Days")) {
			DELETE_CHARACTER_AFTER_7DAYS = Boolean.valueOf(pValue);
		} else if (pName.equalsIgnoreCase("NpcDeletionTime")) {
			NPC_DELETION_TIME = Integer.valueOf(pValue);
		} else if (pName.equalsIgnoreCase("DefaultCharacterSlot")) {
			DEFAULT_CHARACTER_SLOT = Integer.valueOf(pValue);
		}
		// charsettings.properties
		else if (pName.equalsIgnoreCase("PrinceMaxHP")) {
			PRINCE_MAX_HP = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("PrinceMaxMP")) {
			PRINCE_MAX_MP = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("KnightMaxHP")) {
			KNIGHT_MAX_HP = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("KnightMaxMP")) {
			KNIGHT_MAX_MP = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("ElfMaxHP")) {
			ELF_MAX_HP = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("ElfMaxMP")) {
			ELF_MAX_MP = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("WizardMaxHP")) {
			WIZARD_MAX_HP = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("WizardMaxMP")) {
			WIZARD_MAX_MP = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("DarkelfMaxHP")) {
			DARKELF_MAX_HP = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("DarkelfMaxMP")) {
			DARKELF_MAX_MP = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("DragonKnightMaxHP")) {
			DRAGONKNIGHT_MAX_HP = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("DragonKnightMaxMP")) {
			DRAGONKNIGHT_MAX_MP = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("IllusionistMaxHP")) {
			ILLUSIONIST_MAX_HP = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("IllusionistMaxMP")) {
			ILLUSIONIST_MAX_MP = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("Lv50Exp")) {
			LV50_EXP = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("Lv51Exp")) {
			LV51_EXP = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("Lv52Exp")) {
			LV52_EXP = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("Lv53Exp")) {
			LV53_EXP = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("Lv54Exp")) {
			LV54_EXP = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("Lv55Exp")) {
			LV55_EXP = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("Lv56Exp")) {
			LV56_EXP = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("Lv57Exp")) {
			LV57_EXP = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("Lv58Exp")) {
			LV58_EXP = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("Lv59Exp")) {
			LV59_EXP = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("Lv60Exp")) {
			LV60_EXP = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("Lv61Exp")) {
			LV61_EXP = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("Lv62Exp")) {
			LV62_EXP = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("Lv63Exp")) {
			LV63_EXP = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("Lv64Exp")) {
			LV64_EXP = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("Lv65Exp")) {
			LV65_EXP = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("Lv66Exp")) {
			LV66_EXP = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("Lv67Exp")) {
			LV67_EXP = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("Lv68Exp")) {
			LV68_EXP = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("Lv69Exp")) {
			LV69_EXP = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("Lv70Exp")) {
			LV70_EXP = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("Lv71Exp")) {
			LV71_EXP = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("Lv72Exp")) {
			LV72_EXP = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("Lv73Exp")) {
			LV73_EXP = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("Lv74Exp")) {
			LV74_EXP = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("Lv75Exp")) {
			LV75_EXP = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("Lv76Exp")) {
			LV76_EXP = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("Lv77Exp")) {
			LV77_EXP = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("Lv78Exp")) {
			LV78_EXP = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("Lv79Exp")) {
			LV79_EXP = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("Lv80Exp")) {
			LV80_EXP = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("Lv81Exp")) {
			LV81_EXP = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("Lv82Exp")) {
			LV82_EXP = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("Lv83Exp")) {
			LV83_EXP = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("Lv84Exp")) {
			LV84_EXP = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("Lv85Exp")) {
			LV85_EXP = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("Lv86Exp")) {
			LV86_EXP = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("Lv87Exp")) {
			LV87_EXP = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("Lv88Exp")) {
			LV88_EXP = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("Lv89Exp")) {
			LV89_EXP = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("Lv90Exp")) {
			LV90_EXP = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("Lv91Exp")) {
			LV91_EXP = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("Lv92Exp")) {
			LV92_EXP = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("Lv93Exp")) {
			LV93_EXP = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("Lv94Exp")) {
			LV94_EXP = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("Lv95Exp")) {
			LV95_EXP = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("Lv96Exp")) {
			LV96_EXP = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("Lv97Exp")) {
			LV97_EXP = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("Lv98Exp")) {
			LV98_EXP = Integer.parseInt(pValue);
		} else if (pName.equalsIgnoreCase("Lv99Exp")) {
			LV99_EXP = Integer.parseInt(pValue);
		}
		// pcommand.properties
		else if (pName.equalsIgnoreCase("PlayerCommands")) {
			PLAYER_COMMANDS = Boolean.parseBoolean(pValue);
		} else if (pName.equalsIgnoreCase("PlayerBuff")) {
			PLAYER_BUFF = Boolean.parseBoolean(pValue);
		} else if (pName.equalsIgnoreCase("PowerBuff")) {
			POWER_BUFF = Boolean.parseBoolean(pValue);
		} else {
			return false;
		}
		return true;
	}

	/* Functions to reset the rates */
	public static void reset() {
		_log.info("Reloading rates config.");
		try {
			RATE_XP = RATE_XP_ORG;
			RATE_DROP_ADENA = RATE_DROP_ADENA_ORG;
			RATE_DROP_ITEMS = RATE_DROP_ITEMS_ORG;
			RATE_LA = RATE_LA_ORG;
			RATE_KARMA = RATE_KARMA_ORG;
			RATE_WEIGHT_LIMIT = RATE_WEIGHT_LIMIT_ORG;
		} catch (Exception e) {
			_log.error(e.toString());
		}
	}

	private Config() {
	}
}
