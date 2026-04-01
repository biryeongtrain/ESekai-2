package kim.biryeong.esekai2.api.skill.value;

import kim.biryeong.esekai2.api.skill.execution.SkillUseContext;
import net.minecraft.resources.Identifier;

import java.util.Objects;
import java.util.Set;

/**
 * Reads the configured burst cast count for the current prepared skill.
 */
public final class SkillTimesToCastValueExpression implements SkillValueExpression {
    @Override
    public SkillValueExpressionType type() {
        return SkillValueExpressionType.TIMES_TO_CAST;
    }

    @Override
    public double resolve(SkillUseContext context, Set<Identifier> visited) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(visited, "visited");
        return context.preparedStateLookup().timesToCast();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof SkillTimesToCastValueExpression;
    }

    @Override
    public int hashCode() {
        return SkillValueExpressionType.TIMES_TO_CAST.hashCode();
    }

    @Override
    public String toString() {
        return type().serializedName();
    }
}
