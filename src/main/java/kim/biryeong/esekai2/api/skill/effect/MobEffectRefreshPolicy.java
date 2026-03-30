package kim.biryeong.esekai2.api.skill.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

/**
 * Controls how one skill effect reapplies an existing MobEffect on the same target.
 */
public enum MobEffectRefreshPolicy {
    OVERWRITE("overwrite"),
    LONGER_ONLY("longer_only"),
    ADD_DURATION("add_duration");

    /**
     * Codec used by typed skill action payloads.
     */
    public static final Codec<MobEffectRefreshPolicy> CODEC = Codec.STRING.comapFlatMap(
            MobEffectRefreshPolicy::bySerializedName,
            MobEffectRefreshPolicy::serializedName
    );

    private final String serializedName;

    MobEffectRefreshPolicy(String serializedName) {
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

    private static DataResult<MobEffectRefreshPolicy> bySerializedName(String serializedName) {
        for (MobEffectRefreshPolicy policy : values()) {
            if (policy.serializedName.equals(serializedName)) {
                return DataResult.success(policy);
            }
        }

        return DataResult.error(() -> "Unknown mob effect refresh policy: " + serializedName);
    }
}
