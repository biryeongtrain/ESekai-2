package kim.biryeong.esekai2.impl.skill.execution;

import kim.biryeong.esekai2.api.damage.calculation.DamageCalculationResult;
import kim.biryeong.esekai2.api.ailment.AilmentType;
import kim.biryeong.esekai2.api.player.skill.PlayerSkillBursts;
import kim.biryeong.esekai2.api.player.resource.PlayerResources;
import kim.biryeong.esekai2.api.player.skill.PlayerSkillCharges;
import kim.biryeong.esekai2.api.player.skill.PlayerSkillCooldowns;
import kim.biryeong.esekai2.api.stat.combat.CombatStats;
import kim.biryeong.esekai2.api.skill.execution.PreparedSkillAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedSkillExecutionRoute;
import kim.biryeong.esekai2.api.skill.execution.SkillExecutionContext;
import kim.biryeong.esekai2.api.skill.execution.SkillExecutionHooks;
import kim.biryeong.esekai2.api.skill.execution.SkillExecutionResult;
import kim.biryeong.esekai2.impl.ailment.AilmentRuntime;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Internal executor that turns prepared routes into world hooks.
 */
public final class SkillExecutionExecutor {
    public static final String BLOCKED_WARNING_PREFIX = "Skill execution blocked: ";
    private static final long BURST_WINDOW_TICKS = 10L;
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

    private SkillExecutionExecutor() {
    }

