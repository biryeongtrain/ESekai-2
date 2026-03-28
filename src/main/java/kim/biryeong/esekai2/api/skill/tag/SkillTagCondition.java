package kim.biryeong.esekai2.api.skill.tag;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Objects;
import java.util.Set;

/**
 * Skill tag predicate used to gate later stat or affix contributions by skill context.
 *
 * @param requiredTags tags that must all be present on the skill
 * @param excludedTags tags that must not be present on the skill
 */
public record SkillTagCondition(
        Set<SkillTag> requiredTags,
        Set<SkillTag> excludedTags
) {
    private static final Codec<SkillTagCondition> BASE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SkillTags.CODEC.optionalFieldOf("required_tags", Set.of()).forGetter(SkillTagCondition::requiredTags),
            SkillTags.CODEC.optionalFieldOf("excluded_tags", Set.of()).forGetter(SkillTagCondition::excludedTags)
    ).apply(instance, SkillTagCondition::new));

    /**
     * Validated codec used to decode skill tag conditions from datapacks and test fixtures.
     */
    public static final Codec<SkillTagCondition> CODEC = BASE_CODEC.validate(SkillTagCondition::validate);

    public SkillTagCondition {
        Objects.requireNonNull(requiredTags, "requiredTags");
        Objects.requireNonNull(excludedTags, "excludedTags");

        requiredTags = SkillTags.copyOf(requiredTags);
        excludedTags = SkillTags.copyOf(excludedTags);
    }

    /**
     * Returns whether the provided skill tags satisfy this condition.
     *
     * @param tags skill tags to inspect
     * @return {@code true} when required tags are all present and excluded tags are all absent
     */
    public boolean matches(Set<SkillTag> tags) {
        Objects.requireNonNull(tags, "tags");
        return SkillTags.containsAll(tags, requiredTags) && !SkillTags.intersects(tags, excludedTags);
    }

    private static DataResult<SkillTagCondition> validate(SkillTagCondition condition) {
        if (SkillTags.intersects(condition.requiredTags(), condition.excludedTags())) {
            return DataResult.error(() -> "required_tags and excluded_tags must not overlap");
        }

        return DataResult.success(condition);
    }
}
