package kim.biryeong.esekai2.api.player.resource;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

/**
 * Persistent server-side player resource snapshot.
 *
 * @param currentMana current mana stored for the player
 */
public record PlayerResourceState(double currentMana) {
    /**
     * Validated codec used to decode player resource state from saved data and test fixtures.
     */
    public static final Codec<PlayerResourceState> CODEC = Codec.DOUBLE.xmap(PlayerResourceState::new, PlayerResourceState::currentMana)
            .validate(PlayerResourceState::validate);

    private static DataResult<PlayerResourceState> validate(PlayerResourceState state) {
        if (!Double.isFinite(state.currentMana()) || state.currentMana() < 0.0) {
            return DataResult.error(() -> "currentMana must be finite and >= 0");
        }
        return DataResult.success(state);
    }
}
