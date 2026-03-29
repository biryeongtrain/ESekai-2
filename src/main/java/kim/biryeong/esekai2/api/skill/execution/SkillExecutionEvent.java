package kim.biryeong.esekai2.api.skill.execution;

/**
 * Runtime events supported by the current AoE-inspired action graph implementation.
 */
public enum SkillExecutionEvent {
    ON_CAST,
    ON_SPELL_CAST,
    ON_HIT,
    ON_ENTITY_EXPIRE,
    ON_TICK_CONDITION
}
