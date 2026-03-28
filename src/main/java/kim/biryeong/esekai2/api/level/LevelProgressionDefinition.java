package kim.biryeong.esekai2.api.level;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Full progression table describing the experience requirements for every supported level.
 *
 * @param entries ordered progression rows for levels 1 through 100
 */
public record LevelProgressionDefinition(
        List<LevelProgressionEntry> entries
) {
    private static final Codec<LevelProgressionDefinition> BASE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.list(Codec.LONG).fieldOf("experience_to_next_level").forGetter(LevelProgressionDefinition::experienceToNextLevelColumn)
    ).apply(instance, LevelProgressionDefinition::fromExperienceToNextLevelColumn));

    /**
     * Validated codec used to decode progression tables from datapacks and test fixtures.
     */
    public static final Codec<LevelProgressionDefinition> CODEC = BASE_CODEC.validate(LevelProgressionDefinition::validate);

    public LevelProgressionDefinition {
        Objects.requireNonNull(entries, "entries");
        entries = List.copyOf(entries);
        for (LevelProgressionEntry entry : entries) {
            Objects.requireNonNull(entry, "entries entry");
        }
    }

    /**
     * Returns the configured progression row for the requested level.
     *
     * @param level level to resolve
     * @return progression row for the requested level
     */
    public LevelProgressionEntry entry(int level) {
        LevelRules.requireValidLevel(level, "level");
        return entries.stream()
                .filter(entry -> entry.level() == level)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing progression row for level " + level));
    }

    /**
     * Returns the configured experience needed to advance from the requested level.
     *
     * @param level level to inspect
     * @return experience required to reach the next level
     */
    public long experienceToNextLevel(int level) {
        return entry(level).experienceToNextLevel();
    }

    /**
     * Returns the total experience required to reach the start of the requested level.
     *
     * @param level target level
     * @return cumulative experience needed to reach the target level
     */
    public long totalExperienceToReachLevel(int level) {
        LevelRules.requireValidLevel(level, "level");

        long total = 0L;
        for (int current = LevelRules.MIN_LEVEL; current < level; current++) {
            total += experienceToNextLevel(current);
        }
        return total;
    }

    private static LevelProgressionDefinition fromExperienceToNextLevelColumn(List<Long> experienceToNextLevel) {
        List<LevelProgressionEntry> entries = new ArrayList<>(experienceToNextLevel.size());
        for (int index = 0; index < experienceToNextLevel.size(); index++) {
            entries.add(new LevelProgressionEntry(index + 1, experienceToNextLevel.get(index)));
        }
        return new LevelProgressionDefinition(entries);
    }

    private List<Long> experienceToNextLevelColumn() {
        return entries.stream()
                .map(LevelProgressionEntry::experienceToNextLevel)
                .toList();
    }

    private static DataResult<LevelProgressionDefinition> validate(LevelProgressionDefinition definition) {
        if (definition.entries().size() != LevelRules.MAX_LEVEL) {
            return DataResult.error(() -> "entries must contain exactly 100 rows");
        }

        Map<Integer, LevelProgressionEntry> byLevel = new LinkedHashMap<>();
        for (LevelProgressionEntry entry : definition.entries()) {
            if (byLevel.put(entry.level(), entry) != null) {
                return DataResult.error(() -> "entries must not contain duplicate level rows");
            }
        }

        for (int level = LevelRules.MIN_LEVEL; level <= LevelRules.MAX_LEVEL; level++) {
            if (!byLevel.containsKey(level)) {
                return DataResult.error(() -> "entries must contain every level from 1 through 100");
            }
        }

        if (byLevel.get(LevelRules.MAX_LEVEL).experienceToNextLevel() != 0L) {
            return DataResult.error(() -> "level 100 must have experience_to_next_level equal to 0");
        }

        return DataResult.success(definition);
    }
}
