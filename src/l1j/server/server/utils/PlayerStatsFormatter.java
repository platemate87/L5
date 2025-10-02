package l1j.server.server.utils;

import static l1j.server.server.model.skill.L1SkillId.COOKING_2_0_N;
import static l1j.server.server.model.skill.L1SkillId.COOKING_2_0_S;
import static l1j.server.server.model.skill.L1SkillId.COOKING_2_3_N;
import static l1j.server.server.model.skill.L1SkillId.COOKING_2_3_S;
import static l1j.server.server.model.skill.L1SkillId.COOKING_3_0_N;
import static l1j.server.server.model.skill.L1SkillId.COOKING_3_0_S;
import static l1j.server.server.model.skill.L1SkillId.COOKING_3_2_N;
import static l1j.server.server.model.skill.L1SkillId.COOKING_3_2_S;
import static l1j.server.server.model.skill.L1SkillId.SOUL_OF_FLAME;

import l1j.server.Config;
import l1j.server.server.model.Instance.L1DollInstance;
import l1j.server.server.model.Instance.L1ItemInstance;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.model.item.L1ItemId;
import l1j.server.server.model.item.WeaponType;
import l1j.server.server.templates.L1Item;
import l1j.server.server.utils.CalcStat;

public final class PlayerStatsFormatter {
        private static final int[] STR_HIT = { -2, -2, -2, -2, -2, -2, -2, -2, -1, -1, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 5,
                        6, 6, 6, 7, 7, 7, 8, 8, 8, 9, 9, 9, 10, 10, 10, 11, 11, 11, 12, 12, 12, 13, 13, 13, 14, 14, 14, 15, 15,
                        15, 16, 16, 16, 17, 17, 17 };

        private static final int[] DEX_HIT = { -2, -2, -2, -2, -2, -2, -1, -1, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 6, 7, 8, 9, 10,
                        11, 12, 13, 14, 15, 16, 17, 18, 19, 19, 19, 20, 20, 20, 21, 21, 21, 22, 22, 22, 23, 23, 23, 24, 24, 24,
                        25, 25, 25, 26, 26, 26, 27, 27, 27, 28 };

        private static final int[] STR_DMG = new int[128];
        private static final int[] DEX_DMG = new int[128];

        static {
                int dmg = -6;
                for (int str = 0; str <= 22; str++) {
                        if (str % 2 == 1) {
                                dmg++;
                        }
                        STR_DMG[str] = dmg;
                }
                for (int str = 23; str <= 28; str++) {
                        if (str % 3 == 2) {
                                dmg++;
                        }
                        STR_DMG[str] = dmg;
                }
                for (int str = 29; str <= 32; str++) {
                        if (str % 2 == 1) {
                                dmg++;
                        }
                        STR_DMG[str] = dmg;
                }
                for (int str = 33; str <= 34; str++) {
                        dmg++;
                        STR_DMG[str] = dmg;
                }
                for (int str = 35; str <= 127; str++) {
                        if ((str + 1) % 4 == 0) {
                                dmg++;
                        }
                        STR_DMG[str] = dmg;
                }

                for (int dex = 0; dex <= 14; dex++) {
                        DEX_DMG[dex] = 0;
                }
                DEX_DMG[15] = 1;
                DEX_DMG[16] = 2;
                DEX_DMG[17] = 3;
                DEX_DMG[18] = 4;
                DEX_DMG[19] = 4;
                DEX_DMG[20] = 4;
                DEX_DMG[21] = 5;
                DEX_DMG[22] = 5;
                DEX_DMG[23] = 5;
                dmg = 5;
                for (int dex = 24; dex <= 35; dex++) {
                        if (dex % 3 == 0) {
                                dmg++;
                        }
                        DEX_DMG[dex] = dmg;
                }
                for (int dex = 36; dex <= 127; dex++) {
                        if (dex % 4 == 0) {
                                dmg++;
                        }
                        DEX_DMG[dex] = dmg;
                }
        }

