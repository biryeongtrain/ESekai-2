package kim.biryeong.esekai2.api.skill.value;

import kim.biryeong.esekai2.api.skill.execution.SkillUseContext;
import net.minecraft.resources.Identifier;

import java.util.Objects;
import java.util.Set;

/**
 * Reads the resolved prepared resource cost for the current skill.
 */
public final class SkillResourceCostValueExpression implements SkillValueExpression {
    @Override
    public SkillValueExpressionType type() {
        return SkillValueExpressionType.RESOURCE_COST;
    }

    @Override
    public double resolve(SkillUseContext context, Set<Identifier> visited) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(visited, "visited");
        return context.preparedStateLookup().resourceCost();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof SkillResourceCostValueExpression;
    }

    @Override
    public int hashCode() {
        return SkillValueExpressionType.RESOURCE_COST.hashCode();
    }

    @Override
    public String toString() {
        return type().serializedName();
    }
}
