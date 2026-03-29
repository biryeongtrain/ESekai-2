package kim.biryeong.esekai2.api.skill.execution;

import kim.biryeong.esekai2.api.skill.definition.graph.SkillPredicate;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Objects;

/**
 * Prepared sound action.
 */
public record PreparedSoundAction(
        Identifier soundId,
        float volume,
        float pitch,
        List<SkillPredicate> enPreds
) implements PreparedSkillAction {
    public PreparedSoundAction {
        Objects.requireNonNull(soundId, "soundId");
        Objects.requireNonNull(enPreds, "enPreds");
        enPreds = List.copyOf(enPreds);
    }

    public PreparedSoundAction(Identifier soundId, float volume, float pitch) {
        this(soundId, volume, pitch, List.of());
    }

    @Override
    public String actionType() {
        return "sound";
    }
}
