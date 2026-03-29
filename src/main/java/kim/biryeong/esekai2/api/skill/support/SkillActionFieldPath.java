package kim.biryeong.esekai2.api.skill.support;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Objects;

/**
 * Typed field-path descriptor used by support action overrides.
 *
 * @param type stable field category
 * @param parameterKey parameter key when {@link #type()} is {@link SkillActionFieldPathType#PARAMETER}
 */
public record SkillActionFieldPath(
        SkillActionFieldPathType type,
        String parameterKey
) {
    private static final Codec<SkillActionFieldPath> BASE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SkillActionFieldPathType.CODEC.fieldOf("type").forGetter(SkillActionFieldPath::type),
            Codec.STRING.optionalFieldOf("parameter_key", "").forGetter(SkillActionFieldPath::parameterKey)
    ).apply(instance, SkillActionFieldPath::new));

    /**
     * Validated codec used to decode field paths from datapacks and fixtures.
     */
    public static final Codec<SkillActionFieldPath> CODEC = BASE_CODEC.validate(SkillActionFieldPath::validate);

    public SkillActionFieldPath {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(parameterKey, "parameterKey");
    }

    /**
     * Creates a typed parameter field path.
     *
     * @param parameterKey parameter key to target
     * @return typed parameter field path
     */
    public static SkillActionFieldPath parameter(String parameterKey) {
        return new SkillActionFieldPath(SkillActionFieldPathType.PARAMETER, parameterKey);
    }

    /**
     * Creates a typed calculation id field path.
     *
     * @return typed calculation id field path
     */
    public static SkillActionFieldPath calculationId() {
        return new SkillActionFieldPath(SkillActionFieldPathType.CALCULATION_ID, "");
    }

    private static DataResult<SkillActionFieldPath> validate(SkillActionFieldPath path) {
        return switch (path.type()) {
            case PARAMETER -> {
                if (path.parameterKey().isBlank()) {
                    yield DataResult.error(() -> "parameter_key is required for parameter field overrides");
                }
                yield DataResult.success(path);
            }
            case CALCULATION_ID -> {
                if (!path.parameterKey().isBlank()) {
                    yield DataResult.error(() -> "parameter_key must be empty for calculation_id field overrides");
                }
                yield DataResult.success(path);
            }
        };
    }
}
