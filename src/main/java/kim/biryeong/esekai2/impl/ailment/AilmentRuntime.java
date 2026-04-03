package kim.biryeong.esekai2.impl.ailment;

import kim.biryeong.esekai2.api.ailment.AilmentPayload;
import kim.biryeong.esekai2.api.ailment.AilmentState;
import kim.biryeong.esekai2.api.ailment.AilmentType;
import kim.biryeong.esekai2.api.damage.breakdown.DamageBreakdown;
import kim.biryeong.esekai2.api.damage.breakdown.DamageType;
import kim.biryeong.esekai2.api.damage.calculation.DamageCalculationResult;
import kim.biryeong.esekai2.api.damage.calculation.DamageCalculations;
import kim.biryeong.esekai2.api.damage.calculation.DamageOverTimeCalculation;
import kim.biryeong.esekai2.api.damage.calculation.DamageOverTimeResult;
import kim.biryeong.esekai2.api.skill.effect.SkillAilmentRefreshPolicy;
import kim.biryeong.esekai2.api.stat.combat.CombatStats;
import kim.biryeong.esekai2.api.stat.definition.StatDefinition;
import kim.biryeong.esekai2.api.stat.holder.StatHolder;
import kim.biryeong.esekai2.api.stat.holder.StatHolders;
import kim.biryeong.esekai2.api.stat.modifier.StatModifier;
import kim.biryeong.esekai2.api.stat.modifier.StatModifierOperation;
import kim.biryeong.esekai2.api.stat.value.StatInstance;
import kim.biryeong.esekai2.impl.stat.registry.StatRegistryAccess;
import kim.biryeong.esekai2.impl.stat.runtime.LivingEntityCombatStatResolver;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Runtime helpers that apply, tick, and resolve attachment-backed ailments.
 */
public final class AilmentRuntime {
    private static final int DEFAULT_TICK_INTERVAL = AilmentPayload.DEFAULT_DAMAGE_TICK_INTERVAL_TICKS;
    private static final double IGNITE_TOTAL_RATIO = 0.50;
    private static final double POISON_TOTAL_RATIO = 0.30;
    private static final double BLEED_TOTAL_RATIO = 0.70;
    // Shock and chill store percentage payloads instead of periodic damage.
    private static final double SHOCK_PERCENT_RATIO = 0.50;
    private static final double SHOCK_PERCENT_CAP = 50.0;
    private static final double CHILL_PERCENT_RATIO = 0.30;
    private static final double CHILL_PERCENT_CAP = 30.0;

    private AilmentRuntime() {
    }

    public static boolean apply(
            LivingEntity target,
            Entity sourceEntity,
            String sourceSkillId,
            AilmentType type,
            DamageCalculationResult hitResult,
            StatHolder attackerStats,
            int durationTicks,
            double potencyMultiplier,
            SkillAilmentRefreshPolicy refreshPolicy
    ) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(sourceSkillId, "sourceSkillId");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(hitResult, "hitResult");
        Objects.requireNonNull(attackerStats, "attackerStats");
        Objects.requireNonNull(refreshPolicy, "refreshPolicy");
        if (durationTicks <= 0) {
            return false;
        }

        double finalDealtDamage = hitResult.finalDamage().totalAmount();
        if (!Double.isFinite(finalDealtDamage) || finalDealtDamage <= 0.0) {
            return false;
        }

        int effectiveDurationTicks = type == AilmentType.FREEZE || type == AilmentType.STUN
                ? controlAilmentDurationTicks(target, attackerStats, type, finalDealtDamage, durationTicks)
                : durationTicks;
        if (effectiveDurationTicks <= 0) {
            return false;
        }

        AilmentPayload incoming = createPayload(
                type,
                sourceSkillId,
                Optional.ofNullable(sourceEntity).map(Entity::getUUID),
                finalDealtDamage,
                effectiveDurationTicks,
                potencyMultiplier
        );
        if (type.usesPotency() && incoming.potency() <= 0.0) {
            return false;
        }
        Optional<AilmentPayload> current = AilmentBootstrap.get(target).flatMap(state -> state.get(type));
        if (current.isPresent() && !shouldReplace(current.orElseThrow(), incoming, refreshPolicy)) {
            return false;
        }

