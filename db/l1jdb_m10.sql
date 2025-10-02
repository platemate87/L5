-- ----------------------------
-- Table structure for character_offline_shop
-- ----------------------------
DROP TABLE IF EXISTS `character_offline_shop`;
CREATE TABLE `character_offline_shop` (
  `char_id` int(11) NOT NULL,
  `char_name` varchar(45) NOT NULL,
  `map_id` smallint(5) unsigned NOT NULL,
  `locx` int(11) NOT NULL,
  `locy` int(11) NOT NULL,
  `heading` tinyint(3) unsigned NOT NULL,
  `shop_chat` blob,
  `sell_list` text,
  `buy_list` text,
  `client_ip` varchar(45) DEFAULT NULL,
  PRIMARY KEY (`char_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `commands` VALUES ('offlinekick', '200', 'L1OfflineShopDisconnect', 'Disconnect all active offline private shops.', '0');

