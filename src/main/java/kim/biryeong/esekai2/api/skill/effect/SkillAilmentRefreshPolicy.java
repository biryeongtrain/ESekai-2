package kim.biryeong.esekai2.api.skill.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import kim.biryeong.esekai2.api.ailment.AilmentType;

/**
 * Controls how one ailment application refreshes an existing attachment-backed ailment.
 */
public enum SkillAilmentRefreshPolicy {
    STRONGER_ONLY("stronger_only"),
    LONGER_ONLY("longer_only"),
    OVERWRITE("overwrite");

    /**
     * Codec used by typed skill action payloads.
     */
    public static final Codec<SkillAilmentRefreshPolicy> CODEC = Codec.STRING.comapFlatMap(
            SkillAilmentRefreshPolicy::bySerializedName,
            SkillAilmentRefreshPolicy::serializedName
    );

    private final String serializedName;

    SkillAilmentRefreshPolicy(String serializedName) {
        this.serializedName = serializedName;
    }

    /**
     * Returns the stable datapack-facing name for this policy.
     *
     * @return snake_case policy identifier
     */
    public String serializedName() {
        return serializedName;
    }

    /**
     * Returns the default refresh policy for the supplied built-in ailment type.
     *
     * @param ailmentType ailment whose default policy should be used
     * @return default policy for the ailment
     */
    public static SkillAilmentRefreshPolicy defaultFor(AilmentType ailmentType) {
        return switch (ailmentType) {
            case IGNITE, SHOCK, POISON, BLEED, CHILL -> STRONGER_ONLY;
            case FREEZE, STUN -> LONGER_ONLY;
        };
    }

    /**
     * Looks up a policy by serialized name.
     *
     * @param serializedName datapack-facing policy id
     * @return parsed policy, or {@code null} when unknown
     */
    public static SkillAilmentRefreshPolicy fromSerializedName(String serializedName) {
        for (SkillAilmentRefreshPolicy policy : values()) {
            if (policy.serializedName.equals(serializedName)) {
                return policy;
            }
        }
        return null;
    }

    private static DataResult<SkillAilmentRefreshPolicy> bySerializedName(String serializedName) {
        SkillAilmentRefreshPolicy parsed = fromSerializedName(serializedName);
        if (parsed != null) {
            return DataResult.success(parsed);
        }
        return DataResult.error(() -> "Unknown ailment refresh policy: " + serializedName);
    }
}
