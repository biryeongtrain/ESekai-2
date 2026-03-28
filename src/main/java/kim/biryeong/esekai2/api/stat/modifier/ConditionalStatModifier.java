package kim.biryeong.esekai2.api.stat.modifier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kim.biryeong.esekai2.api.skill.definition.SkillDefinition;
import kim.biryeong.esekai2.api.skill.tag.SkillTagCondition;

import java.util.Objects;

/**
 * Wraps a stat modifier with skill-tag conditions that decide when it should apply.
 *
 * @param modifier concrete stat modifier to apply when the skill condition matches
 * @param skillCondition skill tag condition that gates the wrapped modifier
 */
public record ConditionalStatModifier(
        StatModifier modifier,
        SkillTagCondition skillCondition
) {
    /**
     * Codec used to decode conditional stat modifiers from datapacks and fixtures.
     */
    public static final Codec<ConditionalStatModifier> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            StatModifier.CODEC.fieldOf("modifier").forGetter(ConditionalStatModifier::modifier),
            SkillTagCondition.CODEC.fieldOf("skill_condition").forGetter(ConditionalStatModifier::skillCondition)
    ).apply(instance, ConditionalStatModifier::new));

    public ConditionalStatModifier {
        Objects.requireNonNull(modifier, "modifier");
        Objects.requireNonNull(skillCondition, "skillCondition");
    }

    /**
     * Returns whether this conditional modifier matches the provided skill definition.
     *
     * @param skillDefinition skill definition to inspect
     * @return {@code true} when the wrapped skill condition matches the skill's tags
     */
    public boolean matches(SkillDefinition skillDefinition) {
        Objects.requireNonNull(skillDefinition, "skillDefinition");
        return skillCondition.matches(skillDefinition.config().tags());
    }
}
