package kim.biryeong.esekai2.api.skill.value;

import com.mojang.serialization.Codec;
import kim.biryeong.esekai2.api.skill.execution.SkillUseContext;
import kim.biryeong.esekai2.api.stat.definition.StatDefinition;
import kim.biryeong.esekai2.api.stat.definition.StatRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.util.Objects;
import java.util.Set;

/**
 * Resolves a numeric value by reading an attacker stat directly.
 *
 * @param stat stat identifier to read from the attacker stat holder
 */
public record SkillStatValueExpression(Identifier stat) implements SkillValueExpression {
    /**
     * Codec used for stat-backed expressions.
     */
    public static final Codec<SkillStatValueExpression> CODEC = Identifier.CODEC.xmap(
            SkillStatValueExpression::new,
            SkillStatValueExpression::stat
    );

    public SkillStatValueExpression {
        Objects.requireNonNull(stat, "stat");
    }

    @Override
    public SkillValueExpressionType type() {
        return SkillValueExpressionType.STAT;
    }

    @Override
    public double resolve(SkillUseContext context, Set<Identifier> visited) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(visited, "visited");
        ResourceKey<StatDefinition> statKey = ResourceKey.create(StatRegistries.STAT, stat);
        return context.attackerStats().resolvedValue(statKey);
    }
}
