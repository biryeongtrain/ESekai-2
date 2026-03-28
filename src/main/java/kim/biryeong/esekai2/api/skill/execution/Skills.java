package kim.biryeong.esekai2.api.skill.execution;

import kim.biryeong.esekai2.api.skill.definition.SkillDefinition;
import kim.biryeong.esekai2.impl.skill.execution.ServerSkillExecutionHooks;
import kim.biryeong.esekai2.impl.skill.execution.SkillExecutionExecutor;
import kim.biryeong.esekai2.impl.skill.execution.SkillHitPreparationCalculator;

/**
 * Entry points for skill runtime preparation.
 */
public final class Skills {
    private Skills() {
    }

    /**
     * Prepare one runtime execution snapshot for the given use context.
     *
     * @param skill   skill definition selected by identifier
     * @param context runtime context used for stat resolution
     * @return prepared skill use that can be executed by callers and tests
     */
    public static PreparedSkillUse prepareUse(SkillDefinition skill, SkillUseContext context) {
        return SkillHitPreparationCalculator.prepareUse(skill, context);
    }

    /**
     * Execute all on-cast routes using the default server hooks.
     *
     * @param context runtime execution context
     * @return execution result summary
     */
    public static SkillExecutionResult executeOnCast(SkillExecutionContext context) {
        return SkillExecutionExecutor.executeOnCast(context, ServerSkillExecutionHooks.INSTANCE);
    }

    /**
     * Execute all on-cast routes using a caller-provided hook implementation.
     *
     * @param context runtime execution context
     * @param hooks world side-effect hooks
     * @return execution result summary
     */
    public static SkillExecutionResult executeOnCast(SkillExecutionContext context, SkillExecutionHooks hooks) {
        return SkillExecutionExecutor.executeOnCast(context, hooks);
    }

    /**
     * Execute all on-hit routes for the given component using the default server hooks.
     *
     * @param context runtime execution context
     * @param componentId entity component identifier
     * @return execution result summary
     */
    public static SkillExecutionResult executeOnHit(SkillExecutionContext context, String componentId) {
        return SkillExecutionExecutor.executeOnHit(context, componentId, ServerSkillExecutionHooks.INSTANCE);
    }

    /**
     * Execute all on-hit routes for the given component using caller-provided hooks.
     *
     * @param context runtime execution context
     * @param componentId entity component identifier
     * @param hooks world side-effect hooks
     * @return execution result summary
     */
    public static SkillExecutionResult executeOnHit(SkillExecutionContext context, String componentId, SkillExecutionHooks hooks) {
        return SkillExecutionExecutor.executeOnHit(context, componentId, hooks);
    }

    /**
     * Execute all on-expire routes for the given component using the default server hooks.
     *
     * @param context runtime execution context
     * @param componentId entity component identifier
     * @return execution result summary
     */
    public static SkillExecutionResult executeOnEntityExpire(SkillExecutionContext context, String componentId) {
        return SkillExecutionExecutor.executeOnEntityExpire(context, componentId, ServerSkillExecutionHooks.INSTANCE);
    }

    /**
     * Execute all on-expire routes for the given component using caller-provided hooks.
     *
     * @param context runtime execution context
     * @param componentId entity component identifier
     * @param hooks world side-effect hooks
     * @return execution result summary
     */
    public static SkillExecutionResult executeOnEntityExpire(SkillExecutionContext context, String componentId, SkillExecutionHooks hooks) {
        return SkillExecutionExecutor.executeOnEntityExpire(context, componentId, hooks);
    }

    /**
     * Execute all tick-gated routes for the given component using the default server hooks.
     *
     * @param context runtime execution context
     * @param componentId entity component identifier
     * @param tick current component tick
     * @return execution result summary
     */
    public static SkillExecutionResult executeTick(SkillExecutionContext context, String componentId, int tick) {
        return SkillExecutionExecutor.executeTick(context, componentId, tick, ServerSkillExecutionHooks.INSTANCE);
    }

    /**
     * Execute all tick-gated routes for the given component using caller-provided hooks.
     *
     * @param context runtime execution context
     * @param componentId entity component identifier
     * @param tick current component tick
     * @param hooks world side-effect hooks
     * @return execution result summary
     */
    public static SkillExecutionResult executeTick(SkillExecutionContext context, String componentId, int tick, SkillExecutionHooks hooks) {
        return SkillExecutionExecutor.executeTick(context, componentId, tick, hooks);
    }
}
