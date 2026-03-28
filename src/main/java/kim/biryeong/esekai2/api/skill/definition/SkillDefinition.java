package kim.biryeong.esekai2.api.skill.definition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Top-level skill definition used by the data-driven skill runtime.
 *
 * @param identifier stable identifier exposed by the datapack definition
 * @param config skill execution configuration and runtime base values
 * @param attached attached action graph grouped by cast and entity component events
 * @param manualTip localized tooltip key shown as manual text in skill UI
 * @param disabledDims dimension identifiers where this skill is disabled
 * @param effectTip localized tooltip key shown as effect description in skill UI
 */
public record SkillDefinition(
        String identifier,
        SkillConfig config,
        SkillAttached attached,
        String manualTip,
        Set<Identifier> disabledDims,
        String effectTip
) {
    private static final Codec<Set<Identifier>> DISABLED_DIMENSION_CODEC = Codec.list(Identifier.CODEC).xmap(
            list -> list.stream().collect(Collectors.toCollection(HashSet::new)),
            list -> List.copyOf(list)
    );

    private static final Codec<SkillDefinition> BASE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("identifier").forGetter(SkillDefinition::identifier),
            SkillConfig.CODEC.optionalFieldOf("config", SkillConfig.DEFAULT).forGetter(SkillDefinition::config),
            SkillAttached.CODEC.optionalFieldOf("attached", SkillAttached.EMPTY).forGetter(SkillDefinition::attached),
            Codec.STRING.optionalFieldOf("manual_tip", "").forGetter(SkillDefinition::manualTip),
            DISABLED_DIMENSION_CODEC.optionalFieldOf("disabled_dims", Set.of()).forGetter(SkillDefinition::disabledDims),
            Codec.STRING.optionalFieldOf("effect_tip", "").forGetter(SkillDefinition::effectTip)
    ).apply(instance, SkillDefinition::new));

    /**
     * Validated codec used to decode skill definitions from datapacks and test fixtures.
     */
    public static final Codec<SkillDefinition> CODEC = BASE_CODEC.validate(SkillDefinition::validate);

    public SkillDefinition {
        Objects.requireNonNull(identifier, "identifier");
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(attached, "attached");
        Objects.requireNonNull(manualTip, "manualTip");
        Objects.requireNonNull(disabledDims, "disabledDims");
        Objects.requireNonNull(effectTip, "effectTip");

        disabledDims = Set.copyOf(disabledDims);
    }

    private static DataResult<SkillDefinition> validate(SkillDefinition definition) {
        if (definition.identifier().isBlank()) {
            return DataResult.error(() -> "identifier must not be blank");
        }
        return DataResult.success(definition);
    }
}
