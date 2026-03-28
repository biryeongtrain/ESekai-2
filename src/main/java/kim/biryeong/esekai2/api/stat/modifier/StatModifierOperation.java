package kim.biryeong.esekai2.api.stat.modifier;

import com.mojang.serialization.Codec;

/**
 * Declares how a {@link StatModifier} contributes to the final value of a stat.
 */
public enum StatModifierOperation {
    /**
     * Adds a flat numeric amount to the stat.
     */
    ADD,
    /**
     * Adds a percentage to the stat's additive increased bucket.
     */
    INCREASED,
    /**
     * Multiplies the stat through a separate multiplicative bucket.
     */
    MORE;

    /**
     * Codec used to load modifier operations from datapacks and test fixtures.
     */
    public static final Codec<StatModifierOperation> CODEC = Codec.STRING.xmap(
            value -> StatModifierOperation.valueOf(value.toUpperCase()),
            value -> value.name().toLowerCase()
    );
}
