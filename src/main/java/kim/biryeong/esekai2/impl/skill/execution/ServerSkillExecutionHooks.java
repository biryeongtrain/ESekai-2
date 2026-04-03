package kim.biryeong.esekai2.impl.skill.execution;

import de.tomalbrc.sandstorm.util.ParticleUtil;
import kim.biryeong.esekai2.Esekai2;
import kim.biryeong.esekai2.api.ailment.AilmentType;
import kim.biryeong.esekai2.api.damage.calculation.DamageCalculationResult;
import kim.biryeong.esekai2.api.damage.calculation.DamageCalculations;
import kim.biryeong.esekai2.api.damage.calculation.HitDamageCalculation;
import kim.biryeong.esekai2.api.damage.critical.HitKind;
import kim.biryeong.esekai2.api.player.resource.PlayerResources;
import kim.biryeong.esekai2.api.skill.execution.PreparedApplyAilmentAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedApplyBuffAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedApplyDotAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedDamageAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedHealAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedProjectileAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedRemoveEffectAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedResourceDeltaAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedSandstormParticleAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedSoundAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedSummonAtSightAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedSummonBlockAction;
import kim.biryeong.esekai2.api.skill.execution.SkillExecutionContext;
import kim.biryeong.esekai2.api.skill.execution.SkillExecutionHooks;
import kim.biryeong.esekai2.api.skill.effect.SkillEffectPurgeMode;
import kim.biryeong.esekai2.api.stat.holder.StatHolder;
import kim.biryeong.esekai2.impl.ailment.AilmentRuntime;
import kim.biryeong.esekai2.impl.player.resource.PlayerResourceService;
import kim.biryeong.esekai2.impl.skill.entity.SkillAnchoredEntity;
import kim.biryeong.esekai2.impl.skill.entity.SkillProjectileEntity;
import kim.biryeong.esekai2.impl.stat.runtime.LivingEntityCombatStatResolver;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Default server-side world hooks for skill execution.
 */
public final class ServerSkillExecutionHooks implements SkillExecutionHooks {
    public static final ServerSkillExecutionHooks INSTANCE = new ServerSkillExecutionHooks();
    private static final float DEFAULT_PROJECTILE_SPEED = 1.2F;

    private ServerSkillExecutionHooks() {
        SkillDotRuntimeManager.bootstrap();
    }

    @Override
    public boolean playSound(SkillExecutionContext context, List<Entity> targets, PreparedSoundAction action) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(targets, "targets");
        Objects.requireNonNull(action, "action");

        SoundEvent soundEvent = BuiltInRegistries.SOUND_EVENT.getOptional(action.soundId()).orElse(null);
        if (soundEvent == null) {
            return false;
        }

