package kim.biryeong.esekai2.api.skill.support;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

/**
 * Stable typed field categories that support overrides may target.
 */
public enum SkillActionFieldPathType {
    PARAMETER("parameter"),
    CALCULATION_ID("calculation_id");

    /**
     * Codec used by support datapacks.
     */
    public static final Codec<SkillActionFieldPathType> CODEC = Codec.STRING.comapFlatMap(
            SkillActionFieldPathType::bySerializedName,
            SkillActionFieldPathType::serializedName
    );

    private final String serializedName;

    SkillActionFieldPathType(String serializedName) {
        this.serializedName = serializedName;
    }

    /**
     * Returns the serialized discriminator used in support fixtures.
     *
     * @return stable serialized field-path type
     */
    public String serializedName() {
        return serializedName;
    }

    private static DataResult<SkillActionFieldPathType> bySerializedName(String serializedName) {
        for (SkillActionFieldPathType type : values()) {
            if (type.serializedName.equals(serializedName)) {
                return DataResult.success(type);
            }
        }

        return DataResult.error(() -> "Unknown action field path type: " + serializedName);
    }
}
