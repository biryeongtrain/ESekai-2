package kim.biryeong.esekai2.api.stat.modifier;

import kim.biryeong.esekai2.api.skill.definition.SkillDefinition;
import kim.biryeong.esekai2.api.skill.tag.SkillTag;

import java.util.Objects;
import java.util.Set;

/**
 * Public helpers for evaluating conditional stat modifiers against skill context.
 */
public final class ConditionalStatModifiers {
    private ConditionalStatModifiers() {
    }

    /**
     * Returns whether the provided conditional modifier matches the provided skill definition.
     *
     * @param skillDefinition skill definition to evaluate
     * @param modifier conditional modifier whose skill condition should be checked
     * @return {@code true} when the conditional modifier applies to the provided skill
     */
    public static boolean matches(SkillDefinition skillDefinition, ConditionalStatModifier modifier) {
        Objects.requireNonNull(skillDefinition, "skillDefinition");
        Objects.requireNonNull(modifier, "modifier");
        return modifier.matches(skillDefinition);
    }

    /**
     * Returns whether the provided conditional modifier matches the provided skill tag set.
     *
     * @param skillTags skill tags to evaluate
     * @param modifier conditional modifier whose skill condition should be checked
     * @return {@code true} when the conditional modifier applies to the provided skill tags
     */
    public static boolean matches(Set<SkillTag> skillTags, ConditionalStatModifier modifier) {
        Objects.requireNonNull(skillTags, "skillTags");
        Objects.requireNonNull(modifier, "modifier");
        return modifier.skillCondition().matches(skillTags);
    }
}
