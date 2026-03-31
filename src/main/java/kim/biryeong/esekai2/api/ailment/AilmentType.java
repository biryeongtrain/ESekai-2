package kim.biryeong.esekai2.api.ailment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import kim.biryeong.esekai2.api.damage.breakdown.DamageType;
import kim.biryeong.esekai2.api.skill.effect.SkillAilmentRefreshPolicy;
import net.minecraft.resources.Identifier;

/**
 * Stable built-in ailment ids used by ESekai's first MobEffect-backed ailment foundation.
 */
public enum AilmentType {
    IGNITE("ignite"),
    SHOCK("shock"),
    POISON("poison"),
    BLEED("bleed"),
    CHILL("chill"),
    FREEZE("freeze"),
    STUN("stun");

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
        return switch (this) {
            case IGNITE, POISON, BLEED -> true;
            case SHOCK, CHILL, FREEZE, STUN -> false;
        };
    }

    /**
     * Returns whether this ailment's replacement semantics are driven by numeric potency.
     *
     * @return {@code true} when stronger payloads should replace weaker ones
     */
    public boolean usesPotency() {
        return switch (this) {
            case IGNITE, SHOCK, POISON, BLEED, CHILL -> true;
            case FREEZE, STUN -> false;
        };
    }

    /**
     * Returns whether this ailment blocks active skill execution while present.
     *
     * @return {@code true} when server-side cast execution should no-op
     */
    public boolean blocksSkillExecution() {
        return this == FREEZE || this == STUN;
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
            case SHOCK, CHILL, FREEZE, STUN -> null;
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

    /**
     * Returns the default replacement policy used when apply_ailment omits an explicit refresh policy.
     *
     * @return ailment refresh policy compatible with the ailment's runtime semantics
     */
    public SkillAilmentRefreshPolicy defaultRefreshPolicy() {
        return switch (this) {
            case IGNITE, SHOCK, POISON, BLEED, CHILL -> SkillAilmentRefreshPolicy.STRONGER_ONLY;
            case FREEZE, STUN -> SkillAilmentRefreshPolicy.LONGER_ONLY;
        };
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
