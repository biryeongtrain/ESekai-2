package kim.biryeong.esekai2.api.level;

import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

/**
 * Public registry keys exposed by the shared level API.
 */
public final class LevelRegistries {
    public static final ResourceKey<Registry<LevelProgressionDefinition>> PLAYER_PROGRESSION = ResourceKey.createRegistryKey(
            Identifier.fromNamespaceAndPath("esekai2", "player_progression")
    );

    private LevelRegistries() {
    }
}
