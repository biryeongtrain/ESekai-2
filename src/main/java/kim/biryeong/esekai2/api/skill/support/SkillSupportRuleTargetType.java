package kim.biryeong.esekai2.api.skill.support;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

/**
 * Target route categories that appended support rules may be attached to.
 */
public enum SkillSupportRuleTargetType {
    ON_CAST("on_cast"),
    ENTITY_COMPONENT("entity_component");

    /**
     * Codec used by support datapacks.
     */
    public static final Codec<SkillSupportRuleTargetType> CODEC = Codec.STRING.comapFlatMap(
            SkillSupportRuleTargetType::bySerializedName,
            SkillSupportRuleTargetType::serializedName
    );

    private final String serializedName;

    SkillSupportRuleTargetType(String serializedName) {
        this.serializedName = serializedName;
    }

    /**
     * Returns the serialized discriminator used in support fixtures.
     *
     * @return stable serialized rule-target type
     */
    public String serializedName() {
        return serializedName;
    }

    private static DataResult<SkillSupportRuleTargetType> bySerializedName(String serializedName) {
        for (SkillSupportRuleTargetType type : values()) {
            if (type.serializedName.equals(serializedName)) {
                return DataResult.success(type);
            }
        }

        return DataResult.error(() -> "Unknown support rule target type: " + serializedName);
    }
}
