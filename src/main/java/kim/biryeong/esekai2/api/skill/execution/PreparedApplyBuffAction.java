package kim.biryeong.esekai2.api.skill.execution;

import kim.biryeong.esekai2.api.skill.definition.graph.SkillPredicate;
import kim.biryeong.esekai2.api.skill.effect.MobEffectRefreshPolicy;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Objects;

/**
 * Prepared MobEffect action used by both {@code apply_effect} and legacy {@code apply_buff} skill nodes.
 */
public record PreparedApplyBuffAction(
        Identifier effectId,
        int durationTicks,
        int amplifier,
        boolean ambient,
        boolean showParticles,
        boolean showIcon,
        String serializedActionType,
        MobEffectRefreshPolicy refreshPolicy,
        List<SkillPredicate> enPreds
) implements PreparedSkillAction {
    public PreparedApplyBuffAction {
        Objects.requireNonNull(effectId, "effectId");
        Objects.requireNonNull(serializedActionType, "serializedActionType");
        Objects.requireNonNull(refreshPolicy, "refreshPolicy");
        Objects.requireNonNull(enPreds, "enPreds");
        enPreds = List.copyOf(enPreds);
        if (serializedActionType.isBlank()) {
            throw new IllegalArgumentException("serializedActionType must not be blank");
        }
        if (durationTicks < 0) {
            throw new IllegalArgumentException("durationTicks must be >= 0");
        }
        if (amplifier < 0) {
            throw new IllegalArgumentException("amplifier must be >= 0");
        }
    }

    public PreparedApplyBuffAction(
            Identifier effectId,
            int durationTicks,
            int amplifier,
            boolean ambient,
            boolean showParticles,
            boolean showIcon,
            String serializedActionType,
            MobEffectRefreshPolicy refreshPolicy
    ) {
        this(effectId, durationTicks, amplifier, ambient, showParticles, showIcon, serializedActionType, refreshPolicy, List.of());
    }

    @Override
    public String actionType() {
        return serializedActionType;
    }
}
