package kim.biryeong.esekai2.api.skill.value;

import kim.biryeong.esekai2.api.skill.execution.SkillExecutionContext;
import kim.biryeong.esekai2.api.skill.execution.SkillUseContext;
import kim.biryeong.esekai2.impl.skill.execution.PlayerSkillRuntimeStateResolver;
import net.minecraft.resources.Identifier;

import java.util.Objects;
import java.util.Set;

/**
 * Reads the currently available charges for the current prepared skill.
 */
public final class SkillChargesAvailableValueExpression implements SkillValueExpression {
    @Override
    public SkillValueExpressionType type() {
        return SkillValueExpressionType.CHARGES_AVAILABLE;
    }

    @Override
    public double resolve(SkillUseContext context, Set<Identifier> visited) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(visited, "visited");
        return context.activeSkillId()
                .map(skillId -> (double) context.playerStateLookup().availableCharges(skillId, context.activeSkillMaxCharges()))
                .orElse(0.0);
    }

    @Override
    public double resolve(SkillExecutionContext context) {
        Objects.requireNonNull(context, "context");
        return PlayerSkillRuntimeStateResolver.availableCharges(context);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof SkillChargesAvailableValueExpression;
    }

    @Override
    public int hashCode() {
        return SkillValueExpressionType.CHARGES_AVAILABLE.hashCode();
    }

    @Override
    public String toString() {
        return type().serializedName();
    }
}
