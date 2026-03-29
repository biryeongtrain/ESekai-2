package kim.biryeong.esekai2.api.skill.execution;

import kim.biryeong.esekai2.api.skill.definition.graph.SkillPredicate;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Objects;

/**
 * Prepared Sandstorm particle action.
 */
public record PreparedSandstormParticleAction(
        Identifier particleId,
        String anchor,
        double offsetX,
        double offsetY,
        double offsetZ,
        List<SkillPredicate> enPreds
) implements PreparedSkillAction {
    public PreparedSandstormParticleAction {
        Objects.requireNonNull(particleId, "particleId");
        Objects.requireNonNull(anchor, "anchor");
        Objects.requireNonNull(enPreds, "enPreds");
        enPreds = List.copyOf(enPreds);
    }

    public PreparedSandstormParticleAction(Identifier particleId, String anchor, double offsetX, double offsetY, double offsetZ) {
        this(particleId, anchor, offsetX, offsetY, offsetZ, List.of());
    }

    @Override
    public String actionType() {
        return "sandstorm_particle";
    }
}
