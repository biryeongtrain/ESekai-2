package kim.biryeong.esekai2.api.skill.execution;

/**
 * Tick-gated action inside an entity-component rule.
 */
public record PreparedTickAction(int intervalTicks, PreparedSkillAction action) {
}

