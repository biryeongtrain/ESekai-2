package kim.biryeong.esekai2.api.skill.execution;

import kim.biryeong.esekai2.api.item.socket.SocketedSkills;
import kim.biryeong.esekai2.api.player.skill.PlayerActiveSkills;
import kim.biryeong.esekai2.api.player.skill.SelectedActiveSkillRef;
import kim.biryeong.esekai2.api.skill.definition.SkillDefinition;
import kim.biryeong.esekai2.api.skill.support.SkillSupportDefinition;
import kim.biryeong.esekai2.impl.skill.execution.PlayerSelectedSkillResolver;
import kim.biryeong.esekai2.impl.skill.execution.ServerSkillExecutionHooks;
import kim.biryeong.esekai2.impl.skill.execution.SkillExecutionExecutor;
import kim.biryeong.esekai2.impl.skill.execution.SkillHitPreparationCalculator;
import net.minecraft.core.Registry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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
     * Prepare one runtime execution snapshot from a socketed item stack.
     *
     * @param stack item stack holding the active skill and linked supports
     * @param skillRegistry registry used to resolve the active skill
     * @param supportRegistry registry used to resolve linked supports
     * @param context runtime context used for stat resolution
     * @return prepared skill use resolved from the item state
     */
    public static PreparedSkillUse prepareUse(
            ItemStack stack,
            Registry<SkillDefinition> skillRegistry,
            Registry<SkillSupportDefinition> supportRegistry,
            SkillUseContext context
    ) {
        Objects.requireNonNull(stack, "stack");
        Objects.requireNonNull(skillRegistry, "skillRegistry");
        Objects.requireNonNull(supportRegistry, "supportRegistry");
        Objects.requireNonNull(context, "context");

        var resolved = SocketedSkills.resolveDefinitions(stack, skillRegistry, supportRegistry);
        SkillDefinition activeSkill = resolved.activeSkill()
                .orElseThrow(() -> new IllegalArgumentException(String.join("; ", resolved.warnings())));
        SkillUseContext mergedContext = context.withLinkedSupports(resolved.linkedSupports());
        return prepareUse(activeSkill, mergedContext);
    }

    /**
     * Prepares the player's currently selected active skill from equipped socket state.
     *
     * @param player player whose selected active skill should be prepared
     * @param context runtime context used for stat resolution
     * @return failure-safe preparation result
     */
    public static SelectedSkillUseResult prepareSelectedUse(ServerPlayer player, SkillUseContext context) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(context, "context");

        Optional<SelectedActiveSkillRef> selection = PlayerActiveSkills.get(player);
        if (selection.isEmpty()) {
            return new SelectedSkillUseResult(false, List.of("Player has no selected active skill"), Optional.empty(), Optional.empty());
        }

        PlayerSelectedSkillResolver.ResolutionResult resolution = PlayerSelectedSkillResolver.resolve(player, selection.orElseThrow());
        if (!resolution.success()) {
            return new SelectedSkillUseResult(false, resolution.warnings(), selection, Optional.empty());
        }

        PlayerSelectedSkillResolver.ResolvedSelectedSkill resolved = resolution.resolvedSkill().orElseThrow();
        PreparedSkillUse preparedUse = prepareUse(resolved.activeSkill(), context.withLinkedSupports(resolved.linkedSupports()));
        List<String> warnings = new ArrayList<>(resolution.warnings());
        warnings.addAll(preparedUse.warnings());
        return new SelectedSkillUseResult(true, List.copyOf(warnings), selection, Optional.of(preparedUse));
    }

    /**
     * Executes the player's currently selected active skill from equipped socket state.
     *
     * @param player player whose selected active skill should be cast
     * @param context runtime context used for stat resolution
     * @param target optional cast target
     * @return failure-safe cast result
     */
    public static SelectedSkillCastResult castSelectedSkill(
            ServerPlayer player,
            SkillUseContext context,
            Optional<Entity> target
    ) {
        return castSelectedSkill(player, context, target, ServerSkillExecutionHooks.INSTANCE);
    }

    /**
     * Executes the player's currently selected active skill from equipped socket state with custom hooks.
     *
     * @param player player whose selected active skill should be cast
     * @param context runtime context used for stat resolution
     * @param target optional cast target
     * @param hooks world side-effect hooks
     * @return failure-safe cast result
     */
    public static SelectedSkillCastResult castSelectedSkill(
            ServerPlayer player,
            SkillUseContext context,
            Optional<Entity> target,
            SkillExecutionHooks hooks
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(hooks, "hooks");

        SelectedSkillUseResult preparation = prepareSelectedUse(player, context);
        if (!preparation.success()) {
            return new SelectedSkillCastResult(false, preparation.warnings(), preparation.selection(), Optional.empty(), Optional.empty());
        }

        PreparedSkillUse preparedUse = preparation.preparedUse().orElseThrow();
        ServerLevel level = (ServerLevel) player.level();
        SkillExecutionResult executionResult = executeOnCast(
                SkillExecutionContext.forCast(preparedUse, level, player, target),
                hooks
        );

        List<String> warnings = new ArrayList<>(preparation.warnings());
        warnings.addAll(executionResult.warnings());
        return new SelectedSkillCastResult(
                true,
                List.copyOf(warnings),
                preparation.selection(),
                Optional.of(preparedUse),
                Optional.of(executionResult)
        );
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
