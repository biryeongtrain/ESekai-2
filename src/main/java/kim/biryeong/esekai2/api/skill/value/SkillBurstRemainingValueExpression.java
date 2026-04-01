package kim.biryeong.esekai2.api.skill.value;

import kim.biryeong.esekai2.api.skill.execution.SkillExecutionContext;
import kim.biryeong.esekai2.api.skill.execution.SkillUseContext;
import kim.biryeong.esekai2.impl.skill.execution.PlayerSkillRuntimeStateResolver;
import net.minecraft.resources.Identifier;

import java.util.Objects;
import java.util.Set;

/**
 * Reads the remaining burst follow-up casts for the current prepared skill.
 */
public final class SkillBurstRemainingValueExpression implements SkillValueExpression {
    @Override
    public SkillValueExpressionType type() {
        return SkillValueExpressionType.BURST_REMAINING;
    }

    @Override
    public double resolve(SkillUseContext context, Set<Identifier> visited) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(visited, "visited");
        return context.activeSkillId()
                .map(skillId -> (double) context.playerStateLookup().burstRemainingCasts(skillId))
                .orElse(0.0);
    }

    @Override
    public double resolve(SkillExecutionContext context) {
        Objects.requireNonNull(context, "context");
        return PlayerSkillRuntimeStateResolver.burstRemaining(context);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof SkillBurstRemainingValueExpression;
    }

    @Override
    public int hashCode() {
        return SkillValueExpressionType.BURST_REMAINING.hashCode();
    }

    @Override
    public String toString() {
        return type().serializedName();
    }
}
