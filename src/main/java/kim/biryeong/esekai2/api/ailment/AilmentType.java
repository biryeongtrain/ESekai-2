package kim.biryeong.esekai2.api.ailment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import kim.biryeong.esekai2.api.damage.breakdown.DamageType;
import net.minecraft.resources.Identifier;

/**
 * Stable built-in ailment ids used by ESekai's first MobEffect-backed ailment foundation.
 */
public enum AilmentType {
    IGNITE("ignite"),
    SHOCK("shock"),
    POISON("poison"),
    BLEED("bleed");

    /**
     * Codec used by skill datapacks and persistent ailment attachments.
     */
    public static final Codec<AilmentType> CODEC = Codec.STRING.comapFlatMap(AilmentType::bySerializedName, AilmentType::serializedName);

    private final String serializedName;

    AilmentType(String serializedName) {
        this.serializedName = serializedName;
    }

    /**
     * Returns the stable serialized name used in datapacks.
     *
     * @return lower-case ailment id
     */
    public String serializedName() {
        return serializedName;
    }

    /**
     * Returns whether this ailment applies periodic damage.
     *
     * @return {@code true} when the ailment uses the damage-over-time path
     */
    public boolean isDamageOverTime() {
        return this != SHOCK;
    }

    /**
     * Returns the typed damage category used by periodic ailment ticks.
     *
     * @return ailment tick damage category, or {@code null} when the ailment is not a DoT ailment
     */
    public DamageType damageType() {
        return switch (this) {
            case IGNITE -> DamageType.FIRE;
            case POISON -> DamageType.CHAOS;
            case BLEED -> DamageType.PHYSICAL;
            case SHOCK -> null;
        };
    }

    /**
     * Returns the registered MobEffect identifier used to represent this ailment.
     *
     * @return built-in ailment effect id
     */
    public Identifier effectId() {
        return Identifier.fromNamespaceAndPath("esekai2", serializedName);
    }

    private static DataResult<AilmentType> bySerializedName(String serializedName) {
        for (AilmentType type : values()) {
            if (type.serializedName.equals(serializedName)) {
                return DataResult.success(type);
            }
        }
        return DataResult.error(() -> "Unknown ailment type: " + serializedName);
    }
}
