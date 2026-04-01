package kim.biryeong.esekai2.api.skill.definition.graph;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

/**
 * Predicate discriminator for entity-component conditional execution loops.
 */
public enum SkillPredicateType {
    ALWAYS("always"),
    RANDOM_CHANCE("random_chance"),
    HAS_TARGET("has_target"),
    HAS_EFFECT("has_effect"),
    HAS_RESOURCE("has_resource"),
    COOLDOWN_READY("cooldown_ready"),
    HAS_CHARGES("has_charges"),
    HAS_BURST_FOLLOWUP("has_burst_followup");

    public static final Codec<SkillPredicateType> CODEC = Codec.STRING.comapFlatMap(
            SkillPredicateType::bySerializedName,
            SkillPredicateType::serializedName
    );

    private final String serializedName;

    SkillPredicateType(String serializedName) {
        this.serializedName = serializedName;
    }

    /**
     * Returns serialized predicate name for codec usage.
     *
     * @return stable discriminator value
     */
    public String serializedName() {
        return serializedName;
    }

    private static DataResult<SkillPredicateType> bySerializedName(String serializedName) {
        for (SkillPredicateType type : values()) {
            if (type.serializedName.equals(serializedName)) {
                return DataResult.success(type);
            }
        }

        return DataResult.error(() -> "Unknown predicate type: " + serializedName);
    }
}
