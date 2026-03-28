package kim.biryeong.esekai2.api.monster.stat;

import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

/**
 * Public registry keys exposed by the monster stat API.
 */
public final class MonsterRegistries {
    /**
     * Dynamic registry containing {@link MonsterStatDefinition} entries loaded from datapacks.
     */
    public static final ResourceKey<Registry<MonsterStatDefinition>> MONSTER_STAT = ResourceKey.createRegistryKey(
            Identifier.fromNamespaceAndPath("esekai2", "monster_stat")
    );

    private MonsterRegistries() {
    }
}
