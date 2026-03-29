package kim.biryeong.esekai2.api.skill.execution;

import kim.biryeong.esekai2.api.skill.definition.graph.SkillPredicate;

import java.util.List;
import java.util.Objects;

/**
 * Prepared summon-at-sight action placeholder.
 */
public record PreparedSummonAtSightAction(
        String componentId,
        String summonEntityId,
        int lifeTicks,
        boolean gravity,
        List<SkillPredicate> enPreds
) implements PreparedSkillAction {
    public PreparedSummonAtSightAction {
        Objects.requireNonNull(componentId, "componentId");
        Objects.requireNonNull(summonEntityId, "summonEntityId");
        Objects.requireNonNull(enPreds, "enPreds");
        enPreds = List.copyOf(enPreds);
        if (lifeTicks < 0) {
            throw new IllegalArgumentException("lifeTicks must be >= 0");
        }
    }

    public PreparedSummonAtSightAction(String componentId, String summonEntityId, int lifeTicks, boolean gravity) {
        this(componentId, summonEntityId, lifeTicks, gravity, List.of());
    }

    @Override
    public String actionType() {
        return "summon_at_sight";
    }
}
