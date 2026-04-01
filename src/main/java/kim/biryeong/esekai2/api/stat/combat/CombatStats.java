package kim.biryeong.esekai2.api.stat.combat;

import kim.biryeong.esekai2.api.stat.definition.StatDefinition;
import kim.biryeong.esekai2.api.stat.definition.StatRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

/**
 * Canonical combat-related stat keys used by ESekai's core combat systems.
 *
 * <p>These keys are stable API constants. Their actual definitions are loaded from datapacks
 * through the stat dynamic registry.</p>
 */
public final class CombatStats {
    /**
     * Base life pool before later gameplay systems attach entity-specific values.
     */
    public static final ResourceKey<StatDefinition> LIFE = stat("life");

    /**
     * Ailment application threshold used by control ailment duration scaling.
     */
    public static final ResourceKey<StatDefinition> AILMENT_THRESHOLD = stat("ailment_threshold");

    /**
     * Base mana pool before later gameplay systems attach entity-specific values.
     */
    public static final ResourceKey<StatDefinition> MANA = stat("mana");

    /**
     * Flat mana recovery rate in mana per second.
     */
    public static final ResourceKey<StatDefinition> MANA_REGENERATION_PER_SECOND = stat("mana_regeneration_per_second");

    /**
     * Base guard pool used by the generic named-resource runtime fixture.
     */
    public static final ResourceKey<StatDefinition> GUARD = stat("guard");

    /**
     * Flat guard recovery rate in guard per second.
     */
    public static final ResourceKey<StatDefinition> GUARD_REGENERATION_PER_SECOND = stat("guard_regeneration_per_second");

    /**
     * Base energy shield pool before later gameplay systems attach entity-specific values.
     */
    public static final ResourceKey<StatDefinition> ENERGY_SHIELD = stat("energy_shield");

    /**
     * Armour rating used by later physical damage mitigation rules.
     */
    public static final ResourceKey<StatDefinition> ARMOUR = stat("armour");

    /**
     * Evade rating used by later accuracy and evasion calculations.
     */
    public static final ResourceKey<StatDefinition> EVADE = stat("evade");

    /**
     * Accuracy rating used by later attack hit chance calculations.
     */
    public static final ResourceKey<StatDefinition> ACCURACY = stat("accuracy");

    /**
     * Fire resistance before later cap and mitigation rules are applied.
     */
    public static final ResourceKey<StatDefinition> FIRE_RESISTANCE = stat("fire_resistance");

    /**
     * Cold resistance before later cap and mitigation rules are applied.
     */
    public static final ResourceKey<StatDefinition> COLD_RESISTANCE = stat("cold_resistance");

    /**
     * Lightning resistance before later cap and mitigation rules are applied.
     */
    public static final ResourceKey<StatDefinition> LIGHTNING_RESISTANCE = stat("lightning_resistance");

    /**
     * Chaos resistance before later cap and mitigation rules are applied.
     */
    public static final ResourceKey<StatDefinition> CHAOS_RESISTANCE = stat("chaos_resistance");

    /**
     * Maximum fire resistance cap used by later mitigation rules.
     */
    public static final ResourceKey<StatDefinition> MAX_FIRE_RESISTANCE = stat("max_fire_resistance");

    /**
     * Maximum cold resistance cap used by later mitigation rules.
     */
    public static final ResourceKey<StatDefinition> MAX_COLD_RESISTANCE = stat("max_cold_resistance");

    /**
     * Maximum lightning resistance cap used by later mitigation rules.
     */
    public static final ResourceKey<StatDefinition> MAX_LIGHTNING_RESISTANCE = stat("max_lightning_resistance");

    /**
     * Maximum chaos resistance cap used by later mitigation rules.
     */
    public static final ResourceKey<StatDefinition> MAX_CHAOS_RESISTANCE = stat("max_chaos_resistance");

    /**
     * Percentage increased attack critical strike chance applied to attack hit base critical chance.
     */
    public static final ResourceKey<StatDefinition> ATTACK_CRITICAL_STRIKE_CHANCE_INCREASED = stat("attack_critical_strike_chance_increased");

    /**
     * Percentage increased spell critical strike chance applied to spell hit base critical chance.
     */
    public static final ResourceKey<StatDefinition> SPELL_CRITICAL_STRIKE_CHANCE_INCREASED = stat("spell_critical_strike_chance_increased");

    /**
     * Flat bonus added to the base attack critical strike multiplier.
     */
    public static final ResourceKey<StatDefinition> ATTACK_CRITICAL_STRIKE_MULTIPLIER_BONUS = stat("attack_critical_strike_multiplier_bonus");

    /**
     * Flat bonus added to the base spell critical strike multiplier.
     */
    public static final ResourceKey<StatDefinition> SPELL_CRITICAL_STRIKE_MULTIPLIER_BONUS = stat("spell_critical_strike_multiplier_bonus");

    /**
     * Final multiplicative damage taken modifier applied after mitigation.
     */
    public static final ResourceKey<StatDefinition> DAMAGE_TAKEN_MORE = stat("damage_taken_more");

    /**
     * Percentage increased freeze duration applied after the freeze duration factor is resolved.
     */
    public static final ResourceKey<StatDefinition> FREEZE_DURATION_INCREASED = stat("freeze_duration_increased");

    /**
     * Percentage increased stun duration applied after the stun duration factor is resolved.
     */
    public static final ResourceKey<StatDefinition> STUN_DURATION_INCREASED = stat("stun_duration_increased");

    private CombatStats() {
    }

    private static ResourceKey<StatDefinition> stat(String path) {
        return ResourceKey.create(StatRegistries.STAT, Identifier.fromNamespaceAndPath("esekai2", path));
    }
}
