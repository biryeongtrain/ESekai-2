package kim.biryeong.esekai2.api.skill.execution;

import kim.biryeong.esekai2.api.skill.definition.graph.SkillTargetSelector;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Prepared execution route for a single rule branch.
 *
 * <p>The route keeps the original target selectors so runtime execution can resolve targets
 * against the current world context without reparsing the datapack model.</p>
 *
 * @param event runtime event that triggers this route
 * @param targets selectors attached to the rule
 * @param actions prepared actions executed when the route fires
 * @param tickIntervalTicks tick interval for {@link SkillExecutionEvent#ON_TICK_CONDITION}
 */
public record PreparedSkillExecutionRoute(
        SkillExecutionEvent event,
        Set<SkillTargetSelector> targets,
        List<PreparedSkillAction> actions,
        int tickIntervalTicks
) {
    public PreparedSkillExecutionRoute {
        Objects.requireNonNull(event, "event");
        Objects.requireNonNull(targets, "targets");
        Objects.requireNonNull(actions, "actions");

        targets = Set.copyOf(targets);
        actions = List.copyOf(actions);

        if (tickIntervalTicks < 0) {
            throw new IllegalArgumentException("tickIntervalTicks must be >= 0");
        }
    }
}
