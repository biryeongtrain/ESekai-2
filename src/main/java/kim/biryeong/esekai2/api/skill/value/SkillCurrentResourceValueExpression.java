package kim.biryeong.esekai2.api.skill.value;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kim.biryeong.esekai2.api.player.resource.PlayerResourceIds;
import kim.biryeong.esekai2.api.skill.definition.graph.SkillPredicateSubject;
import kim.biryeong.esekai2.api.skill.execution.SkillUseContext;
import net.minecraft.resources.Identifier;

import java.util.Objects;
import java.util.Set;

/**
 * Reads the current amount of one named player resource for a selected subject.
 *
 * @param resource stable resource identifier
 * @param subject logical subject whose current resource amount should be inspected
 */
public record SkillCurrentResourceValueExpression(
        String resource,
        SkillPredicateSubject subject
) implements SkillValueExpression {
    /**
     * Codec used for current-resource expressions.
     */
    public static final Codec<SkillCurrentResourceValueExpression> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("resource")
                    .forGetter(SkillCurrentResourceValueExpression::resource),
            SkillPredicateSubject.CODEC.optionalFieldOf("subject", SkillPredicateSubject.SELF)
                    .forGetter(SkillCurrentResourceValueExpression::subject)
    ).apply(instance, SkillCurrentResourceValueExpression::new));

    public SkillCurrentResourceValueExpression {
        PlayerResourceIds.requireUsable(resource);
        Objects.requireNonNull(subject, "subject");
    }

    @Override
    public SkillValueExpressionType type() {
        return SkillValueExpressionType.RESOURCE_CURRENT;
    }

    @Override
    public double resolve(SkillUseContext context, Set<Identifier> visited) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(visited, "visited");
        return context.resourceLookup().resolve(resource, subject)
                .map(resolved -> resolved.currentAmount())
                .orElse(0.0);
    }
}
