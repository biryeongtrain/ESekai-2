package kim.biryeong.esekai2.api.skill.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

/**
 * Broad purge policy used by {@code remove_effect} utility actions.
 */
public enum SkillEffectPurgeMode {
    POSITIVE("positive"),
    NEGATIVE("negative"),
    ALL("all");

    /**
     * Codec used by typed skill action payloads.
     */
    public static final Codec<SkillEffectPurgeMode> CODEC = Codec.STRING.comapFlatMap(
            SkillEffectPurgeMode::bySerializedName,
            SkillEffectPurgeMode::serializedName
    );

    private final String serializedName;

    SkillEffectPurgeMode(String serializedName) {
        this.serializedName = serializedName;
    }

    /**
     * Returns the stable datapack-facing name for this purge policy.
     *
     * @return snake_case purge identifier
     */
    public String serializedName() {
        return serializedName;
    }

    /**
     * Parses the provided datapack-facing name.
     *
     * @param serializedName datapack-facing purge name
     * @return parsed purge mode or {@code null} when the name is unknown
     */
    public static SkillEffectPurgeMode fromSerializedName(String serializedName) {
        if (serializedName == null || serializedName.isBlank()) {
            return null;
        }
        for (SkillEffectPurgeMode mode : values()) {
            if (mode.serializedName.equals(serializedName)) {
                return mode;
            }
        }
        return null;
    }

    private static DataResult<SkillEffectPurgeMode> bySerializedName(String serializedName) {
        SkillEffectPurgeMode parsed = fromSerializedName(serializedName);
        if (parsed != null) {
            return DataResult.success(parsed);
        }
        return DataResult.error(() -> "Unknown skill effect purge mode: " + serializedName);
    }
}
