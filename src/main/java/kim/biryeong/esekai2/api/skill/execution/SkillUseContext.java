package kim.biryeong.esekai2.api.skill.execution;

import kim.biryeong.esekai2.api.skill.calculation.SkillCalculationLookup;
import kim.biryeong.esekai2.api.skill.support.SkillSupportDefinition;
import kim.biryeong.esekai2.api.skill.value.SkillValueLookup;
import kim.biryeong.esekai2.api.stat.holder.StatHolder;
import kim.biryeong.esekai2.api.stat.modifier.ConditionalStatModifier;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Runtime context used to prepare one skill hit from a static skill definition.
 *
 * <p>This type is the low-level/raw preparation surface. Its constructors do not infer live player
 * resource snapshots, cooldown or charge state, or active-skill identity on behalf of callers.
 * Callers preparing skills from live players should prefer {@link SkillUseContexts} and only use
 * raw constructors when they want explicit control over stat holders and lookup seams.</p>
 *
 * <p>This record is the low-level/raw preparation surface. Direct constructor callers are
 * responsible for binding any reusable lookups, linked supports, active-skill identity, or live
 * player runtime state they expect later expressions and predicates to observe. When these
 * optional bindings are left empty, dependent expressions resolve through their documented safe
 * defaults instead of implicitly consulting live player helpers.</p>
 *
 * <p>This record is the low-level preparation surface. Raw constructors do not automatically bind
 * live player resource snapshots, cooldown/charge/burst state, or an active skill identity. Call
 * {@link SkillUseContexts#forPlayer(net.minecraft.server.level.ServerPlayer, double, double)} when
 * callers want the recommended live-player contract, and use the {@code with...} copy helpers when
 * additional lookup or owner-path data must be layered on top of one existing context.</p>
 *
 * @param attackerStats attacker stat holder used for runtime skill stat and hit calculations
 * @param defenderStats defender stat holder passed through to later mitigation layers
 * @param conditionalModifiers conditional stat modifiers that may apply to this skill at runtime
 * @param hitRoll deterministic hit roll forwarded into the hit context
 * @param criticalStrikeRoll deterministic critical strike roll forwarded into the hit context
 * @param calculationLookup optional lookup used to resolve datapack-backed calculation references
 * @param valueLookup optional lookup used to resolve datapack-backed value references
 * @param resourceLookup optional lookup used to resolve subject resource snapshots
 * @param linkedSupports support definitions linked to the active skill for this preparation
 * @param playerStateLookup optional lookup used to resolve active player-skill runtime state
 * @param activeSkillId active skill identifier when the context has been bound to one prepared skill
 * @param activeSkillMaxCharges configured maximum charges for the active skill
 * @param preparedStateLookup optional lookup used to resolve prepared runtime/config values
 */
public record SkillUseContext(
        StatHolder attackerStats,
        StatHolder defenderStats,
        List<ConditionalStatModifier> conditionalModifiers,
        double hitRoll,
        double criticalStrikeRoll,
        SkillCalculationLookup calculationLookup,
        SkillValueLookup valueLookup,
        SkillResourceLookup resourceLookup,
        List<SkillSupportDefinition> linkedSupports,
        SkillPlayerStateLookup playerStateLookup,
        Optional<Identifier> activeSkillId,
        int activeSkillMaxCharges,
        SkillPreparedStateLookup preparedStateLookup
) {
    public SkillUseContext {
        Objects.requireNonNull(attackerStats, "attackerStats");
        Objects.requireNonNull(defenderStats, "defenderStats");
        Objects.requireNonNull(conditionalModifiers, "conditionalModifiers");
        Objects.requireNonNull(calculationLookup, "calculationLookup");
        Objects.requireNonNull(valueLookup, "valueLookup");
        Objects.requireNonNull(resourceLookup, "resourceLookup");
        Objects.requireNonNull(linkedSupports, "linkedSupports");
        Objects.requireNonNull(playerStateLookup, "playerStateLookup");
        Objects.requireNonNull(activeSkillId, "activeSkillId");
        Objects.requireNonNull(preparedStateLookup, "preparedStateLookup");

        conditionalModifiers = List.copyOf(conditionalModifiers);
        for (ConditionalStatModifier modifier : conditionalModifiers) {
            Objects.requireNonNull(modifier, "conditionalModifier entry");
        }

        linkedSupports = List.copyOf(linkedSupports);
        for (SkillSupportDefinition support : linkedSupports) {
            Objects.requireNonNull(support, "linkedSupports entry");
        }
        activeSkillId = activeSkillId.map(identifier -> Objects.requireNonNull(identifier, "activeSkillId entry"));

        if (!Double.isFinite(hitRoll) || hitRoll < 0.0 || hitRoll >= 1.0) {
            throw new IllegalArgumentException("hitRoll must be a finite number in the range [0, 1)");
        }

        if (!Double.isFinite(criticalStrikeRoll) || criticalStrikeRoll < 0.0 || criticalStrikeRoll >= 1.0) {
            throw new IllegalArgumentException("criticalStrikeRoll must be a finite number in the range [0, 1)");
        }
        if (activeSkillMaxCharges < 0) {
            throw new IllegalArgumentException("activeSkillMaxCharges must be >= 0");
        }
    }

    /**
     * Compatibility constructor that defaults prepared-state lookup bindings to the empty lookup.
     *
     * @param attackerStats attacker stat holder used for runtime skill stat and hit calculations
     * @param defenderStats defender stat holder passed through to later mitigation layers
     * @param conditionalModifiers conditional stat modifiers that may apply to this skill at runtime
     * @param hitRoll deterministic hit roll forwarded into the hit context
     * @param criticalStrikeRoll deterministic critical strike roll forwarded into the hit context
     * @param calculationLookup optional lookup used to resolve datapack-backed calculation references
     * @param valueLookup optional lookup used to resolve datapack-backed value references
     * @param resourceLookup optional lookup used to resolve subject resource snapshots
     * @param linkedSupports support definitions linked to the active skill for this preparation
     * @param playerStateLookup optional lookup used to resolve active player-skill runtime state
     * @param activeSkillId active skill identifier when the context has been bound to one prepared skill
     * @param activeSkillMaxCharges configured maximum charges for the active skill
     */
    public SkillUseContext(
            StatHolder attackerStats,
            StatHolder defenderStats,
            List<ConditionalStatModifier> conditionalModifiers,
            double hitRoll,
            double criticalStrikeRoll,
            SkillCalculationLookup calculationLookup,
            SkillValueLookup valueLookup,
            SkillResourceLookup resourceLookup,
            List<SkillSupportDefinition> linkedSupports,
            SkillPlayerStateLookup playerStateLookup,
            Optional<Identifier> activeSkillId,
            int activeSkillMaxCharges
    ) {
        this(
                attackerStats,
                defenderStats,
                conditionalModifiers,
                hitRoll,
                criticalStrikeRoll,
                calculationLookup,
                valueLookup,
                resourceLookup,
                linkedSupports,
                playerStateLookup,
                activeSkillId,
                activeSkillMaxCharges,
                SkillPreparedStateLookup.empty()
        );
    }

    /**
     * Creates one raw use context without any datapack-backed calculation lookup.
     *
     * <p>This constructor does not bind resource, support, or player-state lookups. Resource-current
     * and runtime-state expressions therefore resolve through their safe absent/default semantics
     * until callers explicitly bind those seams.</p>
     *
     * <p>This convenience constructor leaves value/resource/runtime-state lookups, linked
     * supports, and active-skill identity empty. It is suited to raw tests and controlled callers,
     * not as a substitute for {@link SkillUseContexts} live-player factories.</p>
     *
     * <p>This is a raw constructor. Value, resource, and player-state lookups remain empty until
     * callers explicitly bind them later.</p>
     *
     * @param attackerStats attacker stat holder used for runtime skill stat and hit calculations
     * @param defenderStats defender stat holder passed through to later mitigation layers
     * @param conditionalModifiers conditional stat modifiers that may apply to this skill at runtime
     * @param hitRoll deterministic hit roll forwarded into the hit context
     * @param criticalStrikeRoll deterministic critical strike roll forwarded into the hit context
     */
    public SkillUseContext(
            StatHolder attackerStats,
            StatHolder defenderStats,
            List<ConditionalStatModifier> conditionalModifiers,
            double hitRoll,
            double criticalStrikeRoll
    ) {
        this(
                attackerStats,
                defenderStats,
                conditionalModifiers,
                hitRoll,
                criticalStrikeRoll,
                SkillCalculationLookup.empty(),
                SkillValueLookup.empty(),
                SkillResourceLookup.empty(),
                List.of(),
                SkillPlayerStateLookup.empty(),
                Optional.empty(),
                0,
                SkillPreparedStateLookup.empty()
        );
    }

    /**
     * Creates one raw use context with a calculation lookup but without linked supports.
     *
     * <p>This convenience constructor still leaves value/resource/runtime-state lookups, linked
     * supports, and active-skill identity empty.</p>
     *
     * <p>This is still a raw constructor. Resource snapshots, player runtime state, and active
     * skill identity remain absent until callers bind them explicitly.</p>
     *
     * @param attackerStats attacker stat holder used for runtime skill stat and hit calculations
     * @param defenderStats defender stat holder passed through to later mitigation layers
     * @param conditionalModifiers conditional stat modifiers that may apply to this skill at runtime
     * @param hitRoll deterministic hit roll forwarded into the hit context
     * @param criticalStrikeRoll deterministic critical strike roll forwarded into the hit context
     * @param calculationLookup lookup used to resolve datapack-backed calculation references
     */
    public SkillUseContext(
            StatHolder attackerStats,
            StatHolder defenderStats,
            List<ConditionalStatModifier> conditionalModifiers,
            double hitRoll,
            double criticalStrikeRoll,
            SkillCalculationLookup calculationLookup
    ) {
        this(attackerStats, defenderStats, conditionalModifiers, hitRoll, criticalStrikeRoll, calculationLookup, SkillValueLookup.empty(), SkillResourceLookup.empty(), List.of(), SkillPlayerStateLookup.empty(), Optional.empty(), 0, SkillPreparedStateLookup.empty());
    }

    /**
     * Creates one raw use context with a calculation lookup and linked supports but without a value lookup.
     *
     * <p>This convenience constructor still leaves resource/runtime-state lookups and active-skill
     * identity empty.</p>
     *
     * @param attackerStats attacker stat holder used for runtime skill stat and hit calculations
     * @param defenderStats defender stat holder passed through to later mitigation layers
     * @param conditionalModifiers conditional stat modifiers that may apply to this skill at runtime
     * @param hitRoll deterministic hit roll forwarded into the hit context
     * @param criticalStrikeRoll deterministic critical strike roll forwarded into the hit context
     * @param calculationLookup lookup used to resolve datapack-backed calculation references
     * @param linkedSupports support definitions linked to the active skill for this preparation
     */
    public SkillUseContext(
            StatHolder attackerStats,
            StatHolder defenderStats,
            List<ConditionalStatModifier> conditionalModifiers,
            double hitRoll,
            double criticalStrikeRoll,
            SkillCalculationLookup calculationLookup,
            List<SkillSupportDefinition> linkedSupports
    ) {
        this(attackerStats, defenderStats, conditionalModifiers, hitRoll, criticalStrikeRoll, calculationLookup, SkillValueLookup.empty(), SkillResourceLookup.empty(), linkedSupports, SkillPlayerStateLookup.empty(), Optional.empty(), 0, SkillPreparedStateLookup.empty());
    }

    /**
     * Creates one raw use context with calculation and value lookups but without linked supports.
     *
     * <p>This convenience constructor still leaves resource/runtime-state lookups, linked
     * supports, and active-skill identity empty.</p>
     *
     * <p>Resource snapshots, player runtime state, and active skill identity remain absent until
     * callers bind them explicitly.</p>
     *
     * @param attackerStats attacker stat holder used for runtime skill stat and hit calculations
     * @param defenderStats defender stat holder passed through to later mitigation layers
     * @param conditionalModifiers conditional stat modifiers that may apply to this skill at runtime
     * @param hitRoll deterministic hit roll forwarded into the hit context
     * @param criticalStrikeRoll deterministic critical strike roll forwarded into the hit context
     * @param calculationLookup lookup used to resolve datapack-backed calculation references
     * @param valueLookup lookup used to resolve datapack-backed value references
     */
    public SkillUseContext(
            StatHolder attackerStats,
            StatHolder defenderStats,
            List<ConditionalStatModifier> conditionalModifiers,
            double hitRoll,
            double criticalStrikeRoll,
            SkillCalculationLookup calculationLookup,
            SkillValueLookup valueLookup
    ) {
        this(attackerStats, defenderStats, conditionalModifiers, hitRoll, criticalStrikeRoll, calculationLookup, valueLookup, SkillResourceLookup.empty(), List.of(), SkillPlayerStateLookup.empty(), Optional.empty(), 0, SkillPreparedStateLookup.empty());
    }

    /**
     * Creates one raw use context with calculation and value lookups plus linked supports.
     *
     * <p>This convenience constructor still leaves resource/runtime-state lookups and active-skill
     * identity empty.</p>
     *
     * @param attackerStats attacker stat holder used for runtime skill stat and hit calculations
     * @param defenderStats defender stat holder passed through to later mitigation layers
     * @param conditionalModifiers conditional stat modifiers that may apply to this skill at runtime
     * @param hitRoll deterministic hit roll forwarded into the hit context
     * @param criticalStrikeRoll deterministic critical strike roll forwarded into the hit context
     * @param calculationLookup lookup used to resolve datapack-backed calculation references
     * @param valueLookup lookup used to resolve datapack-backed value references
     * @param linkedSupports support definitions linked to the active skill for this preparation
     */
    public SkillUseContext(
            StatHolder attackerStats,
            StatHolder defenderStats,
            List<ConditionalStatModifier> conditionalModifiers,
            double hitRoll,
            double criticalStrikeRoll,
            SkillCalculationLookup calculationLookup,
            SkillValueLookup valueLookup,
            List<SkillSupportDefinition> linkedSupports
    ) {
        this(attackerStats, defenderStats, conditionalModifiers, hitRoll, criticalStrikeRoll, calculationLookup, valueLookup, SkillResourceLookup.empty(), linkedSupports, SkillPlayerStateLookup.empty(), Optional.empty(), 0, SkillPreparedStateLookup.empty());
    }

    /**
     * Creates one raw use context with calculation, value, and resource lookups plus linked supports.
     *
     * <p>This convenience constructor still leaves player-state lookup and active-skill identity
     * empty. Callers that want live-player combat stats, resource snapshots, and skill runtime
     * state without manual binding should still prefer {@link SkillUseContexts}.</p>
     *
     * @param attackerStats attacker stat holder used for runtime skill stat and hit calculations
     * @param defenderStats defender stat holder passed through to later mitigation layers
     * @param conditionalModifiers conditional stat modifiers that may apply to this skill at runtime
     * @param hitRoll deterministic hit roll forwarded into the hit context
     * @param criticalStrikeRoll deterministic critical strike roll forwarded into the hit context
     * @param calculationLookup lookup used to resolve datapack-backed calculation references
     * @param valueLookup lookup used to resolve datapack-backed value references
     * @param resourceLookup lookup used to resolve subject resource snapshots
     * @param linkedSupports support definitions linked to the active skill for this preparation
     */
    public SkillUseContext(
            StatHolder attackerStats,
            StatHolder defenderStats,
            List<ConditionalStatModifier> conditionalModifiers,
            double hitRoll,
            double criticalStrikeRoll,
            SkillCalculationLookup calculationLookup,
            SkillValueLookup valueLookup,
            SkillResourceLookup resourceLookup,
            List<SkillSupportDefinition> linkedSupports
    ) {
        this(
                attackerStats,
                defenderStats,
                conditionalModifiers,
                hitRoll,
                criticalStrikeRoll,
                calculationLookup,
                valueLookup,
                resourceLookup,
                linkedSupports,
                SkillPlayerStateLookup.empty(),
                Optional.empty(),
                0,
                SkillPreparedStateLookup.empty()
        );
    }

    /**
     * Returns a copy of this context with additional conditional modifiers appended.
     *
     * <p>All other lookups, support bindings, and active-skill state are preserved.</p>
     *
     * @param additionalModifiers modifiers to append
     * @return copied context with appended modifiers
     */
    public SkillUseContext withAdditionalConditionalModifiers(List<ConditionalStatModifier> additionalModifiers) {
        Objects.requireNonNull(additionalModifiers, "additionalModifiers");
        if (additionalModifiers.isEmpty()) {
            return this;
        }

        List<ConditionalStatModifier> merged = new ArrayList<>(conditionalModifiers);
        merged.addAll(additionalModifiers);
        return new SkillUseContext(
                attackerStats,
                defenderStats,
                List.copyOf(merged),
                hitRoll,
                criticalStrikeRoll,
                calculationLookup,
                valueLookup,
                resourceLookup,
                linkedSupports,
                playerStateLookup,
                activeSkillId,
                activeSkillMaxCharges,
                preparedStateLookup
        );
    }

    /**
     * Returns a copy of this context with linked supports replaced.
     *
     * <p>All existing stat holders, lookups, and active-skill binding are preserved.</p>
     *
     * <p>All other bindings, including lookups and active-skill identity, are preserved.</p>
     *
     * <p>All other lookups and active-skill bindings are preserved.</p>
     *
     * @param supports support definitions that should be linked for preparation
     * @return copied context with replaced support list
     */
    public SkillUseContext withLinkedSupports(List<SkillSupportDefinition> supports) {
        Objects.requireNonNull(supports, "supports");
        return new SkillUseContext(
                attackerStats,
                defenderStats,
                conditionalModifiers,
                hitRoll,
                criticalStrikeRoll,
                calculationLookup,
                valueLookup,
                resourceLookup,
                supports,
                playerStateLookup,
                activeSkillId,
                activeSkillMaxCharges,
                preparedStateLookup
        );
    }

    /**
     * Returns a copy of this context with the attacker stat holder replaced.
     *
     * <p>All existing lookups, linked supports, and active-skill binding are preserved.</p>
     *
     * <p>All other bindings, including lookups, linked supports, and active-skill identity, are
     * preserved.</p>
     *
     * <p>All lookups, support bindings, and active-skill state are preserved.</p>
     *
     * @param attacker replacement attacker stat holder
     * @return copied context with replaced attacker stats
     */
    public SkillUseContext withAttackerStats(StatHolder attacker) {
        Objects.requireNonNull(attacker, "attacker");
        return new SkillUseContext(
                attacker,
                defenderStats,
                conditionalModifiers,
                hitRoll,
                criticalStrikeRoll,
                calculationLookup,
                valueLookup,
                resourceLookup,
                linkedSupports,
                playerStateLookup,
                activeSkillId,
                activeSkillMaxCharges,
                preparedStateLookup
        );
    }

    /**
     * Returns a copy of this context with the value lookup replaced.
     *
     * <p>All existing stat holders, resource lookup, player-state lookup, and active-skill binding are preserved.</p>
     *
     * <p>All other bindings are preserved.</p>
     *
     * <p>All other lookups, support bindings, and active-skill state are preserved.</p>
     *
     * @param lookup lookup used to resolve reusable value expressions
     * @return copied context with replaced value lookup
     */
    public SkillUseContext withValueLookup(SkillValueLookup lookup) {
        Objects.requireNonNull(lookup, "lookup");
        return new SkillUseContext(
                attackerStats,
                defenderStats,
                conditionalModifiers,
                hitRoll,
                criticalStrikeRoll,
                calculationLookup,
                lookup,
                resourceLookup,
                linkedSupports,
                playerStateLookup,
                activeSkillId,
                activeSkillMaxCharges
        );
    }

    /**
     * Returns a copy of this context with the resource lookup replaced.
     *
     * <p>Current-resource expressions resolve to {@code 0} until a lookup or helper-built live
     * context provides one. Replacing the lookup does not alter active-skill binding or player-state
     * semantics.</p>
     *
     * <p>All other bindings are preserved.</p>
     *
     * <p>All other lookups, support bindings, and active-skill state are preserved.</p>
     *
     * @param lookup lookup used to resolve subject resource snapshots
     * @return copied context with replaced resource lookup
     */
    public SkillUseContext withResourceLookup(SkillResourceLookup lookup) {
        Objects.requireNonNull(lookup, "lookup");
        return new SkillUseContext(
                attackerStats,
                defenderStats,
                conditionalModifiers,
                hitRoll,
                criticalStrikeRoll,
                calculationLookup,
                valueLookup,
                lookup,
                linkedSupports,
                playerStateLookup,
                activeSkillId,
                activeSkillMaxCharges,
                preparedStateLookup
        );
    }

    /**
     * Returns a copy of this context with the player runtime-state lookup replaced.
     *
     * <p>Cooldown, charge, and burst expressions still resolve to {@code 0} until the context is
     * also bound to one active skill identity via {@link #withActiveSkill(Identifier, int)}.</p>
     *
     * <p>All other bindings are preserved. Runtime-state expressions still resolve through safe
     * defaults until an active-skill identity is also bound.</p>
     *
     * <p>All other lookups, support bindings, and active-skill state are preserved.</p>
     *
     * @param lookup lookup used to resolve active player-skill runtime state
     * @return copied context with replaced player-state lookup
     */
    public SkillUseContext withPlayerStateLookup(SkillPlayerStateLookup lookup) {
        Objects.requireNonNull(lookup, "lookup");
        return new SkillUseContext(
                attackerStats,
                defenderStats,
                conditionalModifiers,
                hitRoll,
                criticalStrikeRoll,
                calculationLookup,
                valueLookup,
                resourceLookup,
                linkedSupports,
                lookup,
                activeSkillId,
                activeSkillMaxCharges,
                preparedStateLookup
        );
    }

    /**
     * Returns a copy of this context bound to one active skill identity for state lookups.
     *
     * <p>Binding an active skill id alone does not supply live player runtime state. Raw callers
     * that want cooldown, charge, or burst expressions to resolve non-zero values must also provide
     * a {@linkplain #withPlayerStateLookup(SkillPlayerStateLookup) player-state lookup} or use a
     * helper-built live player context.</p>
     *
     * <p>All other bindings are preserved. Runtime-state expressions still resolve through safe
     * defaults when the player-state lookup itself remains empty.</p>
     *
     * <p>Binding an active skill id does not implicitly provide live player state. Callers still
     * need a non-empty {@link #playerStateLookup()} when runtime-state expressions should resolve
     * meaningful cooldown, charge, or burst values.</p>
     *
     * @param skillId active skill identifier when known
     * @param maxCharges configured maximum charges for the active skill
     * @return copied context bound to one prepared skill identity
     */
    public SkillUseContext withActiveSkill(Identifier skillId, int maxCharges) {
        if (maxCharges < 0) {
            throw new IllegalArgumentException("maxCharges must be >= 0");
        }
        return new SkillUseContext(
                attackerStats,
                defenderStats,
                conditionalModifiers,
                hitRoll,
                criticalStrikeRoll,
                calculationLookup,
                valueLookup,
                resourceLookup,
                linkedSupports,
                playerStateLookup,
                Optional.ofNullable(skillId),
                maxCharges,
                preparedStateLookup
        );
    }

    /**
     * Returns a copy of this context with the prepared-state lookup replaced.
     *
     * <p>All other lookups, support bindings, and active-skill state are preserved.</p>
     *
     * @param lookup lookup used to resolve prepared runtime/config values
     * @return copied context with replaced prepared-state lookup
     */
    public SkillUseContext withPreparedStateLookup(SkillPreparedStateLookup lookup) {
        Objects.requireNonNull(lookup, "lookup");
        return new SkillUseContext(
                attackerStats,
                defenderStats,
                conditionalModifiers,
                hitRoll,
                criticalStrikeRoll,
                calculationLookup,
                valueLookup,
                resourceLookup,
                linkedSupports,
                playerStateLookup,
                activeSkillId,
                activeSkillMaxCharges,
                lookup
        );
    }
}