        private PlayerStatsFormatter() {
        }

        public static String format(L1PcInstance player) {
                CombatSnapshot snapshot = buildCombatSnapshot(player);
                return String.format(
                                "%s | ER: %d | WIS bonus MR: %d | HPR: %d | MPR: %d | Hit: +%d | Dmg (Small): %d-%d | Dmg (Large): %d-%d | Base Magic Hit: +%d | Magic Damage Bonus: +%d | Magic Crit: %d",
                                player.getName(), player.getEr(), CalcStat.calcStatMr(player.getWis()),
                                player.getHpr() + player.getInventory().hpRegenPerTick(),
                                player.getMpr() + player.getInventory().mpRegenPerTick(), snapshot.hitRate, snapshot.small.min,
                                snapshot.small.max, snapshot.large.min, snapshot.large.max, player.getOriginalMagicHit(),
                                player.getOriginalMagicDamage(), player.getOriginalMagicCritical());
        }

        private static CombatSnapshot buildCombatSnapshot(L1PcInstance player) {
                WeaponSnapshot weapon = buildWeaponSnapshot(player);
                int hitRate = computeHitRate(player, weapon);
                DamageRange small = computeDamageRange(player, weapon, true);
                DamageRange large = computeDamageRange(player, weapon, false);
                return new CombatSnapshot(hitRate, small, large);
        }

        private static DamageRange computeDamageRange(L1PcInstance player, WeaponSnapshot weapon, boolean smallTarget) {
                int weaponBase = smallTarget ? weapon.weaponSmall : weapon.weaponLarge;
                int weaponMin = 0;
                int weaponMax = 0;

                if (weapon.isRanged || weapon.weaponType == WeaponType.Fist) {
                        weaponMin = 0;
                        weaponMax = 0;
                } else if (weaponBase > 0) {
                        weaponMax = weaponBase;
                        if (player.hasSkillEffect(SOUL_OF_FLAME)) {
                                weaponMin = weaponBase;
                        } else {
                                weaponMin = 1;
                        }
                }

                int projectileMin = 0;
                int projectileMax = 0;
                if (weapon.isBow) {
                        if (weapon.arrow != null) {
                                int arrowBase = smallTarget ? weapon.arrow.getItem().getDmgSmall()
                                                : weapon.arrow.getItem().getDmgLarge();
                                if (arrowBase <= 0) {
                                        arrowBase = 1;
                                }
                                projectileMin = 1;
                                projectileMax = arrowBase;
                        } else if (weapon.weaponId == L1ItemId.SAYHAS_BOW) {
                                projectileMin = 1;
                                projectileMax = 15;
                        }
                } else if (weapon.isGauntlet && weapon.sting != null) {
                        int stingBase = smallTarget ? weapon.sting.getItem().getDmgSmall()
                                        : weapon.sting.getItem().getDmgLarge();
                        if (stingBase <= 0) {
                                stingBase = 1;
                        }
                        projectileMin = 1;
                        projectileMax = stingBase;
                }

                int additive = weapon.weaponAddDmg + weapon.weaponEnchant + weapon.statusDamage + weapon.additionalDamage;
                int min = weaponMin + projectileMin + additive;
                int max = weaponMax + projectileMax + additive;

                if (weapon.weaponType == WeaponType.Fist) {
                        min = 1;
                        max = 2;
                }

                if (min < 0) {
                        min = 0;
                }
                if (max < min) {
                        max = min;
                }

                return new DamageRange(min, max);
        }

        private static int computeHitRate(L1PcInstance player, WeaponSnapshot weapon) {
                int hitRate = player.getLevel();
                hitRate += getStrHitBonus(player.getStr());
                hitRate += getDexHitBonus(player.getDex());
                hitRate += weapon.weaponAddHit + weapon.weaponEnchant / 2;
                if (weapon.isRanged) {
                        hitRate += player.getBowHitup() + player.getOriginalBowHitup() + player.getBowHitModifierByArmor();
                } else {
                        hitRate += player.getHitup() + player.getOriginalHitup() + player.getHitModifierByArmor();
                }
                hitRate += getWeightHitModifier(player);
                hitRate += getCookingHitModifier(player, weapon.isRanged);
                hitRate += getDollHitModifier(player, weapon.isRanged);
                return hitRate;
        }

