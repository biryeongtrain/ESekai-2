package kim.biryeong.esekai2.api.player.stat;

import kim.biryeong.esekai2.api.stat.definition.StatDefinition;
import kim.biryeong.esekai2.api.stat.holder.StatHolder;
import kim.biryeong.esekai2.impl.player.stat.PlayerCombatStatService;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;

/**
 * Public runtime access to a server player's mutable combat stat holder.
 */
public final class PlayerCombatStats {
    private PlayerCombatStats() {
    }

    /**
     * Returns the player's shared runtime combat stat holder, creating it on first access.
     *
     * @param player player whose combat stats should be resolved
     * @return mutable runtime combat stat holder
     */
    public static StatHolder get(ServerPlayer player) {
        return PlayerCombatStatService.get(player);
    }

    /**
     * Returns one resolved combat stat value from the player's runtime holder.
     *
     * @param player player whose combat stat should be resolved
     * @param stat combat stat key to query
     * @return resolved stat value
     */
    public static double resolvedValue(ServerPlayer player, ResourceKey<StatDefinition> stat) {
        return PlayerCombatStatService.get(player).resolvedValue(stat);
    }
}
