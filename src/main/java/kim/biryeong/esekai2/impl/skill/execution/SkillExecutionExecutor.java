package kim.biryeong.esekai2.impl.skill.execution;

import kim.biryeong.esekai2.api.skill.execution.PreparedSkillAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedSkillExecutionRoute;
import kim.biryeong.esekai2.api.skill.execution.SkillExecutionContext;
import kim.biryeong.esekai2.api.skill.execution.SkillExecutionHooks;
import kim.biryeong.esekai2.api.skill.execution.SkillExecutionResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Internal executor that turns prepared routes into world hooks.
 */
public final class SkillExecutionExecutor {
    private SkillExecutionExecutor() {
    }

    public static SkillExecutionResult executeOnCast(
            SkillExecutionContext context,
            SkillExecutionHooks hooks
    ) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(hooks, "hooks");
        SkillExecutionResult onCast = executeRoutes(context, context.preparedUse().onCastRoutes(), hooks);
        SkillExecutionResult onSpellCast = executeRoutes(context, context.preparedUse().onSpellCastRoutes(), hooks);
        return new SkillExecutionResult(
                onCast.executedActions() + onSpellCast.executedActions(),
                onCast.skippedActions() + onSpellCast.skippedActions(),
                onCast.warnings()
        );
    }

    public static SkillExecutionResult executeOnHit(
            SkillExecutionContext context,
            String componentId,
            SkillExecutionHooks hooks
    ) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(componentId, "componentId");
        Objects.requireNonNull(hooks, "hooks");
        return executeRoutes(context, context.preparedUse().component(componentId).onHitRoutes(), hooks);
    }

    public static SkillExecutionResult executeOnEntityExpire(
            SkillExecutionContext context,
            String componentId,
            SkillExecutionHooks hooks
    ) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(componentId, "componentId");
        Objects.requireNonNull(hooks, "hooks");
        return executeRoutes(context, context.preparedUse().component(componentId).onExpireRoutes(), hooks);
    }

    public static SkillExecutionResult executeTick(
            SkillExecutionContext context,
            String componentId,
            int tick,
            SkillExecutionHooks hooks
    ) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(componentId, "componentId");
        Objects.requireNonNull(hooks, "hooks");

        if (tick < 0) {
            throw new IllegalArgumentException("tick must be >= 0");
        }

        List<PreparedSkillExecutionRoute> routes = context.preparedUse().component(componentId).tickRoutes();
        int executed = 0;
        int skipped = 0;
        List<String> warnings = new ArrayList<>(context.preparedUse().warnings());

        for (PreparedSkillExecutionRoute route : routes) {
            if (route.tickIntervalTicks() > 0 && tick % route.tickIntervalTicks() != 0) {
                skipped += route.actions().size();
                continue;
            }
            if (!route.matches(context)) {
                skipped += route.actions().size();
                continue;
            }
            int executedForRoute = executeRoute(context, route, hooks);
            executed += executedForRoute;
            skipped += route.actions().size() - executedForRoute;
        }

        return new SkillExecutionResult(executed, skipped, warnings);
    }

    private static SkillExecutionResult executeRoutes(
            SkillExecutionContext context,
            List<PreparedSkillExecutionRoute> routes,
            SkillExecutionHooks hooks
    ) {
        int executed = 0;
        int skipped = 0;
        List<String> warnings = new ArrayList<>(context.preparedUse().warnings());

        for (PreparedSkillExecutionRoute route : routes) {
            if (!route.matches(context)) {
                skipped += route.actions().size();
                continue;
            }
            int executedForRoute = executeRoute(context, route, hooks);
            executed += executedForRoute;
            skipped += route.actions().size() - executedForRoute;
        }

        return new SkillExecutionResult(executed, skipped, warnings);
    }

    private static int executeRoute(
            SkillExecutionContext context,
            PreparedSkillExecutionRoute route,
            SkillExecutionHooks hooks
    ) {
        int executed = 0;
        List<net.minecraft.world.entity.Entity> targets = context.resolveTargets(route.targets());
        for (PreparedSkillAction action : route.actions()) {
            if (!action.matches(context)) {
                continue;
            }
            boolean completed = false;
            if (action instanceof kim.biryeong.esekai2.api.skill.execution.PreparedSoundAction preparedSoundAction) {
                completed = hooks.playSound(context, targets, preparedSoundAction);
            } else if (action instanceof kim.biryeong.esekai2.api.skill.execution.PreparedDamageAction preparedDamageAction) {
                completed = hooks.applyDamage(context, targets, preparedDamageAction).isPresent();
            } else if (action instanceof kim.biryeong.esekai2.api.skill.execution.PreparedProjectileAction preparedProjectileAction) {
                completed = hooks.spawnProjectile(context, targets, preparedProjectileAction).isPresent();
            } else if (action instanceof kim.biryeong.esekai2.api.skill.execution.PreparedSummonAtSightAction preparedSummonAtSightAction) {
                completed = hooks.spawnSummonAtSight(context, targets, preparedSummonAtSightAction).isPresent();
            } else if (action instanceof kim.biryeong.esekai2.api.skill.execution.PreparedSummonBlockAction preparedSummonBlockAction) {
                completed = hooks.placeBlock(context, targets, preparedSummonBlockAction);
            } else if (action instanceof kim.biryeong.esekai2.api.skill.execution.PreparedSandstormParticleAction preparedSandstormParticleAction) {
                completed = hooks.emitSandstormParticle(context, targets, preparedSandstormParticleAction);
            }
            if (completed) {
                executed++;
            }
        }
        return executed;
    }
}