    public static SkillExecutionResult executeOnCast(
            SkillExecutionContext context,
            SkillExecutionHooks hooks
    ) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(hooks, "hooks");
        SkillExecutionResult blocked = blockedCastResult(context);
        if (blocked != null) {
            return blocked;
        }
        SkillExecutionResult onCast = executeRoutes(context, context.preparedUse().onCastRoutes(), hooks);
        SkillExecutionResult onSpellCast = executeRoutes(context, context.preparedUse().onSpellCastRoutes(), hooks);
        SkillExecutionResult result = new SkillExecutionResult(
                onCast.executedActions() + onSpellCast.executedActions(),
                onCast.skippedActions() + onSpellCast.skippedActions(),
                onCast.warnings()
        );
        applySuccessfulCastState(context, result);
        return result;
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
        List<Entity> targets = context.resolveTargets(route.targets());
        Map<UUID, DamageCalculationResult> latestDamageResults = new LinkedHashMap<>();
        for (PreparedSkillAction action : route.actions()) {
            if (!action.matches(context)) {
                continue;
            }
            boolean completed = false;
            if (action instanceof kim.biryeong.esekai2.api.skill.execution.PreparedSoundAction preparedSoundAction) {
                completed = hooks.playSound(context, targets, preparedSoundAction);
            } else if (action instanceof kim.biryeong.esekai2.api.skill.execution.PreparedDamageAction preparedDamageAction) {
                completed = applyDamage(context, targets, hooks, preparedDamageAction, latestDamageResults);
            } else if (action instanceof kim.biryeong.esekai2.api.skill.execution.PreparedApplyBuffAction preparedApplyBuffAction) {
                completed = hooks.applyBuff(context, targets, preparedApplyBuffAction);
            } else if (action instanceof kim.biryeong.esekai2.api.skill.execution.PreparedRemoveEffectAction preparedRemoveEffectAction) {
                completed = hooks.removeEffect(context, targets, preparedRemoveEffectAction);
            } else if (action instanceof kim.biryeong.esekai2.api.skill.execution.PreparedApplyAilmentAction preparedApplyAilmentAction) {
                completed = hooks.applyAilment(context, targets, preparedApplyAilmentAction, Map.copyOf(latestDamageResults));
            } else if (action instanceof kim.biryeong.esekai2.api.skill.execution.PreparedApplyDotAction preparedApplyDotAction) {
                completed = hooks.applyDamageOverTime(context, targets, preparedApplyDotAction);
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

    private static boolean applyDamage(
            SkillExecutionContext context,
            List<Entity> targets,
            SkillExecutionHooks hooks,
            kim.biryeong.esekai2.api.skill.execution.PreparedDamageAction action,
            Map<UUID, DamageCalculationResult> latestDamageResults
    ) {
        boolean completed = false;
        for (Entity target : targets) {
            if (!(target instanceof LivingEntity livingTarget)) {
                continue;
            }

            DamageCalculationResult result = hooks.applyDamage(context, List.of(livingTarget), action).orElse(null);
            if (result == null) {
                continue;
            }

            completed = true;
            if (result.finalDamage().totalAmount() > 0.0) {
                latestDamageResults.put(livingTarget.getUUID(), result);
            }
        }
        return completed;
    }

    private static SkillExecutionResult blockedCastResult(SkillExecutionContext context) {
        List<String> warnings = new ArrayList<>(context.preparedUse().warnings());
        boolean blocked = false;

        String disabledDimensionWarning = disabledDimensionWarning(context);
        if (disabledDimensionWarning != null) {
            warnings.add(disabledDimensionWarning);
            blocked = true;
        }

        if (context.source() instanceof LivingEntity livingSource) {
            AilmentType blockingAilment = AilmentRuntime.blockingSkillExecutionAilment(livingSource).orElse(null);
            if (blockingAilment != null) {
                warnings.add(BLOCKED_WARNING_PREFIX + "ailment " + blockingAilment.serializedName());
                blocked = true;
            }
        }

        String cooldownWarning = cooldownWarning(context);
        if (cooldownWarning != null) {
            warnings.add(cooldownWarning);
            blocked = true;
        }

        String burstWarning = burstWarning(context);
        if (burstWarning != null) {
            warnings.add(burstWarning);
            blocked = true;
        }

        String chargeWarning = chargeWarning(context);
        if (chargeWarning != null) {
            warnings.add(chargeWarning);
            blocked = true;
        }

        String manaWarning = manaWarning(context);
        if (manaWarning != null) {
            warnings.add(manaWarning);
            blocked = true;
        }

        if (!(context.source() instanceof LivingEntity)) {
            if (!blocked) {
                return null;
            }
            return new SkillExecutionResult(
                    0,
                    countActions(context.preparedUse().onCastRoutes()) + countActions(context.preparedUse().onSpellCastRoutes()),
                    warnings
            );
        }
        if (!blocked) {
            return null;
        }
        return new SkillExecutionResult(
                0,
                countActions(context.preparedUse().onCastRoutes()) + countActions(context.preparedUse().onSpellCastRoutes()),
                warnings
        );
    }

    private static void applySuccessfulCastState(SkillExecutionContext context, SkillExecutionResult result) {
        if (result.executedActions() <= 0) {
            return;
        }

        ServerPlayer player = resolvePlayer(context);
        if (player == null) {
            return;
        }

        Identifier skillId = resolveSkillId(context);
        if (skillId == null) {
            return;
        }

        long gameTime = context.level().getGameTime();
        int maxCharges = resolvedMaxCharges(context);
        if (maxCharges > 0) {
            long chargeRegenTicks = resolvedChargeRegenTicks(context);
            if (chargeRegenTicks <= 0L) {
                return;
            }
            if (PlayerSkillCharges.consume(player, skillId, maxCharges, gameTime, gameTime + chargeRegenTicks).isEmpty()) {
                return;
            }
        }

        int cooldownTicks = context.preparedUse().cooldownTicks();
        if (cooldownTicks > 0) {
            long readyGameTime = gameTime + cooldownTicks;
            PlayerSkillCooldowns.start(player, skillId, readyGameTime);
        }

        double maxMana = resolvedMaxMana(context);
        if (maxMana <= 0.0) {
            PlayerSkillBursts.recordSuccessfulCast(
                    player,
                    skillId,
                    resolvedTimesToCast(context),
                    gameTime,
                    gameTime + BURST_WINDOW_TICKS
            );
            return;
        }

        double resourceCost = resolvedResourceCost(context);
        if (resourceCost > 0.0) {
            PlayerResources.spendMana(player, resourceCost, maxMana);
        }

        PlayerSkillBursts.recordSuccessfulCast(
                player,
                skillId,
                resolvedTimesToCast(context),
                gameTime,
                gameTime + BURST_WINDOW_TICKS
        );
    }

    private static String disabledDimensionWarning(SkillExecutionContext context) {
        Identifier currentDimension = resolveDimensionId(context);
        if (currentDimension == null) {
            return null;
        }
        if (!context.preparedUse().skill().disabledDims().contains(currentDimension)) {
            return null;
        }

        return BLOCKED_WARNING_PREFIX + "dimension " + currentDimension;
    }

    private static String cooldownWarning(SkillExecutionContext context) {
        ServerPlayer player = resolvePlayer(context);
        if (player == null) {
            return null;
        }

        Identifier skillId = resolveSkillId(context);
        if (skillId == null) {
            return null;
        }

        long gameTime = context.level().getGameTime();
        if (!PlayerSkillCooldowns.isOnCooldown(player, skillId, gameTime)) {
            return null;
        }

        long remaining = PlayerSkillCooldowns.readyGameTime(player, skillId, gameTime)
                .stream()
                .map(readyGameTime -> Math.max(0L, readyGameTime - gameTime))
                .findFirst()
                .orElse(0L);
        return BLOCKED_WARNING_PREFIX + "cooldown " + skillId + " (" + remaining + " ticks remaining)";
    }

    private static String manaWarning(SkillExecutionContext context) {
        ServerPlayer player = resolvePlayer(context);
        if (player == null) {
            return null;
        }

        double maxMana = resolvedMaxMana(context);
        if (maxMana <= 0.0) {
            return null;
        }

        double resourceCost = resolvedResourceCost(context);
        if (resourceCost <= 0.0) {
            return null;
        }

        double currentMana = PlayerResources.getMana(player, maxMana);
        if (currentMana + 1.0E-6 >= resourceCost) {
            return null;
        }
        return BLOCKED_WARNING_PREFIX + "mana required=" + resourceCost + ", current=" + currentMana;
    }

    private static String chargeWarning(SkillExecutionContext context) {
        ServerPlayer player = resolvePlayer(context);
        if (player == null) {
            return null;
        }

        Identifier skillId = resolveSkillId(context);
        if (skillId == null) {
            return null;
        }

        int maxCharges = resolvedMaxCharges(context);
        if (maxCharges <= 0) {
            return null;
        }

        long chargeRegenTicks = resolvedChargeRegenTicks(context);
        if (chargeRegenTicks <= 0L) {
            return BLOCKED_WARNING_PREFIX + "charge_regen " + skillId;
        }

        long gameTime = context.level().getGameTime();
        int availableCharges = PlayerSkillCharges.availableCharges(player, skillId, maxCharges, gameTime);
        if (availableCharges > 0) {
            return null;
        }
        return BLOCKED_WARNING_PREFIX + "charges " + skillId + " (0/" + maxCharges + " available)";
    }

    private static String burstWarning(SkillExecutionContext context) {
        ServerPlayer player = resolvePlayer(context);
        if (player == null) {
            return null;
        }

        Identifier skillId = resolveSkillId(context);
        if (skillId == null) {
            return null;
        }

        int timesToCast = resolvedTimesToCast(context);
        if (timesToCast <= 1) {
            return null;
        }

        long gameTime = context.level().getGameTime();
        if (PlayerSkillBursts.canCast(player, skillId, timesToCast, gameTime)) {
            return null;
        }
        var burst = PlayerSkillBursts.entry(player, skillId, gameTime).orElse(null);
        if (burst == null) {
            return null;
        }

        long remainingWindow = Math.max(0L, burst.expiresAtGameTime() - gameTime);
        int totalFollowUps = Math.max(0, timesToCast - 1);
        return BLOCKED_WARNING_PREFIX + "burst " + skillId + " (0/" + totalFollowUps + " follow-up casts remaining, "
                + remainingWindow + " ticks until reset)";
    }

    private static ServerPlayer resolvePlayer(SkillExecutionContext context) {
        if (context.caster() instanceof ServerPlayer casterPlayer) {
            return casterPlayer;
        }
        if (context.source() instanceof ServerPlayer sourcePlayer) {
            return sourcePlayer;
        }
        return null;
    }

    private static Identifier resolveSkillId(SkillExecutionContext context) {
        return Identifier.tryParse(context.preparedUse().skill().identifier());
    }

    private static Identifier resolveDimensionId(SkillExecutionContext context) {
        String serialized = context.level().dimension().toString();
        Matcher matcher = IDENTIFIER_PATTERN.matcher(serialized);
        Identifier parsed = null;
        while (matcher.find()) {
            parsed = Identifier.tryParse(matcher.group());
        }
        return parsed;
    }

    private static double resolvedMaxMana(SkillExecutionContext context) {
        double mana = context.preparedUse().useContext().attackerStats().resolvedValue(CombatStats.MANA);
        if (!Double.isFinite(mana) || mana <= 0.0) {
            return 0.0;
        }
        return mana;
    }

    private static double resolvedResourceCost(SkillExecutionContext context) {
        double resourceCost = context.preparedUse().resourceCost();
        if (!Double.isFinite(resourceCost) || resourceCost <= 0.0) {
            return 0.0;
        }
        return resourceCost;
    }

    private static int resolvedMaxCharges(SkillExecutionContext context) {
        int charges = context.preparedUse().skill().config().charges();
        return Math.max(charges, 0);
    }

    private static long resolvedChargeRegenTicks(SkillExecutionContext context) {
        double chargeRegenSeconds = context.preparedUse().skill().config().chargeRegen();
        if (!Double.isFinite(chargeRegenSeconds) || chargeRegenSeconds <= 0.0) {
            return 0L;
        }
        return Math.max(1L, (long) Math.ceil(chargeRegenSeconds * 20.0));
    }

    private static int resolvedTimesToCast(SkillExecutionContext context) {
        int timesToCast = context.preparedUse().skill().config().timesToCast();
        return Math.max(1, timesToCast);
    }

    private static int countActions(List<PreparedSkillExecutionRoute> routes) {
        int total = 0;
        for (PreparedSkillExecutionRoute route : routes) {
            total += route.actions().size();
        }
        return total;
    }
}
