package kim.biryeong.esekai2.api.stat.holder;

import kim.biryeong.esekai2.api.stat.definition.StatDefinition;
import kim.biryeong.esekai2.impl.stat.holder.SimpleStatHolder;
import net.minecraft.core.Registry;

/**
 * Factory entry points for creating {@link StatHolder} instances.
 */
public final class StatHolders {
    private StatHolders() {
    }

    /**
     * Creates a mutable stat holder backed by the provided stat definition registry.
     *
     * @param registry stat definition registry used to lazily materialize missing stat instances
     * @return new mutable stat holder
     */
    public static StatHolder create(Registry<StatDefinition> registry) {
        return new SimpleStatHolder(registry);
    }
}
