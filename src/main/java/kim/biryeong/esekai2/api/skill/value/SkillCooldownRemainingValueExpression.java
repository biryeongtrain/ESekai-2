package kim.biryeong.esekai2.api.skill.value;

import kim.biryeong.esekai2.api.skill.execution.SkillExecutionContext;
import kim.biryeong.esekai2.api.skill.execution.SkillUseContext;
import kim.biryeong.esekai2.impl.skill.execution.PlayerSkillRuntimeStateResolver;
import net.minecraft.resources.Identifier;

import java.util.Objects;
import java.util.Set;

/**
 * Reads the remaining cooldown ticks for the current prepared skill.
 */
public final class SkillCooldownRemainingValueExpression implements SkillValueExpression {
    @Override
    public SkillValueExpressionType type() {
        return SkillValueExpressionType.COOLDOWN_REMAINING;
    }

    @Override
    public double resolve(SkillUseContext context, Set<Identifier> visited) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(visited, "visited");
        return context.activeSkillId()
                .map(skillId -> (double) context.playerStateLookup().cooldownRemainingTicks(skillId))
                .orElse(0.0);
    }

    @Override
    public double resolve(SkillExecutionContext context) {
        Objects.requireNonNull(context, "context");
        return PlayerSkillRuntimeStateResolver.cooldownRemainingTicks(context);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof SkillCooldownRemainingValueExpression;
    }

    @Override
    public int hashCode() {
        return SkillValueExpressionType.COOLDOWN_REMAINING.hashCode();
    }

    @Override
    public String toString() {
        return type().serializedName();
    }
}
