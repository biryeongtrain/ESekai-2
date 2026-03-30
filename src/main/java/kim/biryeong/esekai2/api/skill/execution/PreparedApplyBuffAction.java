package kim.biryeong.esekai2.api.skill.execution;

import kim.biryeong.esekai2.api.skill.definition.graph.SkillPredicate;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Objects;

/**
 * Prepared buff action that applies one registry-backed mob effect to living targets.
 */
public record PreparedApplyBuffAction(
        Identifier effectId,
        int durationTicks,
        int amplifier,
        boolean ambient,
        boolean showParticles,
        boolean showIcon,
        List<SkillPredicate> enPreds
) implements PreparedSkillAction {
    public PreparedApplyBuffAction {
        Objects.requireNonNull(effectId, "effectId");
        Objects.requireNonNull(enPreds, "enPreds");
        enPreds = List.copyOf(enPreds);
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
            boolean showIcon
    ) {
        this(effectId, durationTicks, amplifier, ambient, showParticles, showIcon, List.of());
    }

    @Override
    public String actionType() {
        return "apply_buff";
    }
}
