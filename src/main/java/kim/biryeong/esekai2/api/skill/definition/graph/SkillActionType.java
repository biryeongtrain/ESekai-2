package kim.biryeong.esekai2.api.skill.definition.graph;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

/**
 * Action variant keyword used by attached rule execution.
 */
public enum SkillActionType {
    SOUND("sound"),
    DAMAGE("damage"),
    PROJECTILE("projectile"),
    SUMMON_AT_SIGHT("summon_at_sight"),
    SUMMON_BLOCK("summon_block"),
    SANDSTORM_PARTICLE("sandstorm_particle");

    public static final Codec<SkillActionType> CODEC = Codec.STRING.comapFlatMap(
            SkillActionType::bySerializedName,
            SkillActionType::serializedName
    );

    private final String serializedName;

    SkillActionType(String serializedName) {
        this.serializedName = serializedName;
    }

    /**
     * Returns serialized action name for JSON codecs.
     *
     * @return snake_case action discriminator
     */
    public String serializedName() {
        return serializedName;
    }

    private static DataResult<SkillActionType> bySerializedName(String serializedName) {
        for (SkillActionType type : values()) {
            if (type.serializedName.equals(serializedName)) {
                return DataResult.success(type);
            }
        }

        return DataResult.error(() -> "Unknown action type: " + serializedName);
    }
}
