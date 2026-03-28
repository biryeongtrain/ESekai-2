package kim.biryeong.esekai2.api.skill.execution;

import java.util.Objects;

/**
 * Prepared summon-at-block action placeholder.
 */
public record PreparedSummonBlockAction(
        String componentId,
        String blockId,
        int lifeTicks
) implements PreparedSkillAction {
    public PreparedSummonBlockAction {
        Objects.requireNonNull(componentId, "componentId");
        Objects.requireNonNull(blockId, "blockId");
        if (lifeTicks < 0) {
            throw new IllegalArgumentException("lifeTicks must be >= 0");
        }
    }

    @Override
    public String actionType() {
        return "summon_block";
    }
}
