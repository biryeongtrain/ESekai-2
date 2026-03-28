package kim.biryeong.esekai2.api.monster.level;

/**
 * Resolved runtime profile produced from a monster level table row and runtime scaling context.
 *
 * @param level resolved monster level
 * @param baseDamage base damage value from the table
 * @param baseLife base life value from the table
 * @param baseSummonLife base summon life value from the table
 * @param baseAccuracy base accuracy value from the table
 * @param baseEvasion base evasion value from the table
 * @param effectiveLifeMultiplier final multiplicative life scalar after rarity and map bonuses
 * @param effectiveDamageMultiplier final multiplicative damage scalar after map bonuses
 * @param experiencePoints experience points value from the table
 * @param droppedItemLevel derived item level for drops produced by this monster
 * @param bossItemQuantityBonusPercent boss-only item quantity bonus applied in maps
 * @param bossItemRarityBonusPercent boss-only item rarity bonus applied in maps
 */
public record MonsterLevelProfile(
        int level,
        double baseDamage,
        double baseLife,
        double baseSummonLife,
        double baseAccuracy,
        double baseEvasion,
        double effectiveLifeMultiplier,
        double effectiveDamageMultiplier,
        double experiencePoints,
        int droppedItemLevel,
        double bossItemQuantityBonusPercent,
        double bossItemRarityBonusPercent
) {
}
