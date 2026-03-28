package kim.biryeong.esekai2.api.stat.definition;

import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

/**
 * Public registry keys exposed by the stat API.
 */
public final class StatRegistries {
    /**
     * Dynamic registry containing {@link StatDefinition} entries loaded from datapacks.
     */
    public static final ResourceKey<Registry<StatDefinition>> STAT = ResourceKey.createRegistryKey(
            Identifier.fromNamespaceAndPath("esekai2", "stat")
    );

    private StatRegistries() {
    }
}
