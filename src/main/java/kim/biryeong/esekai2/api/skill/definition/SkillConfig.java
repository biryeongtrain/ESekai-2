package kim.biryeong.esekai2.api.skill.definition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kim.biryeong.esekai2.api.skill.tag.SkillTag;
import kim.biryeong.esekai2.api.skill.tag.SkillTags;

import java.util.Objects;
import java.util.Set;

/**
 * Runtime configuration and base tuning surface for a skill definition.
 *
 * <p>These fields are loaded from JSON and interpreted by the runtime execution layer.</p>
 *
 * @param castingWeapon requirement token checked by execution-side rule resolvers
 * @param resourceCost base resource cost before runtime modifiers are applied
 * @param castTimeTicks base use time in ticks before speed adjustments
 * @param cooldownTicks base cooldown in ticks before cooldown modifiers are applied
 * @param style high-level animation/style hint for downstream systems
 * @param castType cast flow classification hint
 * @param swingArm whether player arm swing should be triggered when casting
 * @param applyCastSpeedToCooldown whether cast speed should affect cooldown
 * @param timesToCast maximum casts in a chained action window
 * @param charges maximum stored charges for this skill
 * @param chargeRegen charge regen per second while active
 * @param tags static tags assigned in definition
 */
public record SkillConfig(
        String castingWeapon,
        double resourceCost,
        int castTimeTicks,
        int cooldownTicks,
        String style,
        String castType,
        boolean swingArm,
        boolean applyCastSpeedToCooldown,
        int timesToCast,
        int charges,
        double chargeRegen,
        Set<SkillTag> tags
) {
    private static final Codec<SkillConfig> BASE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("casting_weapon", "").forGetter(SkillConfig::castingWeapon),
            Codec.DOUBLE.optionalFieldOf("resource_cost", 0.0).forGetter(SkillConfig::resourceCost),
            Codec.INT.optionalFieldOf("cast_time_ticks", 0).forGetter(SkillConfig::castTimeTicks),
            Codec.INT.optionalFieldOf("cooldown_ticks", 0).forGetter(SkillConfig::cooldownTicks),
            Codec.STRING.optionalFieldOf("style", "").forGetter(SkillConfig::style),
            Codec.STRING.optionalFieldOf("cast_type", "").forGetter(SkillConfig::castType),
            Codec.BOOL.optionalFieldOf("swing_arm", false).forGetter(SkillConfig::swingArm),
            Codec.BOOL.optionalFieldOf("apply_cast_speed_to_cd", false).forGetter(SkillConfig::applyCastSpeedToCooldown),
            Codec.INT.optionalFieldOf("times_to_cast", 1).forGetter(SkillConfig::timesToCast),
            Codec.INT.optionalFieldOf("charges", 0).forGetter(SkillConfig::charges),
            Codec.DOUBLE.optionalFieldOf("charge_regen", 0.0).forGetter(SkillConfig::chargeRegen),
            SkillTags.CODEC.optionalFieldOf("tags", Set.of()).forGetter(SkillConfig::tags)
    ).apply(instance, SkillConfig::new));

    /**
     * Validated codec used to decode skill configuration blocks.
     */
    public static final Codec<SkillConfig> CODEC = BASE_CODEC.validate(SkillConfig::validate);

    public SkillConfig {
        Objects.requireNonNull(castingWeapon, "castingWeapon");
        Objects.requireNonNull(style, "style");
        Objects.requireNonNull(castType, "castType");
        Objects.requireNonNull(tags, "tags");
        tags = SkillTags.copyOf(tags);
    }

    private static DataResult<SkillConfig> validate(SkillConfig config) {
        if (!Double.isFinite(config.resourceCost()) || config.resourceCost() < 0.0) {
            return DataResult.error(() -> "resource_cost must be finite and >= 0");
        }
        if (config.castTimeTicks() < 0) {
            return DataResult.error(() -> "cast_time_ticks must be >= 0");
        }
        if (config.cooldownTicks() < 0) {
            return DataResult.error(() -> "cooldown_ticks must be >= 0");
        }
        if (config.timesToCast() < 1) {
            return DataResult.error(() -> "times_to_cast must be >= 1");
        }
        if (config.charges() < 0) {
            return DataResult.error(() -> "charges must be >= 0");
        }
        if (!Double.isFinite(config.chargeRegen()) || config.chargeRegen() < 0.0) {
            return DataResult.error(() -> "charge_regen must be finite and >= 0");
        }

        return DataResult.success(config);
    }

    /**
     * Shared defaults for newly introduced skill definitions before runtime mutates them.
     */
    public static final SkillConfig DEFAULT = new SkillConfig(
            "",
            0.0,
            0,
            0,
            "",
            "",
            false,
            false,
            1,
            0,
            0.0,
            Set.of()
    );
}
