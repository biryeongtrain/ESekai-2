package kim.biryeong.esekai2.api.skill.execution;

import java.util.Objects;
import java.util.Optional;

/**
 * Resolves SELF-scoped player runtime state for the skill currently being prepared or executed.
 */
@FunctionalInterface
public interface SkillRuntimeStateLookup {
    /**
     * Shared lookup that never resolves any runtime skill state.
     */
    SkillRuntimeStateLookup EMPTY = skillId -> Optional.empty();

    /**
     * Resolves one runtime-state snapshot for the provided skill identifier.
     *
     * @param skillId skill identifier whose runtime state should be inspected
     * @return resolved runtime-state snapshot when available
     */
    Optional<SkillResolvedRuntimeState> resolve(String skillId);

    /**
     * Returns a lookup that never resolves any runtime state.
     *
     * @return empty lookup
     */
    static SkillRuntimeStateLookup empty() {
        return EMPTY;
    }

    /**
     * Wraps a lookup with null-safety around the skill identifier.
     *
     * @return null-safe lookup view
     */
    default SkillRuntimeStateLookup nullSafe() {
        return skillId -> {
            Objects.requireNonNull(skillId, "skillId");
            return resolve(skillId);
        };
    }
}
