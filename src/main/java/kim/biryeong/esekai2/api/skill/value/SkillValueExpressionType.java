package kim.biryeong.esekai2.api.skill.value;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

/**
 * Discriminant for the typed skill value expression surface.
 */
public enum SkillValueExpressionType {
    CONSTANT("constant"),
    REFERENCE("reference"),
    STAT("stat"),
    RESOURCE_CURRENT("resource_current"),
    RESOURCE_MAX("resource_max"),
    RESOURCE_COST("resource_cost"),
    USE_TIME_TICKS("use_time_ticks"),
    COOLDOWN_TICKS("cooldown_ticks"),
    MAX_CHARGES("max_charges"),
    TIMES_TO_CAST("times_to_cast"),
    COOLDOWN_REMAINING("cooldown_remaining"),
    CHARGES_AVAILABLE("charges_available"),
    BURST_REMAINING("burst_remaining");

    public static final Codec<SkillValueExpressionType> CODEC = Codec.STRING.comapFlatMap(
            SkillValueExpressionType::bySerializedName,
            SkillValueExpressionType::serializedName
    );

    private final String serializedName;

    SkillValueExpressionType(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }

    private static DataResult<SkillValueExpressionType> bySerializedName(String serializedName) {
        for (SkillValueExpressionType type : values()) {
            if (type.serializedName.equals(serializedName)) {
                return DataResult.success(type);
            }
        }
        return DataResult.error(() -> "Unknown skill value expression type: " + serializedName);
    }
}
