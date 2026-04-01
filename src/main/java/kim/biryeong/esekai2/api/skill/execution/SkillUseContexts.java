package kim.biryeong.esekai2.api.skill.execution;

import kim.biryeong.esekai2.api.player.resource.PlayerResources;
import kim.biryeong.esekai2.api.player.skill.PlayerSkillBursts;
import kim.biryeong.esekai2.api.player.skill.PlayerSkillCharges;
import kim.biryeong.esekai2.api.player.skill.PlayerSkillCooldowns;
import kim.biryeong.esekai2.api.player.stat.PlayerCombatStats;
import kim.biryeong.esekai2.api.skill.calculation.SkillCalculationLookup;
import kim.biryeong.esekai2.api.skill.definition.graph.SkillPredicateSubject;
import kim.biryeong.esekai2.api.skill.value.SkillValueLookup;
import kim.biryeong.esekai2.api.stat.holder.StatHolder;
import kim.biryeong.esekai2.api.stat.modifier.ConditionalStatModifier;
import kim.biryeong.esekai2.impl.stat.runtime.LivingEntityCombatStatResolver;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Recommended factories for building {@link SkillUseContext} values from live player and target entities.
 *
 * <p>These helpers are the stable live-player entrypoints for callers that want shared player
 * combat stats, registered resource snapshots, and player runtime-state lookups wired in
 * consistently. Raw {@link SkillUseContext} constructors remain available for low-level/manual
 * preparation paths.</p>
 *
 * <p>These helpers are the stable, live-player alternative to raw {@link SkillUseContext}
 * constructors when callers want shared player combat stats, registered resource snapshots, and
 * active skill runtime-state lookups wired consistently.</p>
 *
 * <p>These helpers are the stable player-facing entrypoint for live preparation. They bind shared
 * player combat stats, registered player resources, and player skill runtime-state lookups on top
 * of the raw {@link SkillUseContext} model.</p>
 */
public final class SkillUseContexts {
    private SkillUseContexts() {
    }

    /**
     * Creates a recommended live-player context for a player attacker without a resolved defender.
     *
     * <p>The returned context carries live player combat stats, resource lookup, and runtime-state
     * lookup bindings for the attacker. It still leaves {@link SkillUseContext#activeSkillId()}
     * empty until one concrete skill is later bound with
     * {@link SkillUseContext#withActiveSkill(Identifier, int)}.</p>
     *
     * @param attacker player whose shared runtime combat stats should be used as the attacker source
     * @param hitRoll deterministic hit roll forwarded into the hit context
     * @param criticalStrikeRoll deterministic critical strike roll forwarded into the hit context
     * @return use context backed by live player combat stats
     */
    public static SkillUseContext forPlayer(
            ServerPlayer attacker,
            double hitRoll,
            double criticalStrikeRoll
    ) {
        return forPlayer(
                attacker,
                Optional.empty(),
                List.of(),
                hitRoll,
                criticalStrikeRoll,
                SkillCalculationLookup.empty(),
                SkillValueLookup.empty()
        );
    }

    /**
     * Creates a recommended live-player context for a player attacker and an optional live defender.
     *
     * <p>The returned context carries live player combat stats, resource lookup, and runtime-state
     * lookup bindings for the attacker. It still leaves
     * {@link SkillUseContext#activeSkillId()} empty until one concrete skill is later bound with
     * {@link SkillUseContext#withActiveSkill(Identifier, int)}. Missing defenders preserve the
     * documented absent semantics for target-only helper lookups.</p>
     *
     * @param attacker player whose shared runtime combat stats should be used as the attacker source
     * @param defender optional live defender used to resolve player or monster combat stats
     * @param hitRoll deterministic hit roll forwarded into the hit context
     * @param criticalStrikeRoll deterministic critical strike roll forwarded into the hit context
     * @return use context backed by live attacker and defender combat stats when available
     */
    public static SkillUseContext forPlayer(
            ServerPlayer attacker,
            Optional<? extends Entity> defender,
            double hitRoll,
            double criticalStrikeRoll
    ) {
        return forPlayer(
                attacker,
                defender,
                List.of(),
                hitRoll,
                criticalStrikeRoll,
                SkillCalculationLookup.empty(),
                SkillValueLookup.empty()
        );
    }

