package kim.biryeong.esekai2.api.skill.execution;

import kim.biryeong.esekai2.api.item.affix.AffixDefinition;
import kim.biryeong.esekai2.api.item.affix.ItemAffixes;
import kim.biryeong.esekai2.api.item.socket.SocketedSkills;
import kim.biryeong.esekai2.api.player.skill.PlayerActiveSkills;
import kim.biryeong.esekai2.api.player.skill.SelectedActiveSkillRef;
import kim.biryeong.esekai2.api.skill.definition.SkillDefinition;
import kim.biryeong.esekai2.api.skill.support.SkillSupportDefinition;
import kim.biryeong.esekai2.api.stat.holder.StatHolder;
import kim.biryeong.esekai2.impl.item.affix.AffixRegistryAccess;
import kim.biryeong.esekai2.impl.item.affix.ItemLocalAffixStatOverlay;
import kim.biryeong.esekai2.impl.runtime.ServerRuntimeAccess;
import kim.biryeong.esekai2.impl.skill.execution.PlayerSelectedSkillResolver;
import kim.biryeong.esekai2.impl.skill.execution.ServerSkillExecutionHooks;
import kim.biryeong.esekai2.impl.skill.execution.SkillExecutionExecutor;
import kim.biryeong.esekai2.impl.skill.execution.SkillHitPreparationCalculator;
import net.minecraft.core.Registry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

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
     * <p>This is the low-level/raw preparation entrypoint. It never infers owner-item semantics,
     * linked support definitions, or live player context on behalf of callers. Callers preparing
     * socket-backed items or live player-selected skills should prefer the item-stack or
     * selected-skill entrypoints instead.</p>
     *
     * <p>This is the low-level/raw preparation entrypoint. It does not infer owner-item state,
     * selected skill state, or live player helpers on behalf of the caller. Local item-affix
     * semantics therefore remain inactive unless the caller has already merged them into the
     * provided {@link SkillUseContext}.</p>
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
     * <p>This compatibility overload resolves owner-item affix metadata from the live server when
     * available. Callers that need deterministic local-affix preparation without a live server
     * should use the explicit affix-registry overload. When no live server is available, this
     * overload only accepts owner items without any persisted rolled affix state and otherwise
     * throws {@link IllegalArgumentException} instead of silently dropping item-affix semantics.</p>
     *
     * @param stack item stack holding the active skill and linked supports
     * @param skillRegistry registry used to resolve the active skill
     * @param supportRegistry registry used to resolve linked supports
     * @param context runtime context used for stat resolution
     * @return prepared skill use resolved from the item state
     * @throws IllegalArgumentException when no live server is available and the owner item carries any persisted rolled affix state
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

        return prepareUse(
                stack,
                skillRegistry,
                supportRegistry,
                ServerRuntimeAccess.currentServer().map(AffixRegistryAccess::affixRegistry),
                context
        );
    }

    /**
     * Prepare one runtime execution snapshot from a socketed item stack using an explicit affix registry.
     *
     * <p>This is the recommended owner-item preparation entrypoint when callers want deterministic
     * LOCAL affix overlay semantics without relying on live server singleton state.</p>
     *
     * @param stack item stack holding the active skill and linked supports
     * @param skillRegistry registry used to resolve the active skill
     * @param supportRegistry registry used to resolve linked supports
     * @param affixRegistry registry used to resolve owner-item LOCAL affixes
     * @param context runtime context used for stat resolution
     * @return prepared skill use resolved from the item state
     */
    public static PreparedSkillUse prepareUse(
            ItemStack stack,
            Registry<SkillDefinition> skillRegistry,
            Registry<SkillSupportDefinition> supportRegistry,
            Registry<AffixDefinition> affixRegistry,
            SkillUseContext context
    ) {
        Objects.requireNonNull(affixRegistry, "affixRegistry");
        return prepareUse(stack, skillRegistry, supportRegistry, Optional.of(affixRegistry), context);
    }

    /**
     * Prepares the player's currently selected active skill from equipped socket state.
     *
     * <p>This is the recommended live-player entrypoint for selected-skill preparation. Missing
     * selection or resolution failures are reported through the result object instead of exceptions.</p>
     *
     * <p>This is the recommended live-player entrypoint when callers want the server-tracked
     * selected skill, equipped owner item, and linked support state to be resolved together under
     * failure-safe result semantics.</p>
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
        MinecraftServer server = Objects.requireNonNull(player.level().getServer(), "player is not attached to a running server");
        SkillUseContext selectedContext = context.withAttackerStats(ownerItemAttackerStats(
                        context,
                        resolved.stack(),
                        AffixRegistryAccess.affixRegistry(server)
                ))
                .withLinkedSupports(resolved.linkedSupports());
        PreparedSkillUse preparedUse = prepareUse(resolved.activeSkill(), selectedContext);
        List<String> warnings = new ArrayList<>(resolution.warnings());
        warnings.addAll(preparedUse.warnings());
        return new SelectedSkillUseResult(true, List.copyOf(warnings), selection, Optional.of(preparedUse));
    }

    static PreparedSkillUse prepareUse(
            ItemStack stack,
            Registry<SkillDefinition> skillRegistry,
            Registry<SkillSupportDefinition> supportRegistry,
            Optional<Registry<AffixDefinition>> affixRegistry,
            SkillUseContext context
    ) {
        Objects.requireNonNull(stack, "stack");
        Objects.requireNonNull(skillRegistry, "skillRegistry");
        Objects.requireNonNull(supportRegistry, "supportRegistry");
        Objects.requireNonNull(affixRegistry, "affixRegistry");
        Objects.requireNonNull(context, "context");

        var resolved = SocketedSkills.resolveDefinitions(stack, skillRegistry, supportRegistry);
        SkillDefinition activeSkill = resolved.activeSkill()
                .orElseThrow(() -> new IllegalArgumentException(String.join("; ", resolved.warnings())));
        SkillUseContext mergedContext = context.withAttackerStats(ownerItemAttackerStats(context, stack, affixRegistry))
                .withLinkedSupports(resolved.linkedSupports());
        return prepareUse(activeSkill, mergedContext);
    }

    private static StatHolder ownerItemAttackerStats(
            SkillUseContext context,
            ItemStack ownerStack,
            Optional<Registry<AffixDefinition>> affixRegistry
    ) {
        Objects.requireNonNull(affixRegistry, "affixRegistry");
        if (affixRegistry.isPresent()) {
            return ownerItemAttackerStats(context, ownerStack, affixRegistry.orElseThrow());
        }

        if (ItemAffixes.getRolledAffixes(ownerStack).isEmpty()) {
            return context.attackerStats();
        }

        throw new IllegalArgumentException(
                "prepareUse(ItemStack, ...) requires a live server or explicit affix registry when the owner item has rolled affixes"
        );
    }

    private static StatHolder ownerItemAttackerStats(
            SkillUseContext context,
            ItemStack ownerStack,
            Registry<AffixDefinition> affixRegistry
    ) {
        return ItemLocalAffixStatOverlay.apply(context.attackerStats(), ownerStack, affixRegistry);
    }

    /**
     * Executes the player's currently selected active skill from equipped socket state.
     *
     * <p>This is the recommended live-player cast entrypoint for selected active skills. Selection
     * and preparation failures are reported through the result object instead of exceptions.</p>
     *
     * <p>This is the recommended live-player cast entrypoint when callers want selected-skill
     * resolution and world-side execution under failure-safe result semantics.</p>
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
     * <p>This is the recommended live-player cast entrypoint when callers need custom side-effect
     * hooks while preserving the same failure-safe selected-skill contract.</p>
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
