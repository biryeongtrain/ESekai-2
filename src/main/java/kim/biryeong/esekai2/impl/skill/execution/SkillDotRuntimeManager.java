package kim.biryeong.esekai2.impl.skill.execution;

import kim.biryeong.esekai2.api.damage.breakdown.DamageBreakdown;
import kim.biryeong.esekai2.api.damage.calculation.DamageCalculations;
import kim.biryeong.esekai2.api.damage.calculation.DamageOverTimeCalculation;
import kim.biryeong.esekai2.api.damage.calculation.DamageOverTimeResult;
import kim.biryeong.esekai2.api.monster.stat.MonsterStats;
import kim.biryeong.esekai2.api.skill.execution.PreparedApplyDotAction;
import kim.biryeong.esekai2.api.skill.execution.SkillExecutionContext;
import kim.biryeong.esekai2.api.stat.holder.StatHolder;
import kim.biryeong.esekai2.api.stat.holder.StatHolders;
import kim.biryeong.esekai2.impl.ailment.AilmentRuntime;
import kim.biryeong.esekai2.impl.stat.registry.StatRegistryAccess;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Tracks generic skill-owned damage-over-time instances independently from entity components.
 */
public final class SkillDotRuntimeManager {
    private static final Map<SkillDotKey, ActiveSkillDot> ACTIVE = new LinkedHashMap<>();
    private static boolean bootstrapped;

    private SkillDotRuntimeManager() {
    }