        Vec3 position = resolveDefaultPosition(context, targets);
        context.level().playSound(null, position.x(), position.y(), position.z(), soundEvent, SoundSource.PLAYERS, action.volume(), action.pitch());
        return true;
    }

    @Override
    public Optional<DamageCalculationResult> applyDamage(
            SkillExecutionContext context,
            List<Entity> targets,
            PreparedDamageAction action
    ) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(targets, "targets");
        Objects.requireNonNull(action, "action");

        DamageCalculationResult lastResult = null;
        for (Entity target : targets) {
            if (!(target instanceof LivingEntity livingTarget)) {
                continue;
            }

            StatHolder defenderStats = resolveDefenderStats(context, livingTarget, action);
            HitDamageCalculation resolvedCalculation = copyCalculation(action.hitDamageCalculation(), defenderStats);
            DamageCalculationResult result = DamageCalculations.calculateHit(resolvedCalculation);
            float damage = (float) result.finalDamage().totalAmount();
            if (damage > 0.0F) {
                livingTarget.hurtServer(context.level(), resolveDamageSource(context, action), damage);
            }
            lastResult = result;
        }

        return Optional.ofNullable(lastResult);
    }

    @Override
    public boolean heal(
            SkillExecutionContext context,
            List<Entity> targets,
            PreparedHealAction action
    ) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(targets, "targets");
        Objects.requireNonNull(action, "action");

        boolean healed = false;
        for (Entity target : targets) {
            if (!(target instanceof LivingEntity livingTarget) || !livingTarget.isAlive()) {
                continue;
            }

            float previousHealth = livingTarget.getHealth();
            livingTarget.heal((float) action.amount());
            healed |= livingTarget.getHealth() > previousHealth;
        }
        return healed;
    }

    @Override
    public boolean applyResourceDelta(
            SkillExecutionContext context,
            List<Entity> targets,
            PreparedResourceDeltaAction action
    ) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(targets, "targets");
        Objects.requireNonNull(action, "action");

        if (!PlayerResources.supports(action.resource())) {
            return false;
        }

        boolean applied = false;
        for (Entity target : targets) {
            if (!(target instanceof net.minecraft.server.level.ServerPlayer player)) {
                continue;
            }
            if (target != context.source()) {
                continue;
            }

            double maxAmount = PlayerResources.maxAmount(player, action.resource());
            if ((!Double.isFinite(maxAmount) || maxAmount <= 0.0) && context.level().getServer() != null) {
                maxAmount = PlayerResourceService.definition(context.level().getServer(), action.resource())
                        .map(definition -> context.preparedUse().useContext().attackerStats().resolvedValue(definition.maxStat()))
                        .orElse(0.0);
            }

            double previousAmount = maxAmount > 0.0
                    ? PlayerResources.getAmount(player, action.resource(), maxAmount)
                    : PlayerResources.getAmount(player, action.resource());
            double nextAmount = maxAmount > 0.0
                    ? PlayerResources.add(player, action.resource(), action.amount(), maxAmount).currentAmount()
                    : PlayerResources.add(player, action.resource(), action.amount()).currentAmount();
            applied |= Double.compare(previousAmount, nextAmount) != 0;
        }
        return applied;
    }

    @Override
    public boolean applyEffect(
            SkillExecutionContext context,
            List<Entity> targets,
            PreparedApplyBuffAction action
    ) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(targets, "targets");
        Objects.requireNonNull(action, "action");

        if (action.durationTicks() <= 0) {
            return false;
        }

        MobEffect effect = BuiltInRegistries.MOB_EFFECT.getOptional(action.effectId()).orElse(null);
        if (effect == null) {
            return false;
        }

        boolean applied = false;
        for (Entity target : targets) {
            if (!(target instanceof LivingEntity livingTarget)) {
                continue;
            }

            MobEffectInstance effectInstance = resolveEffectInstance(livingTarget, effect, action);
            if (effectInstance == null) {
                continue;
            }

            livingTarget.removeEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect));
            applied |= livingTarget.addEffect(effectInstance);
        }
        return applied;
    }

    @Override
    public boolean removeEffect(
            SkillExecutionContext context,
            List<Entity> targets,
            PreparedRemoveEffectAction action
    ) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(targets, "targets");
        Objects.requireNonNull(action, "action");

        boolean removed = false;
        for (Entity target : targets) {
            if (!(target instanceof LivingEntity livingTarget)) {
                continue;
            }

            java.util.LinkedHashSet<Identifier> effectIds = new java.util.LinkedHashSet<>(action.effectIds());
            action.purgeMode().ifPresent(mode -> collectPurgedEffectIds(livingTarget, mode, effectIds));

            for (Identifier effectId : effectIds) {
                MobEffect effect = BuiltInRegistries.MOB_EFFECT.getOptional(effectId).orElse(null);
                if (effect == null) {
                    continue;
                }

                if (AilmentRuntime.remove(livingTarget, effectId)) {
                    removed = true;
                    continue;
                }

                if (livingTarget.getEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect)) == null) {
                    continue;
                }

                livingTarget.removeEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect));
                removed = true;
            }
        }
        return removed;
    }

    private static void collectPurgedEffectIds(
            LivingEntity livingTarget,
            SkillEffectPurgeMode mode,
            java.util.Set<Identifier> removalIds
    ) {
        Objects.requireNonNull(livingTarget, "livingTarget");
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(removalIds, "removalIds");

        for (MobEffectInstance effectInstance : livingTarget.getActiveEffects()) {
            MobEffect effect = effectInstance.getEffect().value();
            if (matchesPurgeMode(mode, effect.getCategory())) {
                removalIds.add(BuiltInRegistries.MOB_EFFECT.getKey(effect));
            }
        }

        if (mode == SkillEffectPurgeMode.NEGATIVE || mode == SkillEffectPurgeMode.ALL) {
            for (AilmentType ailmentType : AilmentType.values()) {
                removalIds.add(ailmentType.effectId());
            }
        }
    }

    private static boolean matchesPurgeMode(SkillEffectPurgeMode mode, MobEffectCategory category) {
        return switch (mode) {
            case POSITIVE -> category == MobEffectCategory.BENEFICIAL;
            case NEGATIVE -> category == MobEffectCategory.HARMFUL;
            case ALL -> true;
        };
    }

    private static MobEffectInstance resolveEffectInstance(
            LivingEntity target,
            MobEffect effect,
            PreparedApplyBuffAction action
    ) {
        MobEffectInstance existing = target.getEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect));
        if (existing == null) {
            return createEffectInstance(effect, action.durationTicks(), action.amplifier(), action);
        }

        return switch (action.refreshPolicy()) {
            case OVERWRITE -> createEffectInstance(effect, action.durationTicks(), action.amplifier(), action);
            case ADD_DURATION -> createEffectInstance(
                    effect,
                    existing.getDuration() + action.durationTicks(),
                    Math.max(existing.getAmplifier(), action.amplifier()),
                    action
            );
            case LONGER_ONLY -> resolveLongerOnlyInstance(effect, existing, action);
        };
    }

    private static MobEffectInstance resolveLongerOnlyInstance(
            MobEffect effect,
            MobEffectInstance existing,
            PreparedApplyBuffAction action
    ) {
        if (existing.getAmplifier() > action.amplifier()) {
            return null;
        }
        if (existing.getAmplifier() == action.amplifier() && existing.getDuration() >= action.durationTicks()) {
            return null;
        }
        return createEffectInstance(effect, action.durationTicks(), action.amplifier(), action);
    }

    private static MobEffectInstance createEffectInstance(
            MobEffect effect,
            int durationTicks,
            int amplifier,
            PreparedApplyBuffAction action
    ) {
        return new MobEffectInstance(
                BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect),
                durationTicks,
                amplifier,
                action.ambient(),
                action.showParticles(),
                action.showIcon()
        );
    }

    @Override
    public boolean applyDamageOverTime(
            SkillExecutionContext context,
            List<Entity> targets,
            PreparedApplyDotAction action
    ) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(targets, "targets");
        Objects.requireNonNull(action, "action");

        if (action.durationTicks() <= 0 || action.tickIntervalTicks() <= 0) {
            return false;
        }

        boolean applied = false;
        for (Entity target : targets) {
            if (!(target instanceof LivingEntity livingTarget)) {
                continue;
            }
            applied |= SkillDotRuntimeManager.registerOrRefresh(livingTarget, context, action);
        }
        return applied;
    }

    @Override
    public boolean applyAilment(
            SkillExecutionContext context,
            List<Entity> targets,
            PreparedApplyAilmentAction action,
            Map<UUID, DamageCalculationResult> latestDamageResults
    ) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(targets, "targets");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(latestDamageResults, "latestDamageResults");

        if (action.durationTicks() <= 0 || action.chancePercent() <= 0.0) {
            return false;
        }
        if (context.preparedUse().useContext().hitRoll() >= action.chancePercent() / 100.0) {
            return false;
        }

        boolean applied = false;
        for (Entity target : targets) {
            if (!(target instanceof LivingEntity livingTarget)) {
                continue;
            }

            DamageCalculationResult hitResult = latestDamageResults.get(livingTarget.getUUID());
            if (hitResult == null || hitResult.finalDamage().totalAmount() <= 0.0) {
                continue;
            }

            applied |= AilmentRuntime.apply(
                    livingTarget,
                    context.source(),
                    context.preparedUse().skill().identifier(),
                    action.ailmentType(),
                    hitResult,
                    context.preparedUse().useContext().attackerStats(),
                    action.durationTicks(),
                    action.potencyMultiplierPercent(),
                    action.refreshPolicy()
            );
        }
        return applied;
    }

    @Override
    public Optional<Entity> spawnProjectile(
            SkillExecutionContext context,
            List<Entity> targets,
            PreparedProjectileAction action
    ) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(targets, "targets");
        Objects.requireNonNull(action, "action");

        Identifier skillId = Identifier.tryParse(context.preparedUse().skill().identifier());
        if (skillId == null) {
            skillId = Identifier.fromNamespaceAndPath(Esekai2.MOD_ID, "unknown_skill");
        }

        Vec3 velocity = resolveProjectileVelocity(context, targets);
        Vec3 spawnPosition = resolveProjectileSpawnPosition(context, velocity);
        SkillProjectileEntity projectile = SkillProjectileEntity.create(
                context.level(),
                skillId,
                action.componentId(),
                action.projectileEntityId(),
                action.lifeTicks(),
                action.gravity(),
                velocity
        );
        projectile.setPos(spawnPosition);
        projectile.setOwnerUuid(context.caster().getUUID());
        if (!context.level().tryAddFreshEntityWithPassengers(projectile)) {
            return Optional.empty();
        }

        SkillRuntimeManager.register(projectile, context, action.componentId(), action.lifeTicks(), null, null);
        return Optional.of(projectile);
    }

    @Override
    public Optional<Entity> spawnSummonAtSight(
            SkillExecutionContext context,
            List<Entity> targets,
            PreparedSummonAtSightAction action
    ) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(targets, "targets");
        Objects.requireNonNull(action, "action");

        Identifier entityId = Identifier.tryParse(action.summonEntityId());
        if (entityId == null) {
            return Optional.empty();
        }

        Entity entity = BuiltInRegistries.ENTITY_TYPE.getOptional(entityId)
                .map(type -> type.create(context.level(), EntitySpawnReason.TRIGGERED))
                .orElse(null);
        if (entity == null) {
            return Optional.empty();
        }

        entity.setPos(resolveDefaultPosition(context, targets));
        entity.setNoGravity(!action.gravity());
        if (!context.level().tryAddFreshEntityWithPassengers(entity)) {
            return Optional.empty();
        }

        if (shouldTrackComponent(context, action.componentId(), action.lifeTicks())) {
            SkillRuntimeManager.register(entity, context, action.componentId(), action.lifeTicks(), null, null);
        }

        return Optional.of(entity);
    }

    @Override
    public boolean placeBlock(SkillExecutionContext context, List<Entity> targets, PreparedSummonBlockAction action) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(targets, "targets");
        Objects.requireNonNull(action, "action");

        Identifier blockId = Identifier.tryParse(action.blockId());
        if (blockId == null) {
            return false;
        }

        Block block = BuiltInRegistries.BLOCK.getOptional(blockId).orElse(null);
        if (block == null) {
            return false;
        }

        BlockPos blockPos = resolveBlockPosition(context, targets);
        BlockState previousState = context.level().getBlockState(blockPos);
        if (!context.level().setBlock(blockPos, block.defaultBlockState(), 3)) {
            return false;
        }

        if (!shouldTrackComponent(context, action.componentId(), action.lifeTicks())) {
            return true;
        }

        Identifier skillId = Identifier.tryParse(context.preparedUse().skill().identifier());
        if (skillId == null) {
            skillId = Identifier.fromNamespaceAndPath(Esekai2.MOD_ID, "unknown_skill");
        }

        SkillAnchoredEntity anchor = SkillAnchoredEntity.create(
                context.level(),
                skillId,
                action.componentId(),
                action.blockId(),
                action.lifeTicks(),
                false
        );
        Vec3 anchorPos = Vec3.atCenterOf(blockPos);
        anchor.setPos(anchorPos);
        if (!context.level().addFreshEntity(anchor)) {
            context.level().setBlock(blockPos, previousState, 3);
            return false;
        }

        SkillRuntimeManager.register(anchor, context, action.componentId(), action.lifeTicks(), blockPos, previousState);
        return true;
    }

    @Override
    public boolean emitSandstormParticle(
            SkillExecutionContext context,
            List<Entity> targets,
            PreparedSandstormParticleAction action
    ) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(targets, "targets");
        Objects.requireNonNull(action, "action");

        List<Vec3> positions = resolveParticlePositions(context, targets, action);
        if (positions.isEmpty()) {
            return false;
        }

        Vector2f rotation = new Vector2f(0.0F, 0.0F);
        for (Vec3 position : positions) {
            ParticleUtil.emit(action.particleId(), context.level(), position, rotation);
        }
        return true;
    }

    private static boolean shouldTrackComponent(SkillExecutionContext context, String componentId, int lifeTicks) {
        return lifeTicks > 0 || context.preparedUse().components().containsKey(componentId);
    }

    private static HitDamageCalculation copyCalculation(HitDamageCalculation source, StatHolder defenderStats) {
        return new HitDamageCalculation(
                source.baseDamage(),
                source.conversions(),
                source.extraGains(),
                source.scaling(),
                source.exposures(),
                source.penetrations(),
                source.attackerStats(),
                source.hitContext(),
                defenderStats
        );
    }

    private static StatHolder resolveDefenderStats(
            SkillExecutionContext context,
            LivingEntity target,
            PreparedDamageAction action
    ) {
        StatHolder base = LivingEntityCombatStatResolver.resolve(target)
                .orElse(action.hitDamageCalculation().defenderStats() != null
                        ? action.hitDamageCalculation().defenderStats()
                        : LivingEntityCombatStatResolver.empty(context.level().getServer()));
        return AilmentRuntime.resolveDefenderStats(context.level(), target, base);
    }

    private static DamageSource resolveDamageSource(SkillExecutionContext context, PreparedDamageAction action) {
        HitKind hitKind = action.hitDamageCalculation().hitContext().kind();
        Entity caster = context.caster();

        if (hitKind == HitKind.ATTACK) {
            if (caster instanceof Player player) {
                return context.level().damageSources().playerAttack(player);
            }
            if (caster instanceof LivingEntity living) {
                return context.level().damageSources().mobAttack(living);
            }
            return context.level().damageSources().generic();
        }

        if (context.source() != context.caster()) {
            return context.level().damageSources().indirectMagic(context.source(), context.caster());
        }
        return context.level().damageSources().magic();
    }

    private static Vec3 resolveProjectileVelocity(SkillExecutionContext context, List<Entity> targets) {
        Vec3 destination = context.target()
                .map(target -> target instanceof LivingEntity livingEntity ? livingEntity.getEyePosition() : target.position())
                .orElseGet(context::impactPosition);
        Vec3 direction = destination.subtract(context.origin());
        if (direction.lengthSqr() <= 1.0e-8) {
            direction = resolveDefaultPosition(context, targets).subtract(context.origin());
        }
        if (direction.lengthSqr() <= 1.0e-8) {
            direction = new Vec3(0.0, 0.0, 1.0);
        }
        return direction.normalize().scale(DEFAULT_PROJECTILE_SPEED);
    }

    private static Vec3 resolveProjectileSpawnPosition(SkillExecutionContext context, Vec3 velocity) {
        Vec3 forward = velocity.lengthSqr() > 1.0e-8 ? velocity.normalize() : context.caster().getLookAngle();
        if (forward.lengthSqr() <= 1.0e-8) {
            forward = new Vec3(0.0, 0.0, 1.0);
        }

        Vec3 base = context.caster().position().add(0.0, context.caster().getBbHeight() * 0.75, 0.0);
        return base.add(forward.scale(0.5));
    }

    private static Vec3 resolveDefaultPosition(SkillExecutionContext context, List<Entity> targets) {
        if (!targets.isEmpty()) {
            Entity target = targets.get(0);
            if (target instanceof LivingEntity livingEntity) {
                return livingEntity.getEyePosition();
            }
            return target.position();
        }
        return context.impactPosition();
    }

    private static BlockPos resolveBlockPosition(SkillExecutionContext context, List<Entity> targets) {
        if (!targets.isEmpty()) {
            return targets.get(0).blockPosition();
        }
        return context.resolveImpactBlockPos();
    }

    private static List<Vec3> resolveParticlePositions(
            SkillExecutionContext context,
            List<Entity> targets,
            PreparedSandstormParticleAction action
    ) {
        List<Vec3> positions = new ArrayList<>();
        Vec3 offset = new Vec3(action.offsetX(), action.offsetY(), action.offsetZ());

        switch (action.anchor()) {
            case "target" -> {
                for (Entity target : targets) {
                    positions.add(target.position().add(offset));
                }
            }
            case "impact_point" -> positions.add(context.impactPosition().add(offset));
            case "caster_hand" -> positions.add(context.caster().getEyePosition().add(context.caster().getLookAngle().scale(0.5)).add(offset));
            case "self" -> positions.add(context.source().position().add(offset));
            default -> positions.add(resolveDefaultPosition(context, targets).add(offset));
        }

        return positions;
    }
}
