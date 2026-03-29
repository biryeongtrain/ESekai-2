package kim.biryeong.esekai2.api.skill.calculation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import kim.biryeong.esekai2.api.damage.breakdown.DamageBreakdown;
import kim.biryeong.esekai2.api.damage.breakdown.DamageType;
import kim.biryeong.esekai2.api.skill.execution.SkillUseContext;
import kim.biryeong.esekai2.api.skill.value.SkillValueExpression;

import java.util.Map;
import java.util.Objects;

/**
 * Typed damage payload made of per-damage-type numeric expressions.
 *
 * @param components typed damage expressions keyed by damage type
 */
public record SkillDamageExpression(
        Map<DamageType, SkillValueExpression> components
) {
    private static final Codec<SkillDamageExpression> BASE_CODEC = Codec.unboundedMap(DamageType.CODEC, SkillValueExpression.CODEC)
            .xmap(SkillDamageExpression::new, SkillDamageExpression::components);

    /**
     * Validated codec used to decode typed damage payloads from datapacks.
     */
    public static final Codec<SkillDamageExpression> CODEC = BASE_CODEC.validate(SkillDamageExpression::validate);

    public SkillDamageExpression {
        Objects.requireNonNull(components, "components");
        components = Map.copyOf(components);
        for (Map.Entry<DamageType, SkillValueExpression> entry : components.entrySet()) {
            Objects.requireNonNull(entry.getKey(), "damageType");
            Objects.requireNonNull(entry.getValue(), "expression");
        }
    }

    /**
     * Resolves the damage payload for the provided runtime context.
     *
     * @param context runtime skill use context
     * @return resolved typed damage breakdown
     */
    public DamageBreakdown resolve(SkillUseContext context) {
        Objects.requireNonNull(context, "context");

        DamageBreakdown resolved = DamageBreakdown.empty();
        for (Map.Entry<DamageType, SkillValueExpression> entry : components.entrySet()) {
            double amount = entry.getValue().resolve(context);
            if (!Double.isFinite(amount) || amount <= 0.0) {
                continue;
            }
            resolved = resolved.with(entry.getKey(), amount);
        }
        return resolved;
    }

    /**
     * Returns whether this payload contains any typed components.
     *
     * @return {@code true} when no typed damage entries are present
     */
    public boolean isEmpty() {
        return components.isEmpty();
    }

    private static DataResult<SkillDamageExpression> validate(SkillDamageExpression expression) {
        if (expression.components().isEmpty()) {
            return DataResult.error(() -> "base_damage must not be empty");
        }
        return DataResult.success(expression);
    }
}
