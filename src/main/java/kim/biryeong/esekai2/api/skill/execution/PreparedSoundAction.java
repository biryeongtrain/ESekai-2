package kim.biryeong.esekai2.api.skill.execution;

import net.minecraft.resources.Identifier;

import java.util.Objects;

/**
 * Prepared sound action.
 */
public record PreparedSoundAction(Identifier soundId, float volume, float pitch) implements PreparedSkillAction {
    public PreparedSoundAction {
        Objects.requireNonNull(soundId, "soundId");
    }

    @Override
    public String actionType() {
        return "sound";
    }
}

