package kim.biryeong.esekai2.api.monster.level;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kim.biryeong.esekai2.api.level.LevelRules;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Full monster level table used to resolve monster scaling across the supported level range.
 *
 * @param entries ordered monster level rows for levels 1 through 100
 */
public record MonsterLevelDefinition(
        List<MonsterLevelEntry> entries
) {
    private static final Codec<MonsterLevelDefinition> BASE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.list(MonsterLevelEntry.CODEC).fieldOf("entries").forGetter(MonsterLevelDefinition::entries)
    ).apply(instance, MonsterLevelDefinition::new));

    /**
     * Validated codec used to decode monster level tables from datapacks and test fixtures.
     */
    public static final Codec<MonsterLevelDefinition> CODEC = BASE_CODEC.validate(MonsterLevelDefinition::validate);

    public MonsterLevelDefinition {
        Objects.requireNonNull(entries, "entries");
        entries = List.copyOf(entries);
        for (MonsterLevelEntry entry : entries) {
            Objects.requireNonNull(entry, "entries entry");
        }
    }

    /**
     * Returns the configured row for the requested monster level.
     *
     * @param level level to resolve
     * @return matching monster level row
     */
    public MonsterLevelEntry entry(int level) {
        LevelRules.requireValidLevel(level, "level");
        return entries.stream()
                .filter(entry -> entry.level() == level)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing monster level row for level " + level));
    }

    private static DataResult<MonsterLevelDefinition> validate(MonsterLevelDefinition definition) {
        if (definition.entries().size() != LevelRules.MAX_LEVEL) {
            return DataResult.error(() -> "entries must contain exactly 100 rows");
        }

        Map<Integer, MonsterLevelEntry> byLevel = new LinkedHashMap<>();
        for (MonsterLevelEntry entry : definition.entries()) {
            if (byLevel.put(entry.level(), entry) != null) {
                return DataResult.error(() -> "entries must not contain duplicate level rows");
            }
        }

        for (int level = LevelRules.MIN_LEVEL; level <= LevelRules.MAX_LEVEL; level++) {
            if (!byLevel.containsKey(level)) {
                return DataResult.error(() -> "entries must contain every level from 1 through 100");
            }
        }

        return DataResult.success(definition);
    }
}
