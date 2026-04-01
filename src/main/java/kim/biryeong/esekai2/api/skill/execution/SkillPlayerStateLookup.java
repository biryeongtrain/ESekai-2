package kim.biryeong.esekai2.api.skill.execution;

import net.minecraft.resources.Identifier;

import java.util.Objects;

/**
 * Resolves player-bound runtime state snapshots for the active skill being prepared.
 */
public interface SkillPlayerStateLookup {
    /**
     * Shared lookup that reports no player-bound runtime state.
     */
    SkillPlayerStateLookup EMPTY = new SkillPlayerStateLookup() {
    };

    /**
     * Returns remaining cooldown ticks for the provided skill.
     *
     * @param skillId active skill identifier
     * @return remaining cooldown ticks, or {@code 0} when unavailable
     */
    default long cooldownRemainingTicks(Identifier skillId) {
        Objects.requireNonNull(skillId, "skillId");
        return 0L;
    }

    /**
     * Returns currently available charges for the provided skill.
     *
     * @param skillId active skill identifier
     * @param maxCharges configured maximum charges for the skill
     * @return currently available charges, or {@code 0} when unavailable
     */
    default int availableCharges(Identifier skillId, int maxCharges) {
        Objects.requireNonNull(skillId, "skillId");
        return 0;
    }

    /**
     * Returns currently available follow-up burst casts for the provided skill.
     *
     * @param skillId active skill identifier
     * @return remaining burst follow-up casts, or {@code 0} when unavailable
     */
    default int burstRemainingCasts(Identifier skillId) {
        Objects.requireNonNull(skillId, "skillId");
        return 0;
    }

    /**
     * Returns a lookup that reports no player-bound runtime state.
     *
     * @return empty lookup
     */
    static SkillPlayerStateLookup empty() {
        return EMPTY;
    }
}
