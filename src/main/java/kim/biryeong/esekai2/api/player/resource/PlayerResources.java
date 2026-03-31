package kim.biryeong.esekai2.api.player.resource;

import kim.biryeong.esekai2.impl.player.resource.PlayerResourceService;
import net.minecraft.server.level.ServerPlayer;

/**
 * Public entry points for interacting with ESekai's server-side player mana state.
 */
public final class PlayerResources {
    private PlayerResources() {
    }

    /**
     * Returns the current persistent mana state for the player, creating it from the provided max mana when missing.
     *
     * @param player player to inspect
     * @param maxMana current maximum mana used to initialize or clamp stored state
     * @return current persistent mana state
     */
    public static PlayerResourceState get(ServerPlayer player, double maxMana) {
        return PlayerResourceService.get(player, maxMana);
    }

    /**
     * Returns the current mana value for the player, creating it from the provided max mana when missing.
     *
     * @param player player to inspect
     * @param maxMana current maximum mana used to initialize or clamp stored state
     * @return current mana value
     */
    public static double getMana(ServerPlayer player, double maxMana) {
        return PlayerResourceService.get(player, maxMana).currentMana();
    }

    /**
     * Replaces the player's current mana, clamped to the provided max mana.
     *
     * @param player player to update
     * @param currentMana replacement current mana
     * @param maxMana current maximum mana used to clamp stored state
     * @return updated persistent mana state
     */
    public static PlayerResourceState setMana(ServerPlayer player, double currentMana, double maxMana) {
        return PlayerResourceService.setMana(player, currentMana, maxMana);
    }

    /**
     * Applies a signed mana delta to the player and clamps the resulting state to {@code [0, maxMana]}.
     *
     * @param player player to update
     * @param amount signed mana delta, where positive restores and negative spends
     * @param maxMana current maximum mana used to initialize or clamp stored state
     * @return updated persistent mana state
     */
    public static PlayerResourceState addMana(ServerPlayer player, double amount, double maxMana) {
        return PlayerResourceService.addMana(player, amount, maxMana);
    }

    /**
     * Attempts to spend mana from the player.
     *
     * @param player player to update
     * @param amount amount of mana to spend
     * @param maxMana current maximum mana used to initialize or clamp stored state
     * @return updated state when the spend succeeded
     */
    public static java.util.Optional<PlayerResourceState> spendMana(ServerPlayer player, double amount, double maxMana) {
        return PlayerResourceService.spendMana(player, amount, maxMana);
    }
}
