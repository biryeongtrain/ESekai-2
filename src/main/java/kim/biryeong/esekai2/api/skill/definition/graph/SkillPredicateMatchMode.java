package kim.biryeong.esekai2.api.skill.definition.graph;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

/**
 * Compound match policy used by {@code has_effect} predicates.
 */
public enum SkillPredicateMatchMode {
    ANY_OF("any_of"),
    ALL_OF("all_of");

    public static final Codec<SkillPredicateMatchMode> CODEC = Codec.STRING.comapFlatMap(
            SkillPredicateMatchMode::bySerializedName,
            SkillPredicateMatchMode::serializedName
    );

    private final String serializedName;

    SkillPredicateMatchMode(String serializedName) {
        this.serializedName = serializedName;
    }

    /**
     * Returns the stable datapack-facing match mode name.
     *
     * @return match mode discriminator
     */
    public String serializedName() {
        return serializedName;
    }

    /**
     * Resolves a serialized datapack name into a match mode.
     *
     * @param serializedName datapack-facing name
     * @return parsed mode or {@code null} when unknown
     */
    public static SkillPredicateMatchMode fromSerializedName(String serializedName) {
        for (SkillPredicateMatchMode mode : values()) {
            if (mode.serializedName.equals(serializedName)) {
                return mode;
            }
        }
        return null;
    }

    private static DataResult<SkillPredicateMatchMode> bySerializedName(String serializedName) {
        SkillPredicateMatchMode mode = fromSerializedName(serializedName);
        if (mode != null) {
            return DataResult.success(mode);
        }

        return DataResult.error(() -> "Unknown predicate match mode: " + serializedName);
    }
}
