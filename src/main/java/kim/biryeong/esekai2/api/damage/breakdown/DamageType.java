package kim.biryeong.esekai2.api.damage.breakdown;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

/**
 * Declares the hardcoded damage categories used by ESekai combat systems.
 *
 * <p>These types are stable API constants shared by later payload, mitigation, and calculation
 * layers. They are intentionally not datapack-driven.</p>
 */
public enum DamageType {
    /**
     * Physical damage mitigated by later physical defense systems.
     */
    PHYSICAL("physical", false),
    /**
     * Fire damage mitigated by later elemental resistance systems.
     */
    FIRE("fire", false),
    /**
     * Cold damage mitigated by later elemental resistance systems.
     */
    COLD("cold", false),
    /**
     * Lightning damage mitigated by later elemental resistance systems.
     */
    LIGHTNING("lightning", false),
    /**
     * Chaos damage mitigated by later chaos resistance systems.
     */
    CHAOS("chaos", false),
    /**
     * Fixed damage that bypasses later resistance and armour mitigation systems.
     */
    FIXED("fixed", true);

    /**
     * Codec used to serialize and deserialize stable damage type identifiers.
     */
    public static final Codec<DamageType> CODEC = Codec.STRING.comapFlatMap(DamageType::fromId, DamageType::id);

    private final String id;
    private final boolean bypassesMitigation;

    DamageType(String id, boolean bypassesMitigation) {
        this.id = id;
        this.bypassesMitigation = bypassesMitigation;
    }

    /**
     * Returns the stable serialized identifier used to reference this damage type.
     *
     * @return stable lower-case identifier for this damage type
     */
    public String id() {
        return id;
    }

    /**
     * Returns whether this damage type bypasses later mitigation layers.
     *
     * @return {@code true} when mitigation should be bypassed
     */
    public boolean bypassesMitigation() {
        return bypassesMitigation;
    }

    private static DataResult<DamageType> fromId(String id) {
        for (DamageType damageType : values()) {
            if (damageType.id.equals(id)) {
                return DataResult.success(damageType);
            }
        }

        return DataResult.error(() -> "Unknown damage type: " + id);
    }
}
