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
package l1j.server.server.model.shop;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l1j.server.Config;
import l1j.server.server.datatables.CastleTable;
import l1j.server.server.datatables.ItemTable;
import l1j.server.server.datatables.TownTable;
import l1j.server.server.log.LogShopBuy;
import l1j.server.server.log.LogShopSell;
import l1j.server.server.model.L1CastleLocation;
import l1j.server.server.model.L1PcInventory;
import l1j.server.server.model.L1TaxCalculator;
import l1j.server.server.model.L1TownLocation;
import l1j.server.server.model.L1World;
import l1j.server.server.model.Instance.L1ItemInstance;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.model.item.L1ItemId;
import l1j.server.server.serverpackets.S_ServerMessage;
import l1j.server.server.templates.L1Castle;
import l1j.server.server.templates.L1Item;
import l1j.server.server.templates.L1ShopItem;
import l1j.server.server.utils.IntRange;

public class L1Shop {

	private static Logger _log = LoggerFactory.getLogger(L1Shop.class.getName());

	private final int _npcId;
	private final List<L1ShopItem> _sellingItems;
	private final List<L1ShopItem> _purchasingItems;

	public L1Shop(int npcId, List<L1ShopItem> sellingItems, List<L1ShopItem> purchasingItems) {
		if (sellingItems == null || purchasingItems == null) {
			throw new NullPointerException();
		}
		_npcId = npcId;
		_sellingItems = sellingItems;
		_purchasingItems = purchasingItems;
	}

	public int getNpcId() {
		return _npcId;
	}

	public List<L1ShopItem> getSellingItems() {
		return _sellingItems;
	}

	private boolean isPurchaseableItem(L1ItemInstance item) {
		if ((item == null) || item.isEquipped() || (item.getEnchantLevel() != 0) || (item.getBless() >= 128)) {
			return false;
		}
		return true;
	}

	private L1ShopItem getPurchasingItem(int itemId) {
		for (L1ShopItem shopItem : _purchasingItems) {
			if (shopItem.getItemId() == itemId) {
				return shopItem;
			}
		}
		return null;
	}

	public L1AssessedItem assessItem(L1ItemInstance item) {
		L1ShopItem shopItem = getPurchasingItem(item.getItemId());
		if (shopItem == null) {
			return null;
		}
		return new L1AssessedItem(item.getId(), getAssessedPrice(shopItem));
	}

	private int getAssessedPrice(L1ShopItem item) {
		return (int) (item.getPrice() * Config.RATE_SHOP_PURCHASING_PRICE / item.getPackCount());
	}

	public List<L1AssessedItem> assessItems(L1PcInventory inv) {
		List<L1AssessedItem> result = new ArrayList<>();
		for (L1ShopItem item : _purchasingItems) {
			for (L1ItemInstance targetItem : inv.findItemsId(item.getItemId())) {
				if (!isPurchaseableItem(targetItem)) {
					continue;
				}

				result.add(new L1AssessedItem(targetItem.getId(), getAssessedPrice(item)));
			}
		}
		return result;
	}

	private boolean ensureSell(L1PcInstance pc, L1ShopBuyOrderList orderList) {
		int price = orderList.getTotalPriceTaxIncluded();
		if (!IntRange.includes(price, 0, 2000000000)) {
			pc.sendPackets(new S_ServerMessage(904, "2000000000"));
			return false;
		}
		if (!pc.getInventory().checkItem(L1ItemId.ADENA, price)) {
			pc.sendPackets(new S_ServerMessage(189));
			return false;
		}
		int currentWeight = pc.getInventory().getWeight() * 1000;
		if (currentWeight + orderList.getTotalWeight() > pc.getMaxWeight() * 1000) {
			pc.sendPackets(new S_ServerMessage(82));
			return false;
		}
		int totalCount = pc.getInventory().getSize();
		for (L1ShopBuyOrder order : orderList.getList()) {
			L1Item temp = order.getItem().getItem();
			if (temp.isStackable()) {
				if (!pc.getInventory().checkItem(temp.getItemId())) {
					totalCount += 1;
				}
			} else {
				totalCount += 1;
			}
		}
		if (totalCount > 180) {
			pc.sendPackets(new S_ServerMessage(263));
			return false;
		}
		return true;
	}

	private void payCastleTax(L1ShopBuyOrderList orderList) {
		L1TaxCalculator calc = orderList.getTaxCalculator();

		int castleId = L1CastleLocation.getCastleIdByNpcid(_npcId);
		int castleTax = calc.calcCastleTaxPrice(orderList);
		int nationalTax = calc.calcNationalTaxPrice(orderList);
		if (castleId == L1CastleLocation.ADEN_CASTLE_ID || castleId == L1CastleLocation.DIAD_CASTLE_ID) {
			castleTax += nationalTax;
			nationalTax = 0;
		}

		if (castleId != 0 && castleTax > 0) {
			L1Castle castle = CastleTable.getInstance().getCastleTable(castleId);

			synchronized (castle) {
				int money = castle.getPublicMoney();
				if (2000000000 > money) {
					money = money + castleTax;
					castle.setPublicMoney(money);
					CastleTable.getInstance().updateCastle(castle);
				}
			}

			if (nationalTax > 0) {
				L1Castle aden = CastleTable.getInstance().getCastleTable(L1CastleLocation.ADEN_CASTLE_ID);
				synchronized (aden) {
					int money = aden.getPublicMoney();
					if (2000000000 > money) {
						money = money + nationalTax;
						aden.setPublicMoney(money);
						CastleTable.getInstance().updateCastle(aden);
					}
				}
			}
		}
	}

