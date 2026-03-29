package kim.biryeong.esekai2.api.skill.support;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.Objects;

/**
 * A single typed field override applied to a matched skill action.
 *
 * @param path stable field path that should be rewritten
 * @param value replacement value stored as a serialized string
 */
public record SkillActionFieldOverride(
        SkillActionFieldPath path,
        String value
) {
    private static final Codec<SkillActionFieldOverride> BASE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SkillActionFieldPath.CODEC.fieldOf("path").forGetter(SkillActionFieldOverride::path),
            Codec.STRING.fieldOf("value").forGetter(SkillActionFieldOverride::value)
    ).apply(instance, SkillActionFieldOverride::new));

    /**
     * Validated codec used by support datapacks.
     */
    public static final Codec<SkillActionFieldOverride> CODEC = BASE_CODEC.validate(SkillActionFieldOverride::validate);

    public SkillActionFieldOverride {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(value, "value");
    }

    /**
     * Creates a parameter override for the supplied key.
     *
     * @param parameterKey parameter key to rewrite
     * @param value replacement value
     * @return typed parameter override
     */
    public static SkillActionFieldOverride parameter(String parameterKey, String value) {
        return new SkillActionFieldOverride(SkillActionFieldPath.parameter(parameterKey), value);
    }

    /**
     * Creates a calculation id override.
     *
     * @param value replacement calculation id value
     * @return typed calculation id override
     */
    public static SkillActionFieldOverride calculationId(String value) {
        return new SkillActionFieldOverride(SkillActionFieldPath.calculationId(), value);
    }

    private static DataResult<SkillActionFieldOverride> validate(SkillActionFieldOverride override) {
        if (override.path().type() == SkillActionFieldPathType.PARAMETER
                && override.path().parameterKey().isBlank()) {
            return DataResult.error(() -> "parameter_key must not be blank for parameter overrides");
        }
        if (override.path().type() == SkillActionFieldPathType.CALCULATION_ID
                && !override.value().isBlank()
                && Identifier.tryParse(override.value()) == null) {
            return DataResult.error(() -> "calculation_id override must be blank or a valid identifier: " + override.value());
        }
        return DataResult.success(override);
    }
}
