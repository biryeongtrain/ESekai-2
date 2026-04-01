package kim.biryeong.esekai2.api.skill.value;

import kim.biryeong.esekai2.api.skill.execution.SkillUseContext;
import net.minecraft.resources.Identifier;

import java.util.Objects;
import java.util.Set;

/**
 * Reads the resolved prepared use time in ticks for the current skill.
 */
public final class SkillUseTimeTicksValueExpression implements SkillValueExpression {
    @Override
    public SkillValueExpressionType type() {
        return SkillValueExpressionType.USE_TIME_TICKS;
    }

    @Override
    public double resolve(SkillUseContext context, Set<Identifier> visited) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(visited, "visited");
        return context.preparedStateLookup().useTimeTicks();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof SkillUseTimeTicksValueExpression;
    }

    @Override
    public int hashCode() {
        return SkillValueExpressionType.USE_TIME_TICKS.hashCode();
    }

    @Override
    public String toString() {
        return type().serializedName();
    }
}