        private static WeaponSnapshot buildWeaponSnapshot(L1PcInstance player) {
                WeaponSnapshot snapshot = new WeaponSnapshot();
                L1ItemInstance weapon = player.getWeapon();

                snapshot.weaponType = WeaponType.Fist;
                snapshot.weaponId = 0;
                snapshot.weaponSmall = 0;
                snapshot.weaponLarge = 0;
                snapshot.weaponAddHit = 0;
                snapshot.weaponAddDmg = 0;
                snapshot.weaponEnchant = 0;
                snapshot.isBow = false;
                snapshot.isGauntlet = false;
                snapshot.isRanged = false;
                snapshot.attrEnchantLevel = 0;

                if (weapon != null) {
                        L1Item item = weapon.getItem();
                        snapshot.weaponId = item.getItemId();
                        snapshot.weaponType = item.getType1();
                        snapshot.weaponSmall = item.getDmgSmall();
                        snapshot.weaponLarge = item.getDmgLarge();
                        snapshot.weaponAddHit = item.getHitModifier() + weapon.getHitByMagic();
                        snapshot.weaponAddDmg = item.getDmgModifier() + weapon.getDmgByMagic();
                        snapshot.isBow = snapshot.weaponType == WeaponType.Bow;
                        snapshot.isGauntlet = snapshot.weaponType == WeaponType.Gauntlet;
                        snapshot.isRanged = snapshot.isBow || snapshot.isGauntlet;
                        snapshot.weaponEnchant = weapon.getEnchantLevel() - (snapshot.isRanged ? 0 : weapon.get_durability());
                        if (snapshot.isBow) {
                                snapshot.arrow = player.getInventory().getArrow();
                        } else if (snapshot.isGauntlet) {
                                snapshot.sting = player.getInventory().getSting();
                        }
                        snapshot.attrEnchantLevel = weapon.getAttrEnchantLevel();
                }

                snapshot.statusDamage = snapshot.isBow ? getDexDamage(player.getDex()) : getStrDamage(player.getStr());
                snapshot.attrEnchantDamage = Config.ELEMENTAL_ENCHANTING && snapshot.attrEnchantLevel > 0
                                ? snapshot.attrEnchantLevel * 2 - 1
                                : 0;

                int damageBonus = snapshot.isRanged
                                ? player.getBowDmgup() + player.getOriginalBowDmgup() + player.getBowDmgModifierByArmor()
                                : player.getDmgup() + player.getOriginalDmgup() + player.getDmgModifierByArmor();
                damageBonus += getCookingDmgModifier(player, snapshot.isRanged);
                damageBonus += getDollDmgModifier(player, snapshot.isRanged);
                damageBonus += snapshot.attrEnchantDamage;
                snapshot.additionalDamage = damageBonus;

                return snapshot;
        }

        private static int getWeightHitModifier(final L1PcInstance pc) {
                int weightModifier = 0;
                int currentWeight = pc.getInventory().getWeight240();
                if (80 < currentWeight && 120 >= currentWeight) {
                        weightModifier = -1;
                } else if (121 <= currentWeight && 160 >= currentWeight) {
                        weightModifier = -3;
                } else if (161 <= currentWeight && 200 >= currentWeight) {
                        weightModifier = -5;
                }
                return weightModifier;
        }

