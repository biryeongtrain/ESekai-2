package kim.biryeong.esekai2.api.skill.execution;

import kim.biryeong.esekai2.api.skill.definition.graph.SkillPredicate;

import java.util.List;
import java.util.Objects;

/**
 * Prepared summon-at-block action placeholder.
 */
public record PreparedSummonBlockAction(
        String componentId,
        String blockId,
        int lifeTicks,
        List<SkillPredicate> enPreds
) implements PreparedSkillAction {
    public PreparedSummonBlockAction {
        Objects.requireNonNull(componentId, "componentId");
        Objects.requireNonNull(blockId, "blockId");
        Objects.requireNonNull(enPreds, "enPreds");
        enPreds = List.copyOf(enPreds);
        if (lifeTicks < 0) {
            throw new IllegalArgumentException("lifeTicks must be >= 0");
        }
    }

    public PreparedSummonBlockAction(String componentId, String blockId, int lifeTicks) {
        this(componentId, blockId, lifeTicks, List.of());
    }

    @Override
    public String actionType() {
        return "summon_block";
    }
}
