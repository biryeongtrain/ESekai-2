package kim.biryeong.esekai2.api.skill.definition.graph;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Public graph node used by attached execution blocks.
 *
 * @param targets target selectors this rule can apply to
 * @param acts actions executed by this rule
 * @param ifs conditional branches that gate this rule execution
 * @param enPreds runtime predicates applied while the source entity exists
 */
public record SkillRule(
        Set<SkillTargetSelector> targets,
        List<SkillAction> acts,
        List<SkillCondition> ifs,
        List<SkillPredicate> enPreds
) {
    private static final Codec<Set<SkillTargetSelector>> TARGETS_CODEC = Codec.list(SkillTargetSelector.CODEC).xmap(
            HashSet::new,
            List::copyOf
    );

    private static final Codec<SkillRule> BASE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            TARGETS_CODEC.optionalFieldOf("targets", Set.of(SkillTargetSelector.DEFAULT_SELF)).forGetter(SkillRule::targets),
            SkillAction.CODEC.listOf().optionalFieldOf("acts", List.of()).forGetter(SkillRule::acts),
            SkillCondition.CODEC.listOf().optionalFieldOf("ifs", List.of()).forGetter(SkillRule::ifs),
            SkillPredicate.CODEC.listOf().optionalFieldOf("en_preds", List.of()).forGetter(SkillRule::enPreds)
    ).apply(instance, SkillRule::new));

    /**
     * Validated codec used to decode rule nodes.
     */
    public static final Codec<SkillRule> CODEC = BASE_CODEC;

    public SkillRule {
        Objects.requireNonNull(targets, "targets");
        Objects.requireNonNull(acts, "acts");
        Objects.requireNonNull(ifs, "ifs");
        Objects.requireNonNull(enPreds, "enPreds");

        targets = Set.copyOf(targets);
        acts = List.copyOf(acts);
        ifs = List.copyOf(ifs);
        enPreds = List.copyOf(enPreds);
    }
}
