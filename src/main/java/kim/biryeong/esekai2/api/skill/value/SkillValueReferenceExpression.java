package kim.biryeong.esekai2.api.skill.value;

import com.mojang.serialization.Codec;
import kim.biryeong.esekai2.api.skill.execution.SkillUseContext;
import net.minecraft.resources.Identifier;

import java.util.Objects;
import java.util.Set;

/**
 * Reference to a reusable datapack-backed skill value definition.
 *
 * @param valueId reusable value identifier
 */
public record SkillValueReferenceExpression(Identifier valueId) implements SkillValueExpression {
    /**
     * Codec used for reference expressions.
     */
    public static final Codec<SkillValueReferenceExpression> CODEC = Identifier.CODEC.xmap(
            SkillValueReferenceExpression::new,
            SkillValueReferenceExpression::valueId
    );

    public SkillValueReferenceExpression {
        Objects.requireNonNull(valueId, "valueId");
    }

    @Override
    public SkillValueExpressionType type() {
        return SkillValueExpressionType.REFERENCE;
    }

    @Override
    public double resolve(SkillUseContext context, Set<Identifier> visited) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(visited, "visited");

        if (!visited.add(valueId)) {
            return 0.0;
        }

        return context.valueLookup().resolve(valueId)
                .map(definition -> definition.resolve(context, visited))
                .orElse(0.0);
    }
}
