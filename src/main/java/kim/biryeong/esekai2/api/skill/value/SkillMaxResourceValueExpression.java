package kim.biryeong.esekai2.api.skill.value;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kim.biryeong.esekai2.api.player.resource.PlayerResourceIds;
import kim.biryeong.esekai2.api.skill.definition.graph.SkillPredicateSubject;
import kim.biryeong.esekai2.api.skill.execution.SkillUseContext;
import kim.biryeong.esekai2.api.stat.holder.StatHolder;
import kim.biryeong.esekai2.impl.player.resource.PlayerResourceService;
import kim.biryeong.esekai2.impl.runtime.ServerRuntimeAccess;
import net.minecraft.resources.Identifier;

import java.util.Objects;
import java.util.Set;

/**
 * Reads the maximum amount of one named player resource for a selected subject.
 *
 * <p>The max amount is resolved from the registered resource definition's max-stat binding and the
 * stat holders already present in {@link SkillUseContext}. Subject selection therefore follows the
 * existing skill graph conventions: {@code self} uses attacker stats and {@code target} /
 * {@code primary_target} use defender stats.</p>
 *
 * @param resource stable resource identifier
 * @param subject logical subject whose maximum resource amount should be inspected
 */
public record SkillMaxResourceValueExpression(
        String resource,
        SkillPredicateSubject subject
) implements SkillValueExpression {
    /**
     * Codec used for max-resource expressions.
     */
    public static final Codec<SkillMaxResourceValueExpression> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("resource")
                    .forGetter(SkillMaxResourceValueExpression::resource),
            SkillPredicateSubject.CODEC.optionalFieldOf("subject", SkillPredicateSubject.SELF)
                    .forGetter(SkillMaxResourceValueExpression::subject)
    ).apply(instance, SkillMaxResourceValueExpression::new));

    public SkillMaxResourceValueExpression {
        PlayerResourceIds.requireUsable(resource);
        Objects.requireNonNull(subject, "subject");
    }

    @Override
    public SkillValueExpressionType type() {
        return SkillValueExpressionType.RESOURCE_MAX;
    }

    @Override
    public double resolve(SkillUseContext context, Set<Identifier> visited) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(visited, "visited");

        return context.resourceLookup().resolve(resource, subject)
                .map(resolved -> resolved.maxAmount())
                .orElseGet(() -> ServerRuntimeAccess.currentServer()
                .flatMap(server -> PlayerResourceService.definition(server, resource))
                .map(definition -> selectStatHolder(context).resolvedValue(definition.maxStat()))
                .orElse(0.0));
    }

    private StatHolder selectStatHolder(SkillUseContext context) {
        return switch (subject) {
            case SELF -> context.attackerStats();
            case TARGET, PRIMARY_TARGET -> context.defenderStats();
        };
    }
}
