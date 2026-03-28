package kim.biryeong.esekai2.api.stat.holder;

import kim.biryeong.esekai2.api.stat.definition.StatDefinition;
import kim.biryeong.esekai2.api.stat.modifier.StatModifier;
import kim.biryeong.esekai2.api.stat.value.StatInstance;
import net.minecraft.resources.ResourceKey;

import java.util.Map;

/**
 * Mutable runtime container that stores and resolves multiple {@link StatInstance} values.
 *
 * <p>Implementations are responsible for creating missing stat instances on demand from the
 * underlying stat definition source.</p>
 */
public interface StatHolder {
    /**
     * Returns the stat instance for the provided stat key, creating it lazily if needed.
     *
     * @param stat stat key to resolve
     * @return materialized stat instance for the key
     */
    StatInstance stat(ResourceKey<StatDefinition> stat);

    /**
     * Returns the resolved numeric value of the provided stat.
     *
     * @param stat stat key to resolve
     * @return current resolved numeric value
     */
    double resolvedValue(ResourceKey<StatDefinition> stat);

    /**
     * Replaces the base value of the provided stat instance.
     *
     * @param stat stat key to update
     * @param baseValue replacement base value
     */
    void setBaseValue(ResourceKey<StatDefinition> stat, double baseValue);

    /**
     * Appends a modifier to the stat targeted by the modifier itself.
     *
     * @param modifier modifier to append
     */
    void addModifier(StatModifier modifier);

    /**
     * Removes the first modifier equal to the provided modifier from the targeted stat.
     *
     * @param modifier modifier to remove
     * @return {@code true} if a matching modifier was removed
     */
    boolean removeModifier(StatModifier modifier);

    /**
     * Returns an immutable snapshot of the currently materialized stat instances.
     *
     * @return immutable map of materialized stat instances keyed by stat key
     */
    Map<ResourceKey<StatDefinition>, StatInstance> snapshot();
}
