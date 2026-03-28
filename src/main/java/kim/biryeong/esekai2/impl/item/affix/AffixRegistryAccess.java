package kim.biryeong.esekai2.impl.item.affix;

import kim.biryeong.esekai2.api.item.affix.AffixDefinition;
import kim.biryeong.esekai2.api.item.affix.AffixRegistries;
import net.minecraft.core.Registry;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;

/**
 * Internal helpers for retrieving the affix registry in runtime and GameTest contexts.
 */
public final class AffixRegistryAccess {
    private AffixRegistryAccess() {
    }

    public static Registry<AffixDefinition> affixRegistry(MinecraftServer server) {
        return server.registryAccess().lookupOrThrow(AffixRegistries.AFFIX);
    }

    public static Registry<AffixDefinition> affixRegistry(GameTestHelper helper) {
        return affixRegistry(helper.getLevel().getServer());
    }
}
