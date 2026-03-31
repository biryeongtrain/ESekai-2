package kim.biryeong.esekai2.api.skill.support;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kim.biryeong.esekai2.api.skill.effect.SkillAilmentRefreshPolicy;
import kim.biryeong.esekai2.api.skill.effect.SkillEffectPurgeMode;
import kim.biryeong.esekai2.api.skill.effect.MobEffectRefreshPolicy;
import net.minecraft.resources.Identifier;

import java.util.Objects;

/**
 * A single typed field override applied to a matched skill action.
 *
 * @param path stable field path that should be rewritten
 * @param value replacement value stored as a typed scalar or string-list payload
 */
public record SkillActionFieldOverride(
        SkillActionFieldPath path,
        SkillActionFieldValue value
) {
    private static final Codec<SkillActionFieldOverride> BASE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SkillActionFieldPath.CODEC.fieldOf("path").forGetter(SkillActionFieldOverride::path),
            SkillActionFieldValue.CODEC.fieldOf("value").forGetter(SkillActionFieldOverride::value)
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
        return new SkillActionFieldOverride(SkillActionFieldPath.parameter(parameterKey), SkillActionFieldValue.scalar(value));
    }

    /**
     * Creates a parameter override for the supplied key using a full string-list replacement.
     *
     * @param parameterKey parameter key to rewrite
     * @param values replacement string list
     * @return typed parameter override
     */
    public static SkillActionFieldOverride parameter(String parameterKey, java.util.List<String> values) {
        return new SkillActionFieldOverride(SkillActionFieldPath.parameter(parameterKey), SkillActionFieldValue.list(values));
    }

    /**
     * Creates a calculation id override.
     *
     * @param value replacement calculation id value
     * @return typed calculation id override
     */
    public static SkillActionFieldOverride calculationId(String value) {
        return new SkillActionFieldOverride(SkillActionFieldPath.calculationId(), SkillActionFieldValue.scalar(value));
    }

    private static DataResult<SkillActionFieldOverride> validate(SkillActionFieldOverride override) {
        if (override.path().type() == SkillActionFieldPathType.PARAMETER
                && override.path().parameterKey().isBlank()) {
            return DataResult.error(() -> "parameter_key must not be blank for parameter overrides");
        }
        if (override.path().type() == SkillActionFieldPathType.CALCULATION_ID
                && (!override.value().isScalar()
                || (!override.value().first().isBlank() && Identifier.tryParse(override.value().first()) == null))) {
            return DataResult.error(() -> "calculation_id override must be a scalar valid identifier: " + override.value().values());
        }
        if (override.path().type() == SkillActionFieldPathType.PARAMETER
                && override.path().parameterKey().equals("purge")
                && (!override.value().isScalar() || SkillEffectPurgeMode.fromSerializedName(override.value().first()) == null)) {
            return DataResult.error(() -> "purge override must be one scalar positive/negative/all value: " + override.value().values());
        }
        if (override.path().type() == SkillActionFieldPathType.PARAMETER
                && override.path().parameterKey().equals("refresh_policy")
                && (!override.value().isScalar() || !isSupportedRefreshPolicy(override.value().first()))) {
            return DataResult.error(() -> "refresh_policy override must be one scalar supported policy value: " + override.value().values());
        }
        return DataResult.success(override);
    }

    private static boolean isSupportedRefreshPolicy(String raw) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        for (MobEffectRefreshPolicy policy : MobEffectRefreshPolicy.values()) {
            if (policy.serializedName().equals(raw)) {
                return true;
            }
        }
        return SkillAilmentRefreshPolicy.fromSerializedName(raw) != null;
    }
}
