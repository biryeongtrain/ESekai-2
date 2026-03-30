package kim.biryeong.esekai2.api.skill.execution;

import kim.biryeong.esekai2.api.damage.calculation.DamageOverTimeCalculation;
import kim.biryeong.esekai2.api.skill.definition.graph.SkillPredicate;

import java.util.List;
import java.util.Objects;

/**
 * Prepared generic skill-owned damage-over-time action.
 */
public record PreparedApplyDotAction(
        String dotId,
        DamageOverTimeCalculation damageCalculation,
        int durationTicks,
        int tickIntervalTicks,
        List<SkillPredicate> enPreds
) implements PreparedSkillAction {
    public PreparedApplyDotAction {
        Objects.requireNonNull(dotId, "dotId");
        Objects.requireNonNull(damageCalculation, "damageCalculation");
        Objects.requireNonNull(enPreds, "enPreds");
        enPreds = List.copyOf(enPreds);
        if (dotId.isBlank()) {
            throw new IllegalArgumentException("dotId must not be blank");
        }
        if (durationTicks < 0) {
            throw new IllegalArgumentException("durationTicks must be >= 0");
        }
        if (tickIntervalTicks <= 0) {
            throw new IllegalArgumentException("tickIntervalTicks must be > 0");
        }
    }

    public PreparedApplyDotAction(
            String dotId,
            DamageOverTimeCalculation damageCalculation,
            int durationTicks,
            int tickIntervalTicks
    ) {
        this(dotId, damageCalculation, durationTicks, tickIntervalTicks, List.of());
    }

    @Override
    public String actionType() {
        return "apply_dot";
    }
}
