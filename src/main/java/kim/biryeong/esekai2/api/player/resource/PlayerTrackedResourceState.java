package kim.biryeong.esekai2.api.player.resource;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

/**
 * Persistent server-side snapshot for one named player resource.
 *
 * @param currentAmount current amount stored for the resource
 */
public record PlayerTrackedResourceState(double currentAmount) {
    /**
     * Validated codec used to decode generic tracked resource state from saved data and fixtures.
     */
    public static final Codec<PlayerTrackedResourceState> CODEC = Codec.DOUBLE.xmap(
            PlayerTrackedResourceState::new,
            PlayerTrackedResourceState::currentAmount
    ).validate(PlayerTrackedResourceState::validate);

    private static DataResult<PlayerTrackedResourceState> validate(PlayerTrackedResourceState state) {
        if (!Double.isFinite(state.currentAmount()) || state.currentAmount() < 0.0) {
            return DataResult.error(() -> "currentAmount must be finite and >= 0");
        }
        return DataResult.success(state);
    }
}
