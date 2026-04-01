package kim.biryeong.esekai2.api.skill.support;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kim.biryeong.esekai2.api.skill.definition.graph.SkillAction;
import kim.biryeong.esekai2.api.skill.tag.SkillTag;
import kim.biryeong.esekai2.api.skill.tag.SkillTagCondition;
import kim.biryeong.esekai2.api.skill.tag.SkillTags;
import kim.biryeong.esekai2.api.stat.modifier.ConditionalStatModifier;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A single tag-gated support effect slice.
 *
 * <p>Each effect applies independently when its tag condition matches the source skill tags.</p>
 *
 * @param skillCondition tag requirement/exclusion gate for this effect
 * @param addedTags additional skill tags supplied by this support effect
 * @param addedConditionalStatModifiers conditional stat modifiers injected by this effect
 * @param configOverrides support-level config rewrites injected by this effect
 * @param actionParameterOverrides typed field-path rewrites for matched skill actions
 * @param appendedActions additional actions appended by this support effect
 * @param appendedRules additional rules appended to one supported route bucket
 */
public record SkillSupportEffect(
        SkillTagCondition skillCondition,
        Set<SkillTag> addedTags,
        List<ConditionalStatModifier> addedConditionalStatModifiers,
        List<SkillConfigOverride> configOverrides,
        List<SkillActionOverride> actionParameterOverrides,
        List<SkillAction> appendedActions,
        List<SkillSupportRuleAppend> appendedRules
) {
    private static final Codec<SkillSupportEffect> BASE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SkillTagCondition.CODEC.optionalFieldOf("skill_condition", new SkillTagCondition(Set.of(), Set.of()))
                    .forGetter(SkillSupportEffect::skillCondition),
            SkillTags.CODEC.optionalFieldOf("added_tags", Set.of()).forGetter(SkillSupportEffect::addedTags),
            Codec.list(ConditionalStatModifier.CODEC).optionalFieldOf("added_conditional_stat_modifiers", List.of())
                    .forGetter(SkillSupportEffect::addedConditionalStatModifiers),
            Codec.list(SkillConfigOverride.CODEC).optionalFieldOf("config_overrides", List.of())
                    .forGetter(SkillSupportEffect::configOverrides),
            Codec.list(SkillActionOverride.CODEC).optionalFieldOf("action_parameter_overrides", List.of())
                    .forGetter(SkillSupportEffect::actionParameterOverrides),
            Codec.list(SkillAction.CODEC).optionalFieldOf("appended_actions", List.of())
                    .forGetter(SkillSupportEffect::appendedActions),
            Codec.list(SkillSupportRuleAppend.CODEC).optionalFieldOf("appended_rules", List.of())
                    .forGetter(SkillSupportEffect::appendedRules)
    ).apply(instance, SkillSupportEffect::new));

    /**
     * Validated codec used to decode support effects from datapacks and fixtures.
     */
    public static final Codec<SkillSupportEffect> CODEC = BASE_CODEC.validate(SkillSupportEffect::validate);

    public SkillSupportEffect {
        Objects.requireNonNull(skillCondition, "skillCondition");
        Objects.requireNonNull(addedTags, "addedTags");
        Objects.requireNonNull(addedConditionalStatModifiers, "addedConditionalStatModifiers");
        Objects.requireNonNull(configOverrides, "configOverrides");
        Objects.requireNonNull(actionParameterOverrides, "actionParameterOverrides");
        Objects.requireNonNull(appendedActions, "appendedActions");
        Objects.requireNonNull(appendedRules, "appendedRules");

        addedTags = SkillTags.copyOf(addedTags);
        addedConditionalStatModifiers = List.copyOf(addedConditionalStatModifiers);
        configOverrides = List.copyOf(configOverrides);
        actionParameterOverrides = List.copyOf(actionParameterOverrides);
        appendedActions = List.copyOf(appendedActions);
        appendedRules = List.copyOf(appendedRules);
    }

    /**
     * Compatibility constructor retaining the pre-config-override surface.
     *
     * @param skillCondition tag requirement/exclusion gate for this effect
     * @param addedTags additional skill tags supplied by this support effect
     * @param addedConditionalStatModifiers conditional stat modifiers injected by this effect
     * @param actionParameterOverrides typed field-path rewrites for matched skill actions
     * @param appendedActions additional actions appended by this support effect
     * @param appendedRules additional rules appended to one supported route bucket
     */
    public SkillSupportEffect(
            SkillTagCondition skillCondition,
            Set<SkillTag> addedTags,
            List<ConditionalStatModifier> addedConditionalStatModifiers,
            List<SkillActionOverride> actionParameterOverrides,
            List<SkillAction> appendedActions,
            List<SkillSupportRuleAppend> appendedRules
    ) {
        this(skillCondition, addedTags, addedConditionalStatModifiers, List.of(), actionParameterOverrides, appendedActions, appendedRules);
    }

    /**
     * Returns whether this effect's condition matches the supplied skill tags.
     *
     * @param skillTags resolved tags from a skill definition
     * @return {@code true} when the skill satisfies this effect's condition
     */
    public boolean matches(Set<SkillTag> skillTags) {
        return skillCondition.matches(skillTags);
    }

    private static DataResult<SkillSupportEffect> validate(SkillSupportEffect effect) {
        if (effect.addedTags().isEmpty()
                && effect.addedConditionalStatModifiers().isEmpty()
                && effect.configOverrides().isEmpty()
                && effect.actionParameterOverrides().isEmpty()
                && effect.appendedActions().isEmpty()
                && effect.appendedRules().isEmpty()) {
            return DataResult.error(() -> "support effect must contribute at least one modification");
        }
        return DataResult.success(effect);
    }
}
