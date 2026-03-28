package kim.biryeong.esekai2.impl.skill.execution;

import de.tomalbrc.sandstorm.util.ParticleUtil;
import kim.biryeong.esekai2.Esekai2;
import kim.biryeong.esekai2.api.damage.calculation.DamageCalculationResult;
import kim.biryeong.esekai2.api.damage.calculation.DamageCalculations;
import kim.biryeong.esekai2.api.damage.calculation.HitDamageCalculation;
import kim.biryeong.esekai2.api.damage.critical.HitKind;
import kim.biryeong.esekai2.api.monster.stat.MonsterStats;
import kim.biryeong.esekai2.api.skill.execution.PreparedDamageAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedProjectileAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedSandstormParticleAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedSoundAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedSummonAtSightAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedSummonBlockAction;
import kim.biryeong.esekai2.api.skill.execution.SkillExecutionContext;
import kim.biryeong.esekai2.api.skill.execution.SkillExecutionHooks;
import kim.biryeong.esekai2.api.stat.holder.StatHolder;
import kim.biryeong.esekai2.api.stat.holder.StatHolders;
import kim.biryeong.esekai2.impl.skill.entity.SkillAnchoredEntity;
import kim.biryeong.esekai2.impl.skill.entity.SkillProjectileEntity;
import kim.biryeong.esekai2.impl.stat.registry.StatRegistryAccess;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Default server-side world hooks for skill execution.
 */
public final class ServerSkillExecutionHooks implements SkillExecutionHooks {
    public static final ServerSkillExecutionHooks INSTANCE = new ServerSkillExecutionHooks();
    private static final float DEFAULT_PROJECTILE_SPEED = 1.2F;

    private ServerSkillExecutionHooks() {
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
        context.level().playSound(context.caster(), position.x(), position.y(), position.z(), soundEvent, SoundSource.PLAYERS, action.volume(), action.pitch());
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
        SkillProjectileEntity projectile = SkillProjectileEntity.create(
                context.level(),
                skillId,
                action.componentId(),
                action.projectileEntityId(),
                action.lifeTicks(),
                action.gravity(),
                velocity
        );
        projectile.setPos(context.origin());
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
        return MonsterStats.resolveBaseHolder(target)
                .orElse(action.hitDamageCalculation().defenderStats() != null
                        ? action.hitDamageCalculation().defenderStats()
                        : StatHolders.create(StatRegistryAccess.statRegistry(context.level().getServer())));
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
        Vec3 destination = resolveDefaultPosition(context, targets);
        Vec3 direction = destination.subtract(context.origin());
        if (direction.lengthSqr() <= 1.0e-8) {
            direction = context.caster().getLookAngle();
        }
        if (direction.lengthSqr() <= 1.0e-8) {
            direction = new Vec3(0.0, 0.0, 1.0);
        }
        return direction.normalize().scale(DEFAULT_PROJECTILE_SPEED);
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
