package kim.biryeong.esekai2.api.item.affix;

import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

/**
 * Public registry keys exposed by the affix API.
 */
public final class AffixRegistries {
    /**
     * Dynamic registry containing {@link AffixDefinition} entries loaded from datapacks.
     */
    public static final ResourceKey<Registry<AffixDefinition>> AFFIX = ResourceKey.createRegistryKey(
            Identifier.fromNamespaceAndPath("esekai2", "affix")
    );

    private AffixRegistries() {
    }
}
