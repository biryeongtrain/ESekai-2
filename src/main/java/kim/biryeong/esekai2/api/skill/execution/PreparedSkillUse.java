package kim.biryeong.esekai2.api.skill.execution;

import kim.biryeong.esekai2.api.skill.definition.SkillDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Result of preparing a skill use from data-driven skill definitions.
 */
public final class PreparedSkillUse {
    private final SkillDefinition skill;
    private final SkillUseContext useContext;
    private final String resource;
    private final double resourceCost;
    private final int useTimeTicks;
    private final int cooldownTicks;
    private final List<PreparedSkillExecutionRoute> onCastRoutes;
    private final List<PreparedSkillExecutionRoute> onSpellCastRoutes;
    private final Map<String, PreparedSkillEntityComponent> components;
    private final List<String> warnings;

    public PreparedSkillUse(
            SkillDefinition skill,
            SkillUseContext useContext,
            String resource,
            double resourceCost,
            int useTimeTicks,
            int cooldownTicks,
            List<PreparedSkillExecutionRoute> onCastRoutes,
            List<PreparedSkillExecutionRoute> onSpellCastRoutes,
            Map<String, PreparedSkillEntityComponent> components,
            List<String> warnings
    ) {
        this.skill = Objects.requireNonNull(skill, "skill");
        this.useContext = Objects.requireNonNull(useContext, "useContext");
        this.resource = Objects.requireNonNull(resource, "resource");
        this.resourceCost = resourceCost;
        this.useTimeTicks = useTimeTicks;
        this.cooldownTicks = cooldownTicks;
        this.onCastRoutes = List.copyOf(onCastRoutes);
        this.onSpellCastRoutes = List.copyOf(onSpellCastRoutes);
        this.components = Map.copyOf(components);
        this.warnings = List.copyOf(warnings);
    }

    public SkillDefinition skill() {
        return skill;
    }

    public SkillUseContext useContext() {
        return useContext;
    }

    public String resource() {
        return resource;
    }

    public double resourceCost() {
        return resourceCost;
    }

    public int useTimeTicks() {
        return useTimeTicks;
    }

    public int cooldownTicks() {
        return cooldownTicks;
    }

    public List<PreparedSkillAction> onCastActions() {
        List<PreparedSkillAction> actions = new ArrayList<>();
        for (PreparedSkillExecutionRoute route : onCastRoutes) {
            actions.addAll(route.actions());
        }
        return List.copyOf(actions);
    }

    public List<PreparedSkillExecutionRoute> onCastRoutes() {
        return onCastRoutes;
    }

    public List<PreparedSkillExecutionRoute> onSpellCastRoutes() {
        return onSpellCastRoutes;
    }

    public Map<String, PreparedSkillEntityComponent> components() {
        return components;
    }

    public PreparedSkillEntityComponent component(String componentId) {
        return requireComponent(componentId);
    }

    public List<String> warnings() {
        return warnings;
    }

    public List<PreparedSkillAction> executeOnHit() {
        List<PreparedSkillAction> result = new ArrayList<>();
        for (PreparedSkillEntityComponent component : components.values()) {
            result.addAll(component.onHitActions());
        }
        return List.copyOf(result);
    }

    public List<PreparedSkillAction> executeOnHit(String componentId) {
        PreparedSkillEntityComponent component = requireComponent(componentId);
        return component.onHitActions();
    }

    public List<PreparedSkillAction> executeOnEntityExpire() {
        List<PreparedSkillAction> result = new ArrayList<>();
        for (PreparedSkillEntityComponent component : components.values()) {
            result.addAll(component.onExpireActions());
        }
        return List.copyOf(result);
    }

    public List<PreparedSkillAction> executeOnSpellCast() {
        List<PreparedSkillAction> result = new ArrayList<>();
        for (PreparedSkillExecutionRoute route : onSpellCastRoutes) {
            result.addAll(route.actions());
        }
        return List.copyOf(result);
    }

    public List<PreparedSkillAction> executeOnSpellCast(String componentId) {
        PreparedSkillEntityComponent component = requireComponent(componentId);
        return component.onSpellCastActions();
    }

    public List<PreparedSkillAction> executeOnEntityExpire(String componentId) {
        PreparedSkillEntityComponent component = requireComponent(componentId);
        return component.onExpireActions();
    }

    public List<PreparedSkillAction> executeTick(int tick) {
        if (tick < 0) {
            throw new IllegalArgumentException("tick must be >= 0");
        }

        List<PreparedSkillAction> result = new ArrayList<>();
        for (PreparedSkillEntityComponent component : components.values()) {
            for (PreparedTickAction tickAction : component.tickActions()) {
                if (tickAction.intervalTicks() > 0 && tick % tickAction.intervalTicks() == 0) {
                    result.add(tickAction.action());
                }
            }
        }

        return List.copyOf(result);
    }

    public List<PreparedSkillAction> executeTick(String componentId, int tick) {
        if (tick < 0) {
            throw new IllegalArgumentException("tick must be >= 0");
        }

        PreparedSkillEntityComponent component = requireComponent(componentId);
        List<PreparedSkillAction> result = new ArrayList<>();
        for (PreparedTickAction tickAction : component.tickActions()) {
            if (tickAction.intervalTicks() > 0 && tick % tickAction.intervalTicks() == 0) {
                result.add(tickAction.action());
            }
        }
        return List.copyOf(result);
    }

    private PreparedSkillEntityComponent requireComponent(String componentId) {
        Objects.requireNonNull(componentId, "componentId");
        PreparedSkillEntityComponent component = components.get(componentId);
        if (component == null) {
            throw new IllegalArgumentException("Unknown component: " + componentId);
        }
        return component;
    }
}