    /**
     * Creates a recommended live-player context using live runtime stat sources when available.
     *
     * <p>Callers that already have linked support definitions can append them with
     * {@link SkillUseContext#withLinkedSupports(List)} after the context is created. When no
     * defender is present, {@code PRIMARY_TARGET} resource semantics remain absent instead of
     * falling back to the attacker.</p>
     *
     * @param attacker player whose shared runtime combat stats should be used as the attacker source
     * @param defender optional live defender used to resolve player or monster combat stats
     * @param conditionalModifiers conditional modifiers that may apply at preparation time
     * @param hitRoll deterministic hit roll forwarded into the hit context
     * @param criticalStrikeRoll deterministic critical strike roll forwarded into the hit context
     * @param calculationLookup lookup used to resolve datapack-backed calculation references
     * @param valueLookup lookup used to resolve datapack-backed value references
     * @return use context backed by live attacker and defender combat stats when available
     */
    public static SkillUseContext forPlayer(
            ServerPlayer attacker,
            Optional<? extends Entity> defender,
            List<ConditionalStatModifier> conditionalModifiers,
            double hitRoll,
            double criticalStrikeRoll
    ) {
        return forPlayer(
                attacker,
                defender,
                conditionalModifiers,
                hitRoll,
                criticalStrikeRoll,
                SkillCalculationLookup.empty(),
                SkillValueLookup.empty()
        );
    }

    /**
     * Creates a recommended live-player context for a concrete live defender using empty lookup providers.
     *
     * @param attacker player whose shared runtime combat stats should be used as the attacker source
     * @param defender live defender used to resolve player or monster combat stats
     * @param conditionalModifiers conditional modifiers that may apply at preparation time
     * @param hitRoll deterministic hit roll forwarded into the hit context
     * @param criticalStrikeRoll deterministic critical strike roll forwarded into the hit context
     * @return use context backed by live attacker and defender combat stats when available
     */
    public static SkillUseContext forPlayer(
            ServerPlayer attacker,
            Entity defender,
            List<ConditionalStatModifier> conditionalModifiers,
            double hitRoll,
            double criticalStrikeRoll
    ) {
        Objects.requireNonNull(defender, "defender");
        return forPlayer(
                attacker,
                Optional.of(defender),
                conditionalModifiers,
                hitRoll,
                criticalStrikeRoll,
                SkillCalculationLookup.empty(),
                SkillValueLookup.empty()
        );
    }

    /**
     * Creates a recommended live-player context for a concrete live defender with explicit lookup providers.
     *
     * @param attacker player whose shared runtime combat stats should be used as the attacker source
     * @param defender live defender used to resolve player or monster combat stats
     * @param conditionalModifiers conditional modifiers that may apply at preparation time
     * @param hitRoll deterministic hit roll forwarded into the hit context
     * @param criticalStrikeRoll deterministic critical strike roll forwarded into the hit context
     * @param calculationLookup lookup used to resolve datapack-backed calculation references
     * @param valueLookup lookup used to resolve datapack-backed value references
     * @return use context backed by live attacker and defender combat stats when available
     */
    public static SkillUseContext forPlayer(
            ServerPlayer attacker,
            Entity defender,
            List<ConditionalStatModifier> conditionalModifiers,
            double hitRoll,
            double criticalStrikeRoll,
            SkillCalculationLookup calculationLookup,
            SkillValueLookup valueLookup
    ) {
        Objects.requireNonNull(defender, "defender");
        return forPlayer(
                attacker,
                Optional.of(defender),
                conditionalModifiers,
                hitRoll,
                criticalStrikeRoll,
                calculationLookup,
                valueLookup
        );
    }

