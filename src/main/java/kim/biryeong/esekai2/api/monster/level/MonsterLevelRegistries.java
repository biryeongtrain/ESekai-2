package kim.biryeong.esekai2.api.monster.level;

import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

/**
 * Public registry keys exposed by the monster level API.
 */
public final class MonsterLevelRegistries {
    /**
     * Dynamic registry containing monster level table definitions loaded from datapacks.
     */
    public static final ResourceKey<Registry<MonsterLevelDefinition>> MONSTER_LEVEL = ResourceKey.createRegistryKey(
            Identifier.fromNamespaceAndPath("esekai2", "monster_level")
    );

    private MonsterLevelRegistries() {
    }
}
