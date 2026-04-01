package kim.biryeong.esekai2.api.skill.execution;

import kim.biryeong.esekai2.api.skill.definition.graph.SkillPredicateSubject;

import java.util.Objects;
import java.util.Optional;

/**
 * Resolves named player-resource snapshots for skill value and predicate evaluation.
 */
@FunctionalInterface
public interface SkillResourceLookup {
    /**
     * Shared lookup that never resolves any resource subjects.
     */
    SkillResourceLookup EMPTY = (resource, subject) -> Optional.empty();

    /**
     * Resolves one named resource snapshot for the selected subject.
     *
     * @param resource named resource identifier
     * @param subject logical subject to inspect
     * @return resolved resource snapshot when available
     */
    Optional<SkillResolvedResource> resolve(String resource, SkillPredicateSubject subject);

    /**
     * Returns a lookup that never resolves any resource subjects.
     *
     * @return empty lookup
     */
    static SkillResourceLookup empty() {
        return EMPTY;
    }

    /**
     * Wraps a lookup with null-safety around both inputs.
     *
     * @return null-safe lookup view
     */
    default SkillResourceLookup nullSafe() {
        return (resource, subject) -> {
            Objects.requireNonNull(resource, "resource");
            Objects.requireNonNull(subject, "subject");
            return resolve(resource, subject);
        };
    }
}
