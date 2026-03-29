package kim.biryeong.esekai2.api.skill.support;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Objects;

/**
 * Datapack-backed support definition for skill effect modifications.
 *
 * @param identifier unique, human-internal support id
 * @param effects tag-gated effect chunks contributed by this support
 */
public record SkillSupportDefinition(
        String identifier,
        List<SkillSupportEffect> effects
) {
    private static final Codec<SkillSupportDefinition> BASE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("identifier").forGetter(SkillSupportDefinition::identifier),
            Codec.list(SkillSupportEffect.CODEC).optionalFieldOf("effects", List.of()).forGetter(SkillSupportDefinition::effects)
    ).apply(instance, SkillSupportDefinition::new));

    /**
     * Validated codec used to decode support definitions from datapacks and fixtures.
     */
    public static final Codec<SkillSupportDefinition> CODEC = BASE_CODEC.validate(SkillSupportDefinition::validate);

    public SkillSupportDefinition {
        Objects.requireNonNull(identifier, "identifier");
        Objects.requireNonNull(effects, "effects");
        effects = List.copyOf(effects);
    }

    private static DataResult<SkillSupportDefinition> validate(SkillSupportDefinition definition) {
        if (definition.identifier().isBlank()) {
            return DataResult.error(() -> "identifier must not be blank");
        }
        if (definition.effects().isEmpty()) {
            return DataResult.error(() -> "effects must not be empty");
        }
        return DataResult.success(definition);
    }
}
