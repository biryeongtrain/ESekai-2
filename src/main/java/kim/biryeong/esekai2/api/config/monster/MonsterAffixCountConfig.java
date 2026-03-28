package kim.biryeong.esekai2.api.config.monster;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kim.biryeong.esekai2.api.monster.level.MonsterRarity;

/**
 * Server-tunable monster affix counts grouped by rarity and affix kind.
 *
 * @param normalPrefixes prefix count for normal monsters
 * @param normalSuffixes suffix count for normal monsters
 * @param magicPrefixes prefix count for magic monsters
 * @param magicSuffixes suffix count for magic monsters
 * @param rarePrefixes prefix count for rare monsters
 * @param rareSuffixes suffix count for rare monsters
 * @param uniquePrefixes prefix count for unique monsters
 * @param uniqueSuffixes suffix count for unique monsters
 */
public record MonsterAffixCountConfig(
        int normalPrefixes,
        int normalSuffixes,
        int magicPrefixes,
        int magicSuffixes,
        int rarePrefixes,
        int rareSuffixes,
        int uniquePrefixes,
        int uniqueSuffixes
) {
    public static final MonsterAffixCountConfig DEFAULT = new MonsterAffixCountConfig(0, 0, 1, 1, 1, 1, 0, 0);

    private static final Codec<MonsterAffixCountConfig> BASE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("normal_prefixes", DEFAULT.normalPrefixes()).forGetter(MonsterAffixCountConfig::normalPrefixes),
            Codec.INT.optionalFieldOf("normal_suffixes", DEFAULT.normalSuffixes()).forGetter(MonsterAffixCountConfig::normalSuffixes),
            Codec.INT.optionalFieldOf("magic_prefixes", DEFAULT.magicPrefixes()).forGetter(MonsterAffixCountConfig::magicPrefixes),
            Codec.INT.optionalFieldOf("magic_suffixes", DEFAULT.magicSuffixes()).forGetter(MonsterAffixCountConfig::magicSuffixes),
            Codec.INT.optionalFieldOf("rare_prefixes", DEFAULT.rarePrefixes()).forGetter(MonsterAffixCountConfig::rarePrefixes),
            Codec.INT.optionalFieldOf("rare_suffixes", DEFAULT.rareSuffixes()).forGetter(MonsterAffixCountConfig::rareSuffixes),
            Codec.INT.optionalFieldOf("unique_prefixes", DEFAULT.uniquePrefixes()).forGetter(MonsterAffixCountConfig::uniquePrefixes),
            Codec.INT.optionalFieldOf("unique_suffixes", DEFAULT.uniqueSuffixes()).forGetter(MonsterAffixCountConfig::uniqueSuffixes)
    ).apply(instance, MonsterAffixCountConfig::new));

    /**
     * Validated codec used by the server config layer and GameTests.
     */
    public static final Codec<MonsterAffixCountConfig> CODEC = BASE_CODEC.validate(MonsterAffixCountConfig::validate);

    public MonsterAffixCountConfig {
        requireNonNegative(normalPrefixes, "normalPrefixes");
        requireNonNegative(normalSuffixes, "normalSuffixes");
        requireNonNegative(magicPrefixes, "magicPrefixes");
        requireNonNegative(magicSuffixes, "magicSuffixes");
        requireNonNegative(rarePrefixes, "rarePrefixes");
        requireNonNegative(rareSuffixes, "rareSuffixes");
        requireNonNegative(uniquePrefixes, "uniquePrefixes");
        requireNonNegative(uniqueSuffixes, "uniqueSuffixes");
    }

    /**
     * Resolves the configured prefix and suffix counts for the requested monster rarity.
     *
     * @param rarity monster rarity to inspect
     * @return resolved count profile for that rarity
     */
    public MonsterAffixCountProfile profile(MonsterRarity rarity) {
        return switch (rarity) {
            case NORMAL -> new MonsterAffixCountProfile(normalPrefixes, normalSuffixes);
            case MAGIC -> new MonsterAffixCountProfile(magicPrefixes, magicSuffixes);
            case RARE -> new MonsterAffixCountProfile(rarePrefixes, rareSuffixes);
            case UNIQUE -> new MonsterAffixCountProfile(uniquePrefixes, uniqueSuffixes);
        };
    }

    private static DataResult<MonsterAffixCountConfig> validate(MonsterAffixCountConfig config) {
        if (config.normalPrefixes() < 0 || config.normalSuffixes() < 0
                || config.magicPrefixes() < 0 || config.magicSuffixes() < 0
                || config.rarePrefixes() < 0 || config.rareSuffixes() < 0
                || config.uniquePrefixes() < 0 || config.uniqueSuffixes() < 0) {
            return DataResult.error(() -> "monster affix count config values must be >= 0");
        }

        return DataResult.success(config);
    }

    private static void requireNonNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must be >= 0");
        }
    }
}
