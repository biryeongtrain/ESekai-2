package kim.biryeong.esekai2.api.skill.definition.graph;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

/**
 * Condition discriminator for rule execution branches.
 */
public enum SkillConditionType {
    ON_SPELL_CAST("on_spell_cast"),
    ON_HIT("on_hit"),
    ON_ENTITY_EXPIRE("on_entity_expire"),
    X_TICKS_CONDITION("x_ticks_condition");

    public static final Codec<SkillConditionType> CODEC = Codec.STRING.comapFlatMap(
            SkillConditionType::bySerializedName,
            SkillConditionType::serializedName
    );

    private final String serializedName;

    SkillConditionType(String serializedName) {
        this.serializedName = serializedName;
    }

    /**
     * Returns serialized condition name.
     *
     * @return event or tick predicate token
     */
    public String serializedName() {
        return serializedName;
    }

    private static DataResult<SkillConditionType> bySerializedName(String serializedName) {
        for (SkillConditionType type : values()) {
            if (type.serializedName.equals(serializedName)) {
                return DataResult.success(type);
            }
        }

        return DataResult.error(() -> "Unknown condition type: " + serializedName);
    }
}
