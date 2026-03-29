package kim.biryeong.esekai2.api.skill.execution;

import kim.biryeong.esekai2.api.skill.definition.graph.SkillPredicate;

import java.util.List;
import java.util.Objects;

/**
 * Prepared projectile action used by the execution graph.
 */
public record PreparedProjectileAction(
        String componentId,
        String projectileEntityId,
        int lifeTicks,
        boolean gravity,
        List<SkillPredicate> enPreds
) implements PreparedSkillAction {
    public PreparedProjectileAction {
        Objects.requireNonNull(componentId, "componentId");
        Objects.requireNonNull(projectileEntityId, "projectileEntityId");
        Objects.requireNonNull(enPreds, "enPreds");
        enPreds = List.copyOf(enPreds);
        if (lifeTicks < 0) {
            throw new IllegalArgumentException("lifeTicks must be >= 0");
        }
    }

    public PreparedProjectileAction(String componentId, String projectileEntityId, int lifeTicks, boolean gravity) {
        this(componentId, projectileEntityId, lifeTicks, gravity, List.of());
    }

    @Override
    public String actionType() {
        return "projectile";
    }
}
