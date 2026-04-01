package kim.biryeong.esekai2.impl.gametest.support;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;

/**
 * Shared GameTest player factory used to centralize mock player creation behind one touchpoint.
 *
 * <p>Existing tests still call the helper API directly today. This wrapper gives the suite one
 * replacement point when the deprecated helper needs to be swapped out later.</p>
 */
public final class GameTestPlayers {
    private GameTestPlayers() {
    }

    /**
     * Creates one mock server player in the current GameTest level.
     *
     * @param helper active GameTest helper
     * @return mock server player bound to the test level
     */
    @SuppressWarnings("removal")
    public static ServerPlayer create(GameTestHelper helper) {
        Objects.requireNonNull(helper, "helper");
        return helper.makeMockServerPlayerInLevel();
    }
}
