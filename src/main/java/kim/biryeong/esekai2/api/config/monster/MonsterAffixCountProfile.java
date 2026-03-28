package kim.biryeong.esekai2.api.config.monster;

/**
 * Resolved monster affix count profile for one rarity.
 *
 * @param prefixCount number of prefix affixes to roll
 * @param suffixCount number of suffix affixes to roll
 */
public record MonsterAffixCountProfile(
        int prefixCount,
        int suffixCount
) {
    public MonsterAffixCountProfile {
        if (prefixCount < 0) {
            throw new IllegalArgumentException("prefixCount must be >= 0");
        }

        if (suffixCount < 0) {
            throw new IllegalArgumentException("suffixCount must be >= 0");
        }
    }
}
