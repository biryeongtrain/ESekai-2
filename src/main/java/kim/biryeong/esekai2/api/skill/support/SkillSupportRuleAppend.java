package kim.biryeong.esekai2.api.skill.support;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kim.biryeong.esekai2.api.skill.definition.graph.SkillRule;

import java.util.List;
import java.util.Objects;

/**
 * Bundle of appended rules targeted at one support-merge route bucket.
 *
 * @param target route bucket that should receive these rules
 * @param rules rules appended to the end of the target bucket
 */
public record SkillSupportRuleAppend(
        SkillSupportRuleAppendTarget target,
        List<SkillRule> rules
) {
    private static final Codec<SkillSupportRuleAppend> BASE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SkillSupportRuleAppendTarget.CODEC.fieldOf("target").forGetter(SkillSupportRuleAppend::target),
            Codec.list(SkillRule.CODEC).fieldOf("rules").forGetter(SkillSupportRuleAppend::rules)
    ).apply(instance, SkillSupportRuleAppend::new));

    /**
     * Validated codec used by support datapacks.
     */
    public static final Codec<SkillSupportRuleAppend> CODEC = BASE_CODEC.validate(SkillSupportRuleAppend::validate);

    public SkillSupportRuleAppend {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(rules, "rules");
        rules = List.copyOf(rules);
    }

    private static DataResult<SkillSupportRuleAppend> validate(SkillSupportRuleAppend append) {
        if (append.rules().isEmpty()) {
            return DataResult.error(() -> "rules must not be empty");
        }

        return DataResult.success(append);
    }
}
