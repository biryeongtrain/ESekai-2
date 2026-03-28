package kim.biryeong.esekai2.api.monster.affix;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kim.biryeong.esekai2.api.monster.level.MonsterRarity;

import java.util.List;
import java.util.Objects;

/**
 * Describes one monster affix entry loaded from datapacks.
 *
 * @param translationKey translation key used when the affix is presented to players
 * @param kind prefix or suffix kind rolled in rarity-specific buckets
 * @param weight selection weight used when choosing from eligible candidates
 * @param minimumMonsterLevel minimum monster level required for this affix to be eligible
 * @param allowedRarities rarities that may legally receive this affix
 * @param modifierRanges ranged stat modifiers rolled by this affix
 */
public record MonsterAffixDefinition(
        String translationKey,
        MonsterAffixKind kind,
        int weight,
        int minimumMonsterLevel,
        List<MonsterRarity> allowedRarities,
        List<MonsterAffixModifierDefinition> modifierRanges
) {
    private static final Codec<MonsterAffixDefinition> BASE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("translation_key").forGetter(MonsterAffixDefinition::translationKey),
            MonsterAffixKind.CODEC.fieldOf("kind").forGetter(MonsterAffixDefinition::kind),
            Codec.INT.fieldOf("weight").forGetter(MonsterAffixDefinition::weight),
            Codec.INT.fieldOf("minimum_monster_level").forGetter(MonsterAffixDefinition::minimumMonsterLevel),
            Codec.list(MonsterRarity.CODEC).fieldOf("allowed_rarities").forGetter(MonsterAffixDefinition::allowedRarities),
            Codec.list(MonsterAffixModifierDefinition.CODEC).fieldOf("modifier_ranges").forGetter(MonsterAffixDefinition::modifierRanges)
    ).apply(instance, MonsterAffixDefinition::new));

    /**
     * Validated codec used to decode monster affix definitions from datapacks.
     */
    public static final Codec<MonsterAffixDefinition> CODEC = BASE_CODEC.validate(MonsterAffixDefinition::validate);

    public MonsterAffixDefinition {
        Objects.requireNonNull(translationKey, "translationKey");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(allowedRarities, "allowedRarities");
        Objects.requireNonNull(modifierRanges, "modifierRanges");

        allowedRarities = List.copyOf(allowedRarities);
        for (MonsterRarity rarity : allowedRarities) {
            Objects.requireNonNull(rarity, "allowedRarities entry");
        }

        modifierRanges = List.copyOf(modifierRanges);
        for (MonsterAffixModifierDefinition range : modifierRanges) {
            Objects.requireNonNull(range, "modifierRanges entry");
        }
    }

    /**
     * Returns whether this affix is legal for the requested monster level.
     *
     * @param monsterLevel monster level to inspect
     * @return {@code true} when the level meets this affix's minimum requirement
     */
    public boolean isAvailableAtLevel(int monsterLevel) {
        return monsterLevel >= minimumMonsterLevel;
    }

    /**
     * Returns whether this affix can roll for the requested monster rarity.
     *
     * @param rarity monster rarity to inspect
     * @return {@code true} when the rarity is allowed by this affix
     */
    public boolean supports(MonsterRarity rarity) {
        Objects.requireNonNull(rarity, "rarity");
        return allowedRarities.contains(rarity);
    }

    private static DataResult<MonsterAffixDefinition> validate(MonsterAffixDefinition definition) {
        if (definition.translationKey().isBlank()) {
            return DataResult.error(() -> "translation_key must not be blank");
        }

        if (definition.weight() < 1) {
            return DataResult.error(() -> "weight must be greater than or equal to 1");
        }

        if (definition.minimumMonsterLevel() < 1) {
            return DataResult.error(() -> "minimum_monster_level must be greater than or equal to 1");
        }

        if (definition.allowedRarities().isEmpty()) {
            return DataResult.error(() -> "allowed_rarities must not be empty");
        }

        if (definition.modifierRanges().isEmpty()) {
            return DataResult.error(() -> "modifier_ranges must not be empty");
        }

        return DataResult.success(definition);
    }
}
