package kim.biryeong.esekai2.api.skill.execution;

import net.minecraft.resources.Identifier;

import java.util.Objects;

/**
 * Prepared Sandstorm particle action.
 */
public record PreparedSandstormParticleAction(
        Identifier particleId,
        String anchor,
        double offsetX,
        double offsetY,
        double offsetZ
) implements PreparedSkillAction {
    public PreparedSandstormParticleAction {
        Objects.requireNonNull(particleId, "particleId");
        Objects.requireNonNull(anchor, "anchor");
    }

    @Override
    public String actionType() {
        return "sandstorm_particle";
    }
}
