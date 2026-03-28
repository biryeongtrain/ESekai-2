package kim.biryeong.esekai2.impl.stat.registry;

import kim.biryeong.esekai2.api.stat.definition.StatDefinition;
import kim.biryeong.esekai2.api.stat.definition.StatRegistries;
import net.minecraft.core.Registry;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;

public final class StatRegistryAccess {
    private StatRegistryAccess() {
    }

    public static Registry<StatDefinition> statRegistry(MinecraftServer server) {
        return server.registryAccess().lookupOrThrow(StatRegistries.STAT);
    }

    public static Registry<StatDefinition> statRegistry(GameTestHelper helper) {
        return statRegistry(helper.getLevel().getServer());
    }
}
