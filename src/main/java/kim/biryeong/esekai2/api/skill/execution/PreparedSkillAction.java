package kim.biryeong.esekai2.api.skill.execution;

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
}

