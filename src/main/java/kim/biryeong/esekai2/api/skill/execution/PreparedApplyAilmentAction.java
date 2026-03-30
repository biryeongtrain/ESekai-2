package kim.biryeong.esekai2.api.skill.execution;

import kim.biryeong.esekai2.api.ailment.AilmentType;
import kim.biryeong.esekai2.api.skill.definition.graph.SkillPredicate;

import java.util.List;
import java.util.Objects;

/**
 * Prepared ailment application action backed by a prior successful damage result on the same route.
 */
public record PreparedApplyAilmentAction(
        AilmentType ailmentType,
        double chancePercent,
        int durationTicks,
        double potencyMultiplierPercent,
        List<SkillPredicate> enPreds
) implements PreparedSkillAction {
    public PreparedApplyAilmentAction {
        Objects.requireNonNull(ailmentType, "ailmentType");
        Objects.requireNonNull(enPreds, "enPreds");
        enPreds = List.copyOf(enPreds);
        if (!Double.isFinite(chancePercent) || chancePercent < 0.0 || chancePercent > 100.0) {
            throw new IllegalArgumentException("chancePercent must be finite and in [0, 100]");
        }
        if (durationTicks < 0) {
            throw new IllegalArgumentException("durationTicks must be >= 0");
        }
        if (!Double.isFinite(potencyMultiplierPercent) || potencyMultiplierPercent < 0.0) {
            throw new IllegalArgumentException("potencyMultiplierPercent must be finite and >= 0");
        }
    }

    @Override
    public String actionType() {
        return "apply_ailment";
    }
}