    public static void bootstrap() {
        if (bootstrapped) {
            return;
        }

        ServerTickEvents.END_SERVER_TICK.register(SkillDotRuntimeManager::tickServer);
        ServerLifecycleEvents.SERVER_STARTING.register(server -> ACTIVE.clear());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> ACTIVE.clear());
        bootstrapped = true;
    }

    public static boolean registerOrRefresh(
            LivingEntity target,
            SkillExecutionContext context,
            PreparedApplyDotAction action
    ) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(action, "action");

        if (!(target.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        if (action.durationTicks() <= 0 || action.tickIntervalTicks() <= 0) {
            return false;
        }

        bootstrap();
        SkillDotKey key = new SkillDotKey(target.getUUID(), action.dotId());
        ACTIVE.put(key, new ActiveSkillDot(
                key,
                serverLevel,
                context.preparedUse().skill().identifier(),
                Optional.ofNullable(context.caster()).map(Entity::getUUID),
                Optional.ofNullable(context.source()).map(Entity::getUUID),
                action.damageCalculation(),
                action.durationTicks(),
                action.tickIntervalTicks()
        ));
        return true;
    }

    private static void tickServer(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            tickLevel(level);
        }
    }

    private static void tickLevel(ServerLevel level) {
        List<SkillDotKey> removals = new ArrayList<>();
        List<ActiveSkillDot> snapshot = new ArrayList<>(ACTIVE.values());

        for (ActiveSkillDot activeDot : snapshot) {
            if (!ACTIVE.containsKey(activeDot.key())) {
                continue;
            }
            if (activeDot.level() != level) {
                continue;
            }

            LivingEntity target = activeDot.resolveTarget();
            if (target == null || !target.isAlive() || target.isRemoved()) {
                removals.add(activeDot.key());
                continue;
            }

            int age = activeDot.advanceAge();
            if (age % activeDot.tickIntervalTicks() == 0) {
                applyTickDamage(level, activeDot, target);
            }
            if (age >= activeDot.durationTicks()) {
                removals.add(activeDot.key());
            }
        }

        for (SkillDotKey removal : removals) {
            ACTIVE.remove(removal);
        }
    }

    private static void applyTickDamage(ServerLevel level, ActiveSkillDot activeDot, LivingEntity target) {
        DamageOverTimeCalculation resolvedCalculation = copyCalculation(
                activeDot.damageCalculation(),
                resolveDefenderStats(level, target, activeDot.damageCalculation())
        );
        DamageOverTimeResult result = DamageCalculations.calculateDamageOverTime(resolvedCalculation);
        DamageBreakdown finalDamage = result.finalDamage();
        float damage = (float) finalDamage.totalAmount();
        if (damage <= 0.0F) {
            return;
        }

        // PoE-style periodic damage should not be suppressed by Minecraft hit i-frames.
        target.invulnerableTime = 0;
        target.hurtServer(level, resolveDamageSource(level, activeDot), damage);
    }

    private static DamageOverTimeCalculation copyCalculation(DamageOverTimeCalculation source, StatHolder defenderStats) {
        return new DamageOverTimeCalculation(
                source.baseDamage(),
                source.conversions(),
                source.extraGains(),
                source.scaling(),
                source.exposures(),
                defenderStats
        );
    }

    private static StatHolder resolveDefenderStats(
            ServerLevel level,
            LivingEntity target,
            DamageOverTimeCalculation calculation
    ) {
        StatHolder base = MonsterStats.resolveBaseHolder(target)
                .orElse(calculation.defenderStats() != null
                        ? calculation.defenderStats()
                        : StatHolders.create(StatRegistryAccess.statRegistry(level.getServer())));
        return AilmentRuntime.resolveDefenderStats(level, target, base);
    }

    private static DamageSource resolveDamageSource(ServerLevel level, ActiveSkillDot activeDot) {
        Entity source = activeDot.resolveSource();
        Entity caster = activeDot.resolveCaster();
        if (source != null && caster != null && source != caster) {
            return level.damageSources().indirectMagic(source, caster);
        }
        if (source != null) {
            return level.damageSources().indirectMagic(source, source);
        }
        if (caster != null) {
            return level.damageSources().indirectMagic(caster, caster);
        }
        return level.damageSources().magic();
    }

    private record SkillDotKey(UUID targetUuid, String dotId) {
        private SkillDotKey {
            Objects.requireNonNull(targetUuid, "targetUuid");
            Objects.requireNonNull(dotId, "dotId");
        }
    }

    private static final class ActiveSkillDot {
        private final SkillDotKey key;
        private final ServerLevel level;
        private final String sourceSkillId;
        private final Optional<UUID> casterUuid;
        private final Optional<UUID> sourceUuid;
        private final DamageOverTimeCalculation damageCalculation;
        private final int durationTicks;
        private final int tickIntervalTicks;
        private int ageTicks;

        private ActiveSkillDot(
                SkillDotKey key,
                ServerLevel level,
                String sourceSkillId,
                Optional<UUID> casterUuid,
                Optional<UUID> sourceUuid,
                DamageOverTimeCalculation damageCalculation,
                int durationTicks,
                int tickIntervalTicks
        ) {
            this.key = key;
            this.level = level;
            this.sourceSkillId = sourceSkillId;
            this.casterUuid = casterUuid;
            this.sourceUuid = sourceUuid;
            this.damageCalculation = damageCalculation;
            this.durationTicks = durationTicks;
            this.tickIntervalTicks = tickIntervalTicks;
        }

        private SkillDotKey key() {
            return key;
        }

        private ServerLevel level() {
            return level;
        }

        @SuppressWarnings("unused")
        private String sourceSkillId() {
            return sourceSkillId;
        }

        private DamageOverTimeCalculation damageCalculation() {
            return damageCalculation;
        }

        private int durationTicks() {
            return durationTicks;
        }

        private int tickIntervalTicks() {
            return tickIntervalTicks;
        }

        private int advanceAge() {
            ageTicks++;
            return ageTicks;
        }

        private LivingEntity resolveTarget() {
            Entity entity = level.getEntity(key.targetUuid());
            return entity instanceof LivingEntity livingEntity ? livingEntity : null;
        }

        private Entity resolveCaster() {
            return casterUuid.map(level::getEntity).orElse(null);
        }

        private Entity resolveSource() {
            return sourceUuid.map(level::getEntity).orElse(null);
        }
    }
}
