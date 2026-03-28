package kim.biryeong.esekai2.api.item.affix;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kim.biryeong.esekai2.api.item.type.ItemFamily;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Objects;

/**
 * Describes one affix entry loaded from the ESekai affix dynamic registry.
 *
 * @param translationKey translation key used when the affix is presented to players
 * @param kind PoE-style affix kind used when later item systems count prefixes and suffixes
 * @param groupId shared affix group identifier used by later exclusivity rules
 * @param tier affix tier number where lower numbers represent stronger affixes
 * @param minimumItemLevel minimum item level required for this affix to be eligible
 * @param scope whether this affix is intended to apply locally or globally
 * @param itemFamilies item families this affix can legally roll onto
 * @param modifierRanges ranged stat modifiers rolled by this affix
 */
public record AffixDefinition(
        String translationKey,
        AffixKind kind,
        Identifier groupId,
        int tier,
        int minimumItemLevel,
        AffixScope scope,
        List<ItemFamily> itemFamilies,
        List<AffixModifierDefinition> modifierRanges
) {
    private static final Codec<AffixDefinition> BASE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("translation_key").forGetter(AffixDefinition::translationKey),
            AffixKind.CODEC.fieldOf("kind").forGetter(AffixDefinition::kind),
            Identifier.CODEC.fieldOf("group_id").forGetter(AffixDefinition::groupId),
            Codec.INT.fieldOf("tier").forGetter(AffixDefinition::tier),
            Codec.INT.fieldOf("minimum_item_level").forGetter(AffixDefinition::minimumItemLevel),
            AffixScope.CODEC.fieldOf("scope").forGetter(AffixDefinition::scope),
            Codec.list(ItemFamily.CODEC).fieldOf("item_families").forGetter(AffixDefinition::itemFamilies),
            Codec.list(AffixModifierDefinition.CODEC).fieldOf("modifier_ranges").forGetter(AffixDefinition::modifierRanges)
    ).apply(instance, AffixDefinition::new));

    /**
     * Validated codec used to decode affix definitions from datapacks and test fixtures.
     */
    public static final Codec<AffixDefinition> CODEC = BASE_CODEC.validate(AffixDefinition::validate);

    public AffixDefinition {
        Objects.requireNonNull(translationKey, "translationKey");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(groupId, "groupId");
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(itemFamilies, "itemFamilies");
        Objects.requireNonNull(modifierRanges, "modifierRanges");

        itemFamilies = List.copyOf(itemFamilies);
        for (ItemFamily family : itemFamilies) {
            Objects.requireNonNull(family, "itemFamily entry");
        }

        modifierRanges = List.copyOf(modifierRanges);
        for (AffixModifierDefinition range : modifierRanges) {
            Objects.requireNonNull(range, "modifierRange entry");
        }
    }

    /**
     * Returns whether this affix can legally roll onto the requested family.
     *
     * @param family item family to inspect
     * @return {@code true} when the family is supported by this affix
     */
    public boolean supports(ItemFamily family) {
        Objects.requireNonNull(family, "family");
        return itemFamilies.contains(family);
    }

    /**
     * Returns whether this affix belongs to the requested group.
     *
     * @param groupId group identifier to compare against this affix
     * @return {@code true} when this affix shares the requested group id
     */
    public boolean belongsToGroup(Identifier groupId) {
        Objects.requireNonNull(groupId, "groupId");
        return this.groupId.equals(groupId);
    }

    /**
     * Returns whether this affix is available at the provided item level.
     *
     * @param itemLevel item level to evaluate
     * @return {@code true} when the item level meets this affix's minimum requirement
     */
    public boolean isAvailableAtItemLevel(int itemLevel) {
        return itemLevel >= minimumItemLevel;
    }

    private static DataResult<AffixDefinition> validate(AffixDefinition definition) {
        if (definition.translationKey().isBlank()) {
            return DataResult.error(() -> "translation_key must not be blank");
        }

        if (definition.tier() < 1) {
            return DataResult.error(() -> "tier must be greater than or equal to 1");
        }

        if (definition.minimumItemLevel() < 1) {
            return DataResult.error(() -> "minimum_item_level must be greater than or equal to 1");
        }

        if (definition.itemFamilies().isEmpty()) {
            return DataResult.error(() -> "item_families must not be empty");
        }

        if (definition.modifierRanges().isEmpty()) {
            return DataResult.error(() -> "modifier_ranges must not be empty");
        }

        return DataResult.success(definition);
    }
}
