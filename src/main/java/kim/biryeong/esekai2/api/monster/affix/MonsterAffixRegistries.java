package kim.biryeong.esekai2.api.monster.affix;

import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

/**
 * Public registry keys exposed by the monster affix API.
 */
public final class MonsterAffixRegistries {
    /**
     * Dynamic registry containing {@link MonsterAffixDefinition} entries loaded from datapacks.
     */
    public static final ResourceKey<Registry<MonsterAffixDefinition>> MONSTER_AFFIX = ResourceKey.createRegistryKey(
            Identifier.fromNamespaceAndPath("esekai2", "monster_affix")
    );

    /**
     * Dynamic registry containing {@link MonsterAffixPoolDefinition} entries loaded from datapacks.
     */
    public static final ResourceKey<Registry<MonsterAffixPoolDefinition>> MONSTER_AFFIX_POOL = ResourceKey.createRegistryKey(
            Identifier.fromNamespaceAndPath("esekai2", "monster_affix_pool")
    );

    private MonsterAffixRegistries() {
    }
}
