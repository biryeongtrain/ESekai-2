package kim.biryeong.esekai2.api.skill.tag;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

/**
 * Stable built-in skill tags used by ESekai's current skill and modifier foundations.
 */
public enum SkillTag {
    ATTACK("attack"),
    SPELL("spell"),
    PROJECTILE("projectile"),
    MELEE("melee"),
    AOE("aoe"),
    MINION("minion"),
    TOTEM("totem"),
    TRAP("trap"),
    MINE("mine");

    /**
     * Codec used by datapacks and fixtures that refer to a skill tag.
     */
    public static final Codec<SkillTag> CODEC = Codec.STRING.comapFlatMap(SkillTag::bySerializedName, SkillTag::serializedName);

    private final String serializedName;

    SkillTag(String serializedName) {
        this.serializedName = serializedName;
    }

    /**
     * Returns the stable serialized id used in datapacks.
     *
     * @return lower-case serialized skill tag id
     */
    public String serializedName() {
        return serializedName;
    }

    private static DataResult<SkillTag> bySerializedName(String serializedName) {
        for (SkillTag tag : values()) {
            if (tag.serializedName.equals(serializedName)) {
                return DataResult.success(tag);
            }
        }

        return DataResult.error(() -> "Unknown skill tag: " + serializedName);
    }
}
