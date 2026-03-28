package kim.biryeong.esekai2.api.monster.level;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kim.biryeong.esekai2.api.level.LevelRules;

/**
 * One monster level row mirroring the current PoE snapshot used by ESekai.
 */
public record MonsterLevelEntry(
        int level,
        double damage,
        double evasionRating,
        double accuracyRating,
        double experiencePoints,
        double life,
        double summonLife,
        double magicLifeBonusPercent,
        double rareLifeBonusPercent,
        double mapLifeBonusPercent,
        double mapDamageBonusPercent,
        double bossLifeBonusPercent,
        double bossDamageBonusPercent,
        double bossItemQuantityBonusPercent,
        double bossItemRarityBonusPercent
) {
    private static final Codec<MonsterLevelEntry> BASE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("level").forGetter(MonsterLevelEntry::level),
            Codec.DOUBLE.fieldOf("damage").forGetter(MonsterLevelEntry::damage),
            Codec.DOUBLE.fieldOf("evasion_rating").forGetter(MonsterLevelEntry::evasionRating),
            Codec.DOUBLE.fieldOf("accuracy_rating").forGetter(MonsterLevelEntry::accuracyRating),
            Codec.DOUBLE.fieldOf("experience_points").forGetter(MonsterLevelEntry::experiencePoints),
            Codec.DOUBLE.fieldOf("life").forGetter(MonsterLevelEntry::life),
            Codec.DOUBLE.fieldOf("summon_life").forGetter(MonsterLevelEntry::summonLife),
            Codec.DOUBLE.optionalFieldOf("magic_life_bonus_percent", 0.0).forGetter(MonsterLevelEntry::magicLifeBonusPercent),
            Codec.DOUBLE.optionalFieldOf("rare_life_bonus_percent", 0.0).forGetter(MonsterLevelEntry::rareLifeBonusPercent),
            Codec.DOUBLE.optionalFieldOf("map_life_bonus_percent", 0.0).forGetter(MonsterLevelEntry::mapLifeBonusPercent),
            Codec.DOUBLE.optionalFieldOf("map_damage_bonus_percent", 0.0).forGetter(MonsterLevelEntry::mapDamageBonusPercent),
            Codec.DOUBLE.optionalFieldOf("boss_life_bonus_percent", 0.0).forGetter(MonsterLevelEntry::bossLifeBonusPercent),
            Codec.DOUBLE.optionalFieldOf("boss_damage_bonus_percent", 0.0).forGetter(MonsterLevelEntry::bossDamageBonusPercent),
            Codec.DOUBLE.optionalFieldOf("boss_item_quantity_bonus_percent", 0.0).forGetter(MonsterLevelEntry::bossItemQuantityBonusPercent),
            Codec.DOUBLE.optionalFieldOf("boss_item_rarity_bonus_percent", 0.0).forGetter(MonsterLevelEntry::bossItemRarityBonusPercent)
    ).apply(instance, MonsterLevelEntry::new));

    /**
     * Validated codec used to decode monster level rows from datapacks and test fixtures.
     */
    public static final Codec<MonsterLevelEntry> CODEC = BASE_CODEC.validate(MonsterLevelEntry::validate);

    public MonsterLevelEntry {
        LevelRules.requireValidLevel(level, "level");
        if (!allFinite(
                damage,
                evasionRating,
                accuracyRating,
                experiencePoints,
                life,
                summonLife,
                magicLifeBonusPercent,
                rareLifeBonusPercent,
                mapLifeBonusPercent,
                mapDamageBonusPercent,
                bossLifeBonusPercent,
                bossDamageBonusPercent,
                bossItemQuantityBonusPercent,
                bossItemRarityBonusPercent
        )) {
            throw new IllegalArgumentException("monster level entry values must be finite numbers");
        }

        if (magicLifeBonusPercent < 0.0 || rareLifeBonusPercent < 0.0
                || mapLifeBonusPercent < 0.0 || mapDamageBonusPercent < 0.0
                || bossLifeBonusPercent < 0.0 || bossDamageBonusPercent < 0.0
                || bossItemQuantityBonusPercent < 0.0 || bossItemRarityBonusPercent < 0.0) {
            throw new IllegalArgumentException("monster level bonus percents must be greater than or equal to 0");
        }
    }

    private static DataResult<MonsterLevelEntry> validate(MonsterLevelEntry entry) {
        if (!LevelRules.isValidLevel(entry.level())) {
            return DataResult.error(() -> "level must be between 1 and 100");
        }

        if (!allFinite(
                entry.damage(),
                entry.evasionRating(),
                entry.accuracyRating(),
                entry.experiencePoints(),
                entry.life(),
                entry.summonLife(),
                entry.magicLifeBonusPercent(),
                entry.rareLifeBonusPercent(),
                entry.mapLifeBonusPercent(),
                entry.mapDamageBonusPercent(),
                entry.bossLifeBonusPercent(),
                entry.bossDamageBonusPercent(),
                entry.bossItemQuantityBonusPercent(),
                entry.bossItemRarityBonusPercent()
        )) {
            return DataResult.error(() -> "monster level entry values must be finite numbers");
        }

        if (entry.magicLifeBonusPercent() < 0.0 || entry.rareLifeBonusPercent() < 0.0
                || entry.mapLifeBonusPercent() < 0.0 || entry.mapDamageBonusPercent() < 0.0
                || entry.bossLifeBonusPercent() < 0.0 || entry.bossDamageBonusPercent() < 0.0
                || entry.bossItemQuantityBonusPercent() < 0.0 || entry.bossItemRarityBonusPercent() < 0.0) {
            return DataResult.error(() -> "monster level bonus percents must be greater than or equal to 0");
        }

        return DataResult.success(entry);
    }

    private static boolean allFinite(double... values) {
        for (double value : values) {
            if (!Double.isFinite(value)) {
                return false;
            }
        }
        return true;
    }
}
