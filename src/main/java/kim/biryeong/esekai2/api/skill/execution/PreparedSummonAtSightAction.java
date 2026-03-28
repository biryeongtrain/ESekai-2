package kim.biryeong.esekai2.api.skill.execution;

import java.util.Objects;

/**
 * Prepared summon-at-sight action placeholder.
 */
public record PreparedSummonAtSightAction(
        String componentId,
        String summonEntityId,
        int lifeTicks,
        boolean gravity
) implements PreparedSkillAction {
    public PreparedSummonAtSightAction {
        Objects.requireNonNull(componentId, "componentId");
        Objects.requireNonNull(summonEntityId, "summonEntityId");
        if (lifeTicks < 0) {
            throw new IllegalArgumentException("lifeTicks must be >= 0");
        }
    }

    @Override
    public String actionType() {
        return "summon_at_sight";
    }
}
