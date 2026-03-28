package kim.biryeong.esekai2.api.config.monster;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Root server config currently used by the monster affix runtime.
 *
 * @param monsterAffixCounts rarity-based monster affix count tuning
 */
public record MonsterAffixConfig(
        MonsterAffixCountConfig monsterAffixCounts
) {
    public static final MonsterAffixConfig DEFAULT = new MonsterAffixConfig(MonsterAffixCountConfig.DEFAULT);

    /**
     * Codec used to encode and decode the server config file.
     */
    public static final Codec<MonsterAffixConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            MonsterAffixCountConfig.CODEC.optionalFieldOf("monster_affix_counts", MonsterAffixCountConfig.DEFAULT)
                    .forGetter(MonsterAffixConfig::monsterAffixCounts)
    ).apply(instance, MonsterAffixConfig::new));
}
