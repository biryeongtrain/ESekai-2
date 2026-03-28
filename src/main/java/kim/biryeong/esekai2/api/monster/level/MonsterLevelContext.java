package kim.biryeong.esekai2.api.monster.level;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kim.biryeong.esekai2.api.level.LevelRules;

import java.util.Objects;

/**
 * Runtime context describing how monster level scaling should be resolved.
 *
 * @param level monster level to resolve
 * @param rarity monster rarity affecting life and item level
 * @param mapMonster whether map scaling bonuses should apply
 * @param bossMonster whether boss map bonuses should apply in addition to base map bonuses
 */
public record MonsterLevelContext(
        int level,
        MonsterRarity rarity,
        boolean mapMonster,
        boolean bossMonster
) {
    private static final Codec<MonsterLevelContext> BASE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("level").forGetter(MonsterLevelContext::level),
            MonsterRarity.CODEC.fieldOf("rarity").forGetter(MonsterLevelContext::rarity),
            Codec.BOOL.optionalFieldOf("map_monster", false).forGetter(MonsterLevelContext::mapMonster),
            Codec.BOOL.optionalFieldOf("boss_monster", false).forGetter(MonsterLevelContext::bossMonster)
    ).apply(instance, MonsterLevelContext::new));

    /**
     * Validated codec used to decode runtime fixtures and test cases.
     */
    public static final Codec<MonsterLevelContext> CODEC = BASE_CODEC.validate(MonsterLevelContext::validate);

    public MonsterLevelContext {
        LevelRules.requireValidLevel(level, "level");
        Objects.requireNonNull(rarity, "rarity");
        if (bossMonster && !mapMonster) {
            throw new IllegalArgumentException("bossMonster requires mapMonster to also be true");
        }
    }

    private static DataResult<MonsterLevelContext> validate(MonsterLevelContext context) {
        if (!LevelRules.isValidLevel(context.level())) {
            return DataResult.error(() -> "level must be between 1 and 100");
        }

        if (context.bossMonster() && !context.mapMonster()) {
            return DataResult.error(() -> "boss_monster requires map_monster to also be true");
        }

        return DataResult.success(context);
    }
}
