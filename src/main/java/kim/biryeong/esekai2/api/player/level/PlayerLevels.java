package kim.biryeong.esekai2.api.player.level;

import kim.biryeong.esekai2.impl.player.level.PlayerLevelService;
import net.minecraft.server.level.ServerPlayer;

/**
 * Public entry points for interacting with ESekai's server-side player level system.
 */
public final class PlayerLevels {
    private PlayerLevels() {
    }

    /**
     * Returns the current level state recorded for the player.
     *
     * @param player player to inspect
     * @return current persistent player level state
     */
    public static PlayerLevelState get(ServerPlayer player) {
        return PlayerLevelService.get(player);
    }

    /**
     * Replaces the player's level and resets in-level experience to zero.
     *
     * @param player player to update
     * @param level replacement level
     * @return updated player level state
     */
    public static PlayerLevelState setLevel(ServerPlayer player, int level) {
        return PlayerLevelService.setLevel(player, level);
    }

    /**
     * Replaces the player's total experience and re-resolves level progression from it.
     *
     * @param player player to update
     * @param totalExperience replacement total experience
     * @return updated player level state
     */
    public static PlayerLevelState setExperience(ServerPlayer player, long totalExperience) {
        return PlayerLevelService.setExperience(player, totalExperience);
    }

    /**
     * Adds experience to the player and resolves any resulting level ups.
     *
     * @param player player to update
     * @param experience experience to add
     * @return updated player level state
     */
    public static PlayerLevelState addExperience(ServerPlayer player, long experience) {
        return PlayerLevelService.addExperience(player, experience);
    }

    /**
     * Returns the remaining experience needed for the player to reach the next level.
     *
     * @param player player to inspect
     * @return remaining experience, or {@code 0} at level 100
     */
    public static long experienceToNextLevel(ServerPlayer player) {
        return PlayerLevelService.experienceToNextLevel(player);
    }
}
