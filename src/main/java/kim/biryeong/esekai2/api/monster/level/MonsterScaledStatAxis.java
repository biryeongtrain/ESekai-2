package kim.biryeong.esekai2.api.monster.level;

import com.mojang.serialization.Codec;

/**
 * Supported monster level table axes that can directly populate current stat holders.
 */
public enum MonsterScaledStatAxis {
    LIFE {
        @Override
        public double resolve(MonsterLevelProfile profile) {
            return profile.baseLife() * profile.effectiveLifeMultiplier();
        }
    },
    ACCURACY {
        @Override
        public double resolve(MonsterLevelProfile profile) {
            return profile.baseAccuracy();
        }
    },
    EVADE {
        @Override
        public double resolve(MonsterLevelProfile profile) {
            return profile.baseEvasion();
        }
    };

    public static final Codec<MonsterScaledStatAxis> CODEC = Codec.STRING.xmap(
            value -> MonsterScaledStatAxis.valueOf(value.toUpperCase()),
            value -> value.name().toLowerCase()
    );

    /**
     * Resolves the stat value represented by this axis from the provided monster level profile.
     *
     * @param profile resolved monster level profile
     * @return stat value to inject into the holder
     */
    public abstract double resolve(MonsterLevelProfile profile);
}
