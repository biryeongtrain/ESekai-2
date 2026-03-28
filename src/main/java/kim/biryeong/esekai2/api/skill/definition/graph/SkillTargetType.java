package kim.biryeong.esekai2.api.skill.definition.graph;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

/**
 * Targeting selector mode used by action graph rules.
 */
public enum SkillTargetType {
    SELF("self"),
    TARGET("target"),
    AOE("aoe");

    public static final Codec<SkillTargetType> CODEC = Codec.STRING.comapFlatMap(
            SkillTargetType::bySerializedName,
            SkillTargetType::serializedName
    );

    private final String serializedName;

    SkillTargetType(String serializedName) {
        this.serializedName = serializedName;
    }

    /**
     * Returns the JSON serial name for this target type.
     *
     * @return snake_case target type token
     */
    public String serializedName() {
        return serializedName;
    }

    private static DataResult<SkillTargetType> bySerializedName(String serializedName) {
        for (SkillTargetType type : values()) {
            if (type.serializedName.equals(serializedName)) {
                return DataResult.success(type);
            }
        }

        return DataResult.error(() -> "Unknown target type: " + serializedName);
    }
}
