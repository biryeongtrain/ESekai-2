package kim.biryeong.esekai2.api.skill.execution;

import java.util.Objects;

/**
 * Prepared projectile action used by the execution graph.
 */
public record PreparedProjectileAction(
        String componentId,
        String projectileEntityId,
        int lifeTicks,
        boolean gravity
) implements PreparedSkillAction {
    public PreparedProjectileAction {
        Objects.requireNonNull(componentId, "componentId");
        Objects.requireNonNull(projectileEntityId, "projectileEntityId");
        if (lifeTicks < 0) {
            throw new IllegalArgumentException("lifeTicks must be >= 0");
        }
    }

    @Override
    public String actionType() {
        return "projectile";
    }
}