    /**
     * Creates a recommended live-player context using live runtime stat sources when available.
     *
     * <p>Callers that already have linked support definitions can append them with
     * {@link SkillUseContext#withLinkedSupports(List)} after the context is created. The returned
     * context intentionally leaves {@link SkillUseContext#activeSkillId()} empty until one concrete
     * prepared skill is bound later in the preparation pipeline.</p>
     *
     * @param attacker player whose shared runtime combat stats should be used as the attacker source
     * @param defender optional live defender used to resolve player or monster combat stats
     * @param conditionalModifiers conditional modifiers that may apply at preparation time
     * @param hitRoll deterministic hit roll forwarded into the hit context
     * @param criticalStrikeRoll deterministic critical strike roll forwarded into the hit context
     * @param calculationLookup lookup used to resolve datapack-backed calculation references
     * @param valueLookup lookup used to resolve datapack-backed value references
     * @return use context backed by live attacker and defender combat stats plus live player resource/runtime-state lookups
     */
    public static SkillUseContext forPlayer(
            ServerPlayer attacker,
            Optional<? extends Entity> defender,
            List<ConditionalStatModifier> conditionalModifiers,
            double hitRoll,
            double criticalStrikeRoll,
            SkillCalculationLookup calculationLookup,
            SkillValueLookup valueLookup
    ) {
        Objects.requireNonNull(attacker, "attacker");
        Objects.requireNonNull(defender, "defender");
        Objects.requireNonNull(conditionalModifiers, "conditionalModifiers");
        Objects.requireNonNull(calculationLookup, "calculationLookup");
        Objects.requireNonNull(valueLookup, "valueLookup");

        StatHolder defenderStats = LivingEntityCombatStatResolver.resolveEntityOrFresh(requireServer(attacker), defender);
        return new SkillUseContext(
                PlayerCombatStats.get(attacker),
                defenderStats,
                conditionalModifiers,
                hitRoll,
                criticalStrikeRoll,
                calculationLookup,
                valueLookup,
                resourceLookup(attacker, defender),
                List.of(),
                playerStateLookup(attacker),
                Optional.empty(),
                0,
                SkillPreparedStateLookup.empty()
        );
    }

    private static SkillResourceLookup resourceLookup(
            ServerPlayer attacker,
            Optional<? extends Entity> defender
    ) {
        return (resource, subject) -> resolveResourceSnapshot(selectResourceEntity(attacker, defender, subject), resource);
    }

    private static SkillPlayerStateLookup playerStateLookup(ServerPlayer attacker) {
        long gameTime = attacker.level().getGameTime();
        return new SkillPlayerStateLookup() {
            @Override
            public long cooldownRemainingTicks(Identifier skillId) {
                return PlayerSkillCooldowns.remainingTicks(attacker, skillId, gameTime);
            }

            @Override
            public int availableCharges(Identifier skillId, int maxCharges) {
                if (maxCharges <= 0) {
                    return 0;
                }
                return PlayerSkillCharges.availableCharges(attacker, skillId, maxCharges, gameTime);
            }

            @Override
            public int burstRemainingCasts(Identifier skillId) {
                return PlayerSkillBursts.remainingCasts(attacker, skillId, gameTime);
            }
        };
    }

    private static Optional<Entity> selectResourceEntity(
            ServerPlayer attacker,
            Optional<? extends Entity> defender,
            SkillPredicateSubject subject
    ) {
        return switch (subject) {
            case SELF -> Optional.of(attacker);
            case TARGET -> defender.map(Entity.class::cast);
            case PRIMARY_TARGET -> defender.map(Entity.class::cast);
        };
    }

    private static Optional<SkillResolvedResource> resolveResourceSnapshot(Optional<Entity> entity, String resource) {
        if (entity.isEmpty() || !(entity.orElseThrow() instanceof ServerPlayer player) || !PlayerResources.supports(resource)) {
            return Optional.empty();
        }
        return Optional.of(new SkillResolvedResource(
                PlayerResources.getAmount(player, resource),
                PlayerResources.maxAmount(player, resource)
        ));
    }

    private static MinecraftServer requireServer(ServerPlayer attacker) {
        MinecraftServer server = attacker.level().getServer();
        if (server == null) {
            throw new IllegalStateException("ServerPlayer is not attached to a running server");
        }
        return server;
    }
}
