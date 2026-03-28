package kim.biryeong.esekai2.impl.monster.affix;

import kim.biryeong.esekai2.api.monster.affix.MonsterAffixDefinition;
import kim.biryeong.esekai2.api.monster.affix.MonsterAffixPoolDefinition;
import kim.biryeong.esekai2.api.monster.affix.MonsterAffixRegistries;
import net.minecraft.core.Registry;
import net.minecraft.server.MinecraftServer;

/**
 * Central lookup helpers for the monster affix dynamic registries.
 */
public final class MonsterAffixRegistryAccess {
    private MonsterAffixRegistryAccess() {
    }

    public static Registry<MonsterAffixDefinition> affixRegistry(MinecraftServer server) {
        return server.registryAccess().lookupOrThrow(MonsterAffixRegistries.MONSTER_AFFIX);
    }

    public static Registry<MonsterAffixPoolDefinition> poolRegistry(MinecraftServer server) {
        return server.registryAccess().lookupOrThrow(MonsterAffixRegistries.MONSTER_AFFIX_POOL);
    }
}
