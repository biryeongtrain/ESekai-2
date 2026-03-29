package kim.biryeong.esekai2.api.skill.value;

import com.mojang.serialization.Codec;
import kim.biryeong.esekai2.api.skill.execution.SkillUseContext;
import net.minecraft.resources.Identifier;

import java.util.Set;

/**
 * Inline numeric constant expression.
 *
 * @param value resolved numeric constant
 */
public record SkillLiteralValueExpression(double value) implements SkillValueExpression {
    /**
     * Codec used for constant value expressions.
     */
    public static final Codec<SkillLiteralValueExpression> CODEC = Codec.DOUBLE.xmap(
            SkillLiteralValueExpression::new,
            SkillLiteralValueExpression::value
    );

    @Override
    public SkillValueExpressionType type() {
        return SkillValueExpressionType.CONSTANT;
    }

    @Override
    public double resolve(SkillUseContext context, Set<Identifier> visited) {
        return value;
    }
}