	private void payDiadTax(L1ShopBuyOrderList orderList) {
		L1TaxCalculator calc = orderList.getTaxCalculator();

		int diadTax = calc.calcDiadTaxPrice(orderList);
		if (diadTax <= 0) {
			return;
		}

		L1Castle castle = CastleTable.getInstance().getCastleTable(L1CastleLocation.DIAD_CASTLE_ID);
		synchronized (castle) {
			int money = castle.getPublicMoney();
			if (2000000000 > money) {
				money = money + diadTax;
				castle.setPublicMoney(money);
				CastleTable.getInstance().updateCastle(castle);
			}
		}
	}

	private void payTownTax(L1ShopBuyOrderList orderList) {
		if (!L1World.getInstance().isProcessingContributionTotal()) {
			int town_id = L1TownLocation.getTownIdByNpcid(_npcId);
			if (town_id >= 1 && town_id <= 10) {
				TownTable.getInstance().addSalesMoney(town_id, orderList);
			}
		}
	}

	private void payTax(L1ShopBuyOrderList orderList) {
		payCastleTax(orderList);
		payTownTax(orderList);
		payDiadTax(orderList);
	}

	private void sellItems(L1PcInventory inv, L1ShopBuyOrderList orderList, L1PcInstance pc) {
		int adenabefore = 0;
		int adenaafter = 0;

		L1ItemInstance pcitem = pc.getInventory().findItemId(40308);
		if (pcitem != null) {
			adenabefore = pcitem.getCount();
		}

		if (!inv.consumeItem(L1ItemId.ADENA, orderList.getTotalPriceTaxIncluded()) && !pc.isGm()) {
			throw new IllegalStateException("Unable to consume required adena.");
		}

		if (pcitem != null) {
			adenaafter = pcitem.getCount();
		}
		for (L1ShopBuyOrder order : orderList.getList()) {
			int itemId = order.getItem().getItemId();
			int amount = order.getCount();
			L1ItemInstance item = ItemTable.getInstance().createItem(itemId);
			item.setCount(amount);
			item.setIdentified(true);
			item.setEnchantLevel(order.getItem().getEnchantLevel());
			inv.storeItem(item);
			if (_npcId == 70068 || _npcId == 70020) {
				item.setIdentified(false);
				new Random();
				int chance = ThreadLocalRandom.current().nextInt(100) + 1;
				if (chance <= 15) {
					item.setEnchantLevel(-2);
				} else if (chance >= 16 && chance <= 30) {
					item.setEnchantLevel(-1);
				} else if (chance >= 31 && chance <= 70) {
					item.setEnchantLevel(0);
				} else if (chance >= 71 && chance <= 87) {
					item.setEnchantLevel(ThreadLocalRandom.current().nextInt(2) + 1);
				} else if (chance >= 88 && chance <= 97) {
					item.setEnchantLevel(ThreadLocalRandom.current().nextInt(3) + 3);
				} else if (chance >= 98 && chance <= 99) {
					item.setEnchantLevel(6);
				} else if (chance == 100) {
					item.setEnchantLevel(7);
				}
			}
			LogShopBuy lsb = new LogShopBuy();
			try {
				lsb.storeLogShopBuy(pc, item, amount, adenabefore, adenaafter, orderList.getTotalPriceTaxIncluded());
			} catch (Exception e) {
				_log.warn("Problem with storeLogShopBuy");
				_log.warn(e.toString());
			}
		}
	}

	public void sellItems(L1PcInstance pc, L1ShopBuyOrderList orderList) {
		if (!ensureSell(pc, orderList)) {
			return;
		}
		sellItems(pc.getInventory(), orderList, pc);
		payTax(orderList);
	}

	public void buyItems(L1ShopSellOrderList orderList) {

		LogShopSell lsb = new LogShopSell();

		L1PcInstance pc = orderList.getPc();
		L1PcInventory inv = orderList.getPc().getInventory();
		int totalPrice = 0;
		int adenabefore = pc.getInventory().findItemId(40308).getCount();
		int adenaafter = 0;
		for (L1ShopSellOrder order : orderList.getList()) {
			L1ItemInstance sellme = inv.getItem(order.getItem().getTargetId());
			int count = inv.removeItem(order.getItem().getTargetId(), order.getCount());
			totalPrice += order.getItem().getAssessedPrice() * count;
			adenaafter = adenabefore + (order.getItem().getAssessedPrice() * count);
			// lsb.storeLogShopSell(pc, item, adenabefore, adenaafter,
			// itemprice)
			try {
				lsb.storeLogShopSell(pc, sellme, adenabefore, adenaafter, order.getItem().getAssessedPrice() * count);
			} catch (Exception e) {
				_log.warn("Problem with storeLogShopSell");
				_log.warn(e.toString());
			}
			adenabefore = adenaafter;
		}
		totalPrice = IntRange.ensure(totalPrice, 0, 2000000000);
		if (0 < totalPrice) {
			inv.storeItem(L1ItemId.ADENA, totalPrice);
		}
	}

	public L1ShopBuyOrderList newBuyOrderList() {
		return new L1ShopBuyOrderList(this);
	}

	public L1ShopSellOrderList newSellOrderList(L1PcInstance pc) {
		return new L1ShopSellOrderList(this, pc);
	}
}