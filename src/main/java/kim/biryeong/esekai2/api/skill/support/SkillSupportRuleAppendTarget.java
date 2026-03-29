package kim.biryeong.esekai2.api.skill.support;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Objects;

/**
 * Target descriptor for support-appended rules.
 *
 * @param type route category that should receive the appended rules
 * @param componentId entity component identifier when targeting one component bucket
 */
public record SkillSupportRuleAppendTarget(
        SkillSupportRuleTargetType type,
        String componentId
) {
    private static final Codec<SkillSupportRuleAppendTarget> BASE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SkillSupportRuleTargetType.CODEC.fieldOf("type").forGetter(SkillSupportRuleAppendTarget::type),
            Codec.STRING.optionalFieldOf("component_id", "").forGetter(SkillSupportRuleAppendTarget::componentId)
    ).apply(instance, SkillSupportRuleAppendTarget::new));

    /**
     * Validated codec used by support datapacks.
     */
    public static final Codec<SkillSupportRuleAppendTarget> CODEC = BASE_CODEC.validate(SkillSupportRuleAppendTarget::validate);

    public SkillSupportRuleAppendTarget {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(componentId, "componentId");
    }

    private static DataResult<SkillSupportRuleAppendTarget> validate(SkillSupportRuleAppendTarget target) {
        if (target.type() == SkillSupportRuleTargetType.ON_CAST && !target.componentId().isBlank()) {
            return DataResult.error(() -> "component_id must be empty when target type is on_cast");
        }

        if (target.type() == SkillSupportRuleTargetType.ENTITY_COMPONENT && target.componentId().isBlank()) {
            return DataResult.error(() -> "component_id is required when target type is entity_component");
        }

        return DataResult.success(target);
    }
}
