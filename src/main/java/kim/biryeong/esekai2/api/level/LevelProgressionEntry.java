package kim.biryeong.esekai2.api.level;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * One level progression row describing the experience required to advance from this level.
 *
 * @param level level represented by this row
 * @param experienceToNextLevel experience required to reach the next level from this row
 */
public record LevelProgressionEntry(
        int level,
        long experienceToNextLevel
) {
    private static final Codec<LevelProgressionEntry> BASE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("level").forGetter(LevelProgressionEntry::level),
            Codec.LONG.fieldOf("experience_to_next_level").forGetter(LevelProgressionEntry::experienceToNextLevel)
    ).apply(instance, LevelProgressionEntry::new));

    /**
     * Validated codec used to decode level progression rows from datapacks and test fixtures.
     */
    public static final Codec<LevelProgressionEntry> CODEC = BASE_CODEC.validate(LevelProgressionEntry::validate);

    public LevelProgressionEntry {
        LevelRules.requireValidLevel(level, "level");
        if (experienceToNextLevel < 0L) {
            throw new IllegalArgumentException("experienceToNextLevel must be greater than or equal to 0");
        }
    }

    private static DataResult<LevelProgressionEntry> validate(LevelProgressionEntry entry) {
        if (!LevelRules.isValidLevel(entry.level())) {
            return DataResult.error(() -> "level must be between 1 and 100");
        }

        if (entry.experienceToNextLevel() < 0L) {
            return DataResult.error(() -> "experience_to_next_level must be greater than or equal to 0");
        }

        if (entry.level() == LevelRules.MAX_LEVEL && entry.experienceToNextLevel() != 0L) {
            return DataResult.error(() -> "level 100 must have experience_to_next_level equal to 0");
        }

        return DataResult.success(entry);
    }
}