        AilmentState updatedState = AilmentBootstrap.get(target).orElse(AilmentState.EMPTY).with(incoming);
        AilmentBootstrap.attach(target, updatedState);
        applyEffectIdentity(target, incoming);
        return true;
    }

    public static boolean tick(ServerLevel level, LivingEntity entity, AilmentType type) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(type, "type");

        Optional<AilmentPayload> current = AilmentBootstrap.get(entity).flatMap(state -> state.get(type));
        if (current.isEmpty()) {
            removeEffectIdentity(entity, type);
            return false;
        }

        AilmentPayload ticked = current.orElseThrow().tick();
        boolean producedSideEffect = false;
        if (ticked.shouldTriggerDamageTick()) {
            producedSideEffect = applyPeriodicDamage(level, entity, ticked);
        }

        AilmentState baseState = AilmentBootstrap.get(entity).orElse(AilmentState.EMPTY);
        if (ticked.isExpired()) {
            AilmentState updated = baseState.without(type);
            AilmentBootstrap.attach(entity, updated);
            removeEffectIdentity(entity, type);
            return producedSideEffect;
        }

        AilmentBootstrap.attach(entity, baseState.with(ticked));
        return producedSideEffect;
    }

    public static double shockDamageTakenMore(LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");
        return AilmentBootstrap.get(entity)
                .flatMap(state -> state.get(AilmentType.SHOCK))
                .map(AilmentPayload::potency)
                .orElse(0.0);
    }

    public static boolean remove(LivingEntity entity, AilmentType type) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(type, "type");

        boolean hadPayload = AilmentBootstrap.get(entity)
                .flatMap(state -> state.get(type))
                .isPresent();
        boolean hadEffectIdentity = entity.getEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(AilmentBootstrap.effect(type))) != null;
        if (!hadPayload && !hadEffectIdentity) {
            return false;
        }

        AilmentState updated = AilmentBootstrap.get(entity).orElse(AilmentState.EMPTY).without(type);
        AilmentBootstrap.attach(entity, updated);
        removeEffectIdentity(entity, type);
        return true;
    }

    public static boolean remove(LivingEntity entity, Identifier effectId) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(effectId, "effectId");

        AilmentType type = ailmentType(effectId);
        if (type == null) {
            return false;
        }
        return remove(entity, type);
    }

    static boolean hasActivePayload(LivingEntity entity, AilmentType type) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(type, "type");

        return AilmentBootstrap.get(entity)
                .flatMap(state -> state.get(type))
                .filter(payload -> !payload.isExpired())
                .isPresent();
    }

    public static Optional<AilmentType> blockingSkillExecutionAilment(LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");

        if (hasActive(entity, AilmentType.FREEZE)) {
            return Optional.of(AilmentType.FREEZE);
        }
        if (hasActive(entity, AilmentType.STUN)) {
            return Optional.of(AilmentType.STUN);
        }
        return Optional.empty();
    }

    public static StatHolder resolveDefenderStats(ServerLevel level, LivingEntity target, StatHolder fallback) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(fallback, "fallback");

        double shockMore = shockDamageTakenMore(target);
        if (!Double.isFinite(shockMore) || shockMore == 0.0) {
            return fallback;
        }

        StatHolder copy = StatHolders.create(StatRegistryAccess.statRegistry(level.getServer()));
        for (Map.Entry<net.minecraft.resources.ResourceKey<StatDefinition>, StatInstance> entry : fallback.snapshot().entrySet()) {
            copy.setBaseValue(entry.getKey(), entry.getValue().baseValue());
            for (StatModifier modifier : entry.getValue().modifiers()) {
                copy.addModifier(modifier);
            }
        }
        copy.addModifier(new StatModifier(
                CombatStats.DAMAGE_TAKEN_MORE,
                StatModifierOperation.ADD,
                shockMore,
                Identifier.fromNamespaceAndPath("esekai2", "shock_taken_more")
        ));
        return copy;
    }

    private static boolean applyPeriodicDamage(ServerLevel level, LivingEntity target, AilmentPayload payload) {
        DamageType damageType = payload.type().damageType();
        if (damageType == null || payload.potency() <= 0.0) {
            return false;
        }

        StatHolder defenderStats = resolveDefenderStats(level, target, fallbackStats(level, target));
        DamageOverTimeResult result = DamageCalculations.calculateDamageOverTime(new DamageOverTimeCalculation(
                DamageBreakdown.of(damageType, payload.potency()),
                java.util.List.of(),
                java.util.List.of(),
                java.util.List.of(),
                java.util.List.of(),
                defenderStats
        ));
        float damage = (float) result.finalDamage().totalAmount();
        if (damage <= 0.0F) {
            return false;
        }
        // PoE-style periodic damage should not be suppressed by Minecraft hit i-frames.
        target.invulnerableTime = 0;
        target.hurtServer(level, damageSource(level, payload), damage);
        return true;
    }

    private static StatHolder fallbackStats(ServerLevel level, LivingEntity target) {
        return kim.biryeong.esekai2.api.monster.stat.MonsterStats.resolveBaseHolder(target)
                .orElse(StatHolders.create(StatRegistryAccess.statRegistry(level.getServer())));
    }

    private static DamageSource damageSource(ServerLevel level, AilmentPayload payload) {
        Entity source = payload.sourceEntityUuid().map(level::getEntity).orElse(null);
        if (source != null) {
            return level.damageSources().indirectMagic(source, source);
        }
        return level.damageSources().magic();
    }

    private static AilmentPayload createPayload(
            AilmentType type,
            String sourceSkillId,
            Optional<UUID> sourceEntityUuid,
            double finalDealtDamage,
            int durationTicks,
            double potencyMultiplier
    ) {
        double multiplier = potencyMultiplier / 100.0;
        if (!Double.isFinite(multiplier) || multiplier < 0.0) {
            multiplier = 0.0;
        }
        return switch (type) {
            case IGNITE -> createDamagePayload(type, sourceSkillId, sourceEntityUuid, finalDealtDamage, durationTicks, multiplier, IGNITE_TOTAL_RATIO);
            case POISON -> createDamagePayload(type, sourceSkillId, sourceEntityUuid, finalDealtDamage, durationTicks, multiplier, POISON_TOTAL_RATIO);
            case BLEED -> createDamagePayload(type, sourceSkillId, sourceEntityUuid, finalDealtDamage, durationTicks, multiplier, BLEED_TOTAL_RATIO);
            case SHOCK -> new AilmentPayload(
                    type,
                    sourceSkillId,
                    sourceEntityUuid,
                    Math.min(SHOCK_PERCENT_CAP, finalDealtDamage * SHOCK_PERCENT_RATIO * multiplier),
                    durationTicks,
                    durationTicks,
                    1
            );
            case CHILL -> new AilmentPayload(
                    type,
                    sourceSkillId,
                    sourceEntityUuid,
                    chillPercent(finalDealtDamage, multiplier),
                    durationTicks,
                    durationTicks,
                    1
            );
            case FREEZE, STUN -> new AilmentPayload(
                    type,
                    sourceSkillId,
                    sourceEntityUuid,
                    0.0,
                    durationTicks,
                    durationTicks,
                    1
            );
        };
    }

    private static AilmentPayload createDamagePayload(
            AilmentType type,
            String sourceSkillId,
            Optional<UUID> sourceEntityUuid,
            double finalDealtDamage,
            int durationTicks,
            double multiplier,
            double totalDamageRatio
    ) {
        int tickInterval = DEFAULT_TICK_INTERVAL;
        int tickCount = Math.max(1, (durationTicks + tickInterval - 1) / tickInterval);
        double totalDamage = finalDealtDamage * totalDamageRatio * multiplier;
        double damagePerTick = tickCount <= 0 ? 0.0 : totalDamage / tickCount;
        return new AilmentPayload(type, sourceSkillId, sourceEntityUuid, damagePerTick, durationTicks, durationTicks, tickInterval);
    }

    private static int controlAilmentDurationTicks(
            LivingEntity target,
            StatHolder attackerStats,
            AilmentType type,
            double finalDealtDamage,
            int baseDurationTicks
    ) {
        double ailmentThreshold = controlAilmentThreshold(target);
        double durationIncreased = controlAilmentDurationIncreased(attackerStats, type);
        double factor = clamp(finalDealtDamage / Math.max(1.0, ailmentThreshold), 0.0, 1.0);
        double scaledDuration = Math.floor(baseDurationTicks * factor * (1.0 + durationIncreased / 100.0));
        if (!Double.isFinite(scaledDuration) || scaledDuration <= 0.0) {
            return 0;
        }
        if (scaledDuration >= Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) scaledDuration;
    }

    private static double controlAilmentThreshold(LivingEntity target) {
        Optional<StatHolder> holder = LivingEntityCombatStatResolver.resolve(target);
        double threshold = holder.map(statHolder -> statHolder.resolvedValue(CombatStats.AILMENT_THRESHOLD)).orElse(0.0);
        if (!Double.isFinite(threshold) || threshold <= 0.0) {
            threshold = holder.map(statHolder -> statHolder.resolvedValue(CombatStats.LIFE)).orElse(0.0);
        }
        if (!Double.isFinite(threshold) || threshold <= 0.0) {
            threshold = 1.0;
        }
        return threshold;
    }

    private static double controlAilmentDurationIncreased(StatHolder attackerStats, AilmentType type) {
        double durationIncreased = switch (type) {
            case FREEZE -> attackerStats.resolvedValue(CombatStats.FREEZE_DURATION_INCREASED);
            case STUN -> attackerStats.resolvedValue(CombatStats.STUN_DURATION_INCREASED);
            default -> 0.0;
        };
        if (!Double.isFinite(durationIncreased)) {
            return 0.0;
        }
        return durationIncreased;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static boolean shouldReplace(
            AilmentPayload current,
            AilmentPayload incoming,
            SkillAilmentRefreshPolicy refreshPolicy
    ) {
        return switch (refreshPolicy) {
            case OVERWRITE -> true;
            case LONGER_ONLY -> incoming.remainingTicks() >= current.remainingTicks();
            case STRONGER_ONLY -> {
                if (incoming.potency() > current.potency()) {
                    yield true;
                }
                if (incoming.potency() < current.potency()) {
                    yield false;
                }
                yield incoming.remainingTicks() >= current.remainingTicks();
            }
        };
    }

    private static void applyEffectIdentity(LivingEntity target, AilmentPayload payload) {
        MobEffect effect = AilmentBootstrap.effect(payload.type());
        target.removeEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect));
        target.addEffect(new MobEffectInstance(
                BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect),
                payload.remainingTicks(),
                effectAmplifier(payload),
                false,
                false,
                false
        ));
    }

    private static void removeEffectIdentity(LivingEntity entity, AilmentType type) {
        entity.removeEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(AilmentBootstrap.effect(type)));
    }

    private static AilmentType ailmentType(Identifier effectId) {
        for (AilmentType type : AilmentType.values()) {
            if (type.effectId().equals(effectId)) {
                return type;
            }
        }
        return null;
    }

    private static boolean hasActive(LivingEntity entity, AilmentType type) {
        return hasActivePayload(entity, type)
                || entity.getEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(AilmentBootstrap.effect(type))) != null;
    }

    private static double chillPercent(double finalDealtDamage, double multiplier) {
        return Math.min(CHILL_PERCENT_CAP, finalDealtDamage * CHILL_PERCENT_RATIO * multiplier);
    }

    private static int effectAmplifier(AilmentPayload payload) {
        if (payload.type() != AilmentType.CHILL) {
            return 0;
        }

        int potencyPercent = (int) Math.ceil(Math.min(CHILL_PERCENT_CAP, payload.potency()));
        if (potencyPercent <= 0) {
            return 0;
        }
        return Math.max(0, potencyPercent - 1);
    }
}
