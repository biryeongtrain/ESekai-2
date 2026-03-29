package kim.biryeong.esekai2.api.skill.execution;

import kim.biryeong.esekai2.api.skill.definition.graph.SkillPredicate;

import java.util.List;
import java.util.Objects;

/**
 * Marker for a prepared executable action in a skill action graph.
 */
public interface PreparedSkillAction {
    /**
     * Human-readable action name used by test assertions and debug output.
     *
     * @return action type name
     */
    String actionType();

    /**
     * Runtime predicates attached directly to this prepared action.
     *
     * @return action-local execution predicates
     */
    default List<SkillPredicate> enPreds() {
        return List.of();
    }

    /**
     * Returns whether the action is currently allowed by its action-local predicates.
     *
     * @param context current skill execution snapshot
     * @return {@code true} when all action-local predicates pass
     */
    default boolean matches(SkillExecutionContext context) {
        Objects.requireNonNull(context, "context");
        for (SkillPredicate predicate : enPreds()) {
            if (!predicate.matches(context)) {
                return false;
            }
        }
        return true;
    }
}
