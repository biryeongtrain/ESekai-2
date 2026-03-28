package kim.biryeong.esekai2.api.skill.execution;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Runtime snapshot for a named entity-component action set.
 */
public final class PreparedSkillEntityComponent {
    private final String componentId;
    private final List<PreparedSkillExecutionRoute> onHitRoutes;
    private final List<PreparedSkillExecutionRoute> onExpireRoutes;
    private final List<PreparedSkillExecutionRoute> tickRoutes;
    private final List<PreparedSkillAction> onHitActions;
    private final List<PreparedSkillAction> onExpireActions;
    private final List<PreparedTickAction> tickActions;

    public PreparedSkillEntityComponent(
            String componentId,
            List<PreparedSkillExecutionRoute> onHitRoutes,
            List<PreparedSkillExecutionRoute> onExpireRoutes,
            List<PreparedSkillExecutionRoute> tickRoutes
    ) {
        this.componentId = Objects.requireNonNull(componentId, "componentId");
        this.onHitRoutes = List.copyOf(onHitRoutes);
        this.onExpireRoutes = List.copyOf(onExpireRoutes);
        this.tickRoutes = List.copyOf(tickRoutes);
        this.onHitActions = flattenActions(this.onHitRoutes);
        this.onExpireActions = flattenActions(this.onExpireRoutes);
        this.tickActions = flattenTickActions(this.tickRoutes);
    }

    public String componentId() {
        return componentId;
    }

    public List<PreparedSkillExecutionRoute> onHitRoutes() {
        return onHitRoutes;
    }

    public List<PreparedSkillExecutionRoute> onExpireRoutes() {
        return onExpireRoutes;
    }

    public List<PreparedSkillExecutionRoute> tickRoutes() {
        return tickRoutes;
    }

    public List<PreparedSkillAction> onHitActions() {
        return onHitActions;
    }

    public List<PreparedSkillAction> onExpireActions() {
        return onExpireActions;
    }

    public List<PreparedTickAction> tickActions() {
        return tickActions;
    }

    private static List<PreparedSkillAction> flattenActions(List<PreparedSkillExecutionRoute> routes) {
        List<PreparedSkillAction> actions = new ArrayList<>();
        for (PreparedSkillExecutionRoute route : routes) {
            actions.addAll(route.actions());
        }
        return List.copyOf(actions);
    }

    private static List<PreparedTickAction> flattenTickActions(List<PreparedSkillExecutionRoute> routes) {
        List<PreparedTickAction> actions = new ArrayList<>();
        for (PreparedSkillExecutionRoute route : routes) {
            for (PreparedSkillAction action : route.actions()) {
                actions.add(new PreparedTickAction(route.tickIntervalTicks(), action));
            }
        }
        return List.copyOf(actions);
    }
}
