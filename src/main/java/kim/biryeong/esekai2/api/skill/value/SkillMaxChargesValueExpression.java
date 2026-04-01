package kim.biryeong.esekai2.api.skill.value;

import kim.biryeong.esekai2.api.skill.execution.SkillUseContext;
import net.minecraft.resources.Identifier;

import java.util.Objects;
import java.util.Set;

/**
 * Reads the configured maximum charges for the current prepared skill.
 */
public final class SkillMaxChargesValueExpression implements SkillValueExpression {
    @Override
    public SkillValueExpressionType type() {
        return SkillValueExpressionType.MAX_CHARGES;
    }

    @Override
    public double resolve(SkillUseContext context, Set<Identifier> visited) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(visited, "visited");
        return context.preparedStateLookup().maxCharges();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof SkillMaxChargesValueExpression;
    }

    @Override
    public int hashCode() {
        return SkillValueExpressionType.MAX_CHARGES.hashCode();
    }

    @Override
    public String toString() {
        return type().serializedName();
    }
}