        private static int getCookingHitModifier(final L1PcInstance pc, final boolean ranged) {
                int cookingModifier = 0;
                if (!ranged && (pc.hasSkillEffect(COOKING_2_0_N) || pc.hasSkillEffect(COOKING_2_0_S))) {
                        cookingModifier += 1;
                }
                if (!ranged && (pc.hasSkillEffect(COOKING_3_2_N) || pc.hasSkillEffect(COOKING_3_2_S))) {
                        cookingModifier += 2;
                }
                if (ranged && (pc.hasSkillEffect(COOKING_2_3_N) || pc.hasSkillEffect(COOKING_2_3_S)
                                || pc.hasSkillEffect(COOKING_3_0_N) || pc.hasSkillEffect(COOKING_3_0_S))) {
                        cookingModifier += 1;
                }
                return cookingModifier;
        }

        private static int getCookingDmgModifier(final L1PcInstance pc, final boolean ranged) {
                int damage = 0;
                if (!ranged && (pc.hasSkillEffect(COOKING_2_0_N) || pc.hasSkillEffect(COOKING_2_0_S)
                                || pc.hasSkillEffect(COOKING_3_2_N) || pc.hasSkillEffect(COOKING_3_2_S))) {
                        damage += 1;
                }
                if (ranged && (pc.hasSkillEffect(COOKING_2_3_N) || pc.hasSkillEffect(COOKING_2_3_S)
                                || pc.hasSkillEffect(COOKING_3_0_N) || pc.hasSkillEffect(COOKING_3_0_S))) {
                        damage += 1;
                }
                return damage;
        }

        private static int getDollHitModifier(final L1PcInstance pc, final boolean ranged) {
                int hitRate = 0;
                for (Object dollObject : pc.getDollList().values()) {
                        L1DollInstance doll = (L1DollInstance) dollObject;
                        hitRate += ranged ? doll.getRangedHitByDoll() : doll.getMeleeHitByDoll();
                }
                return hitRate;
        }

        private static int getDollDmgModifier(final L1PcInstance pc, final boolean ranged) {
                int damage = 0;
                for (Object dollObject : pc.getDollList().values()) {
                        L1DollInstance doll = (L1DollInstance) dollObject;
                        damage += ranged ? doll.getRangedDmgByDoll() : doll.getMeleeDmgByDoll();
                }
                return damage;
        }

        private static int getStrHitBonus(int str) {
                if (str <= 0) {
                        return STR_HIT[0];
                }
                int index = Math.min(STR_HIT.length - 1, str - 1);
                return STR_HIT[index];
        }

        private static int getDexHitBonus(int dex) {
                if (dex <= 0) {
                        return DEX_HIT[0];
                }
                int index = Math.min(DEX_HIT.length - 1, dex - 1);
                return DEX_HIT[index];
        }

        private static int getStrDamage(int str) {
                if (str < 0) {
                        str = 0;
                }
                if (str >= STR_DMG.length) {
                        str = STR_DMG.length - 1;
                }
                return STR_DMG[str];
        }

        private static int getDexDamage(int dex) {
                if (dex < 0) {
                        dex = 0;
                }
                if (dex >= DEX_DMG.length) {
                        dex = DEX_DMG.length - 1;
                }
                return DEX_DMG[dex];
        }

        private static final class CombatSnapshot {
                private final int hitRate;
                private final DamageRange small;
                private final DamageRange large;

                private CombatSnapshot(int hitRate, DamageRange small, DamageRange large) {
                        this.hitRate = hitRate;
                        this.small = small;
                        this.large = large;
                }
        }

        private static final class DamageRange {
                private final int min;
                private final int max;

                private DamageRange(int min, int max) {
                        this.min = min;
                        this.max = max;
                }
        }

        private static final class WeaponSnapshot {
                private int weaponId;
                private int weaponType;
                private int weaponSmall;
                private int weaponLarge;
                private int weaponAddHit;
                private int weaponAddDmg;
                private int weaponEnchant;
                private boolean isBow;
                private boolean isGauntlet;
                private boolean isRanged;
                private L1ItemInstance arrow;
                private L1ItemInstance sting;
                private int statusDamage;
                private int attrEnchantLevel;
                private int attrEnchantDamage;
                private int additionalDamage;
        }
}
