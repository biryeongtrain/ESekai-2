package kim.biryeong.esekai2.api.monster.affix;

/**
 * Resolved prefix and suffix roll counts for one monster affix roll operation.
 *
 * @param prefixCount number of prefixes to roll
 * @param suffixCount number of suffixes to roll
 */
public record MonsterAffixRollProfile(
        int prefixCount,
        int suffixCount
) {
    public MonsterAffixRollProfile {
        if (prefixCount < 0) {
            throw new IllegalArgumentException("prefixCount must be >= 0");
        }

        if (suffixCount < 0) {
            throw new IllegalArgumentException("suffixCount must be >= 0");
        }
    }
}
