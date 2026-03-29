package kim.biryeong.esekai2.api.skill.calculation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kim.biryeong.esekai2.api.damage.breakdown.DamageBreakdown;
import kim.biryeong.esekai2.api.damage.breakdown.DamageType;
import kim.biryeong.esekai2.api.damage.critical.HitKind;
import kim.biryeong.esekai2.api.skill.execution.SkillUseContext;
import kim.biryeong.esekai2.api.skill.value.SkillValueExpression;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Datapack-backed typed calculation payload referenced by skill damage actions.
 *
 * @param hitKind base hit category used by the calculation
 * @param baseDamage typed base damage expressions contributed by the calculation
 * @param baseCriticalStrikeChance base critical strike chance expression expressed as a percent
 * @param baseCriticalStrikeMultiplier base critical strike multiplier expression expressed as a percent value
 */
public record SkillCalculationDefinition(
        HitKind hitKind,
        Map<DamageType, SkillValueExpression> baseDamage,
        SkillValueExpression baseCriticalStrikeChance,
        SkillValueExpression baseCriticalStrikeMultiplier
) {
    private static final Codec<Map<DamageType, SkillValueExpression>> BASE_DAMAGE_CODEC = Codec.unboundedMap(
            DamageType.CODEC,
            SkillValueExpression.CODEC
    ).xmap(
            map -> {
                Map<DamageType, SkillValueExpression> copy = new EnumMap<>(DamageType.class);
                copy.putAll(map);
                return Map.copyOf(copy);
            },
            map -> map
    );

    private static final Codec<SkillCalculationDefinition> BASE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            HitKind.CODEC.optionalFieldOf("hit_kind", HitKind.ATTACK).forGetter(SkillCalculationDefinition::hitKind),
            BASE_DAMAGE_CODEC.fieldOf("base_damage").forGetter(SkillCalculationDefinition::baseDamage),
            SkillValueExpression.CODEC.optionalFieldOf("base_critical_strike_chance", SkillValueExpression.constant(0.0))
                    .forGetter(SkillCalculationDefinition::baseCriticalStrikeChance),
            SkillValueExpression.CODEC.optionalFieldOf("base_critical_strike_multiplier", SkillValueExpression.constant(100.0))
                    .forGetter(SkillCalculationDefinition::baseCriticalStrikeMultiplier)
    ).apply(instance, SkillCalculationDefinition::new));

    /**
     * Validated codec used to decode skill calculation entries from datapacks.
     */
    public static final Codec<SkillCalculationDefinition> CODEC = BASE_CODEC.validate(SkillCalculationDefinition::validate);

    public SkillCalculationDefinition {
        Objects.requireNonNull(hitKind, "hitKind");
        Objects.requireNonNull(baseDamage, "baseDamage");
        Objects.requireNonNull(baseCriticalStrikeChance, "baseCriticalStrikeChance");
        Objects.requireNonNull(baseCriticalStrikeMultiplier, "baseCriticalStrikeMultiplier");
        baseDamage = Map.copyOf(baseDamage);
    }

    /**
     * Resolves the typed base damage payload for the provided runtime context.
     *
     * @param context runtime skill use context
     * @return resolved base damage breakdown
     */
    public DamageBreakdown resolveBaseDamage(SkillUseContext context) {
        Objects.requireNonNull(context, "context");
        DamageBreakdown resolved = DamageBreakdown.empty();
        for (Map.Entry<DamageType, SkillValueExpression> entry : baseDamage.entrySet()) {
            double amount = entry.getValue().resolve(context);
            if (!Double.isFinite(amount) || amount <= 0.0) {
                continue;
            }
            resolved = resolved.with(entry.getKey(), amount);
        }
        return resolved;
    }

    /**
     * Resolves the base critical strike chance for the provided runtime context.
     *
     * @param context runtime skill use context
     * @return resolved critical strike chance percentage
     */
    public double resolveBaseCriticalStrikeChance(SkillUseContext context) {
        Objects.requireNonNull(context, "context");
        double resolved = baseCriticalStrikeChance.resolve(context);
        if (!Double.isFinite(resolved)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(100.0, resolved));
    }

    /**
     * Resolves the base critical strike multiplier for the provided runtime context.
     *
     * @param context runtime skill use context
     * @return resolved critical strike multiplier percentage
     */
    public double resolveBaseCriticalStrikeMultiplier(SkillUseContext context) {
        Objects.requireNonNull(context, "context");
        double resolved = baseCriticalStrikeMultiplier.resolve(context);
        if (!Double.isFinite(resolved) || resolved < 100.0) {
            return 100.0;
        }
        return resolved;
    }

    private static DataResult<SkillCalculationDefinition> validate(SkillCalculationDefinition definition) {
        if (definition.baseDamage().isEmpty()) {
            return DataResult.error(() -> "base_damage must not be empty");
        }
        return DataResult.success(definition);
    }
}
