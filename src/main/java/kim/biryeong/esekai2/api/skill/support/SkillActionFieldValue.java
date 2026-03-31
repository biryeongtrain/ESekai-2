package kim.biryeong.esekai2.api.skill.support;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.List;
import java.util.Objects;

/**
 * Typed override payload that can carry either one scalar string or a whole string list.
 */
public record SkillActionFieldValue(
        List<String> values
) {
    private static final Codec<SkillActionFieldValue> BASE_CODEC = Codec.either(
            Codec.STRING,
            Codec.STRING.listOf()
    ).xmap(
            either -> either.map(SkillActionFieldValue::scalar, SkillActionFieldValue::list),
            value -> value.values().size() == 1
                    ? Either.left(value.first())
                    : Either.right(value.values())
    );

    /**
     * Validated codec used by support datapacks.
     */
    public static final Codec<SkillActionFieldValue> CODEC = BASE_CODEC.validate(SkillActionFieldValue::validate);

    public SkillActionFieldValue {
        Objects.requireNonNull(values, "values");
        values = List.copyOf(values);
    }

    /**
     * Creates a scalar override value.
     *
     * @param value scalar replacement value
     * @return scalar override value
     */
    public static SkillActionFieldValue scalar(String value) {
        return new SkillActionFieldValue(List.of(value));
    }

    /**
     * Creates a list override value.
     *
     * @param values replacement list value
     * @return list override value
     */
    public static SkillActionFieldValue list(List<String> values) {
        return new SkillActionFieldValue(values);
    }

    /**
     * Returns whether this override carries exactly one scalar value.
     *
     * @return {@code true} when only one value is present
     */
    public boolean isScalar() {
        return values.size() == 1;
    }

    /**
     * Returns the first stored value.
     *
     * @return first scalar entry
     */
    public String first() {
        return values.getFirst();
    }

    private static DataResult<SkillActionFieldValue> validate(SkillActionFieldValue value) {
        if (value.values().isEmpty()) {
            return DataResult.error(() -> "override values must not be empty");
        }
        if (value.isScalar()) {
            return DataResult.success(value);
        }
        for (String entry : value.values()) {
            if (entry == null || entry.isBlank()) {
                return DataResult.error(() -> "override values must not contain blank entries");
            }
        }
        return DataResult.success(value);
    }
}
