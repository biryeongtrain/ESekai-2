package kim.biryeong.esekai2.api.player.resource;

import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

/**
 * Public registry keys exposed by the shared player-resource API.
 */
public final class PlayerResourceRegistries {
    public static final ResourceKey<Registry<PlayerResourceDefinition>> PLAYER_RESOURCE = ResourceKey.createRegistryKey(
            Identifier.fromNamespaceAndPath("esekai2", "player_resource")
    );

    private PlayerResourceRegistries() {
    }
}
