package kim.biryeong.esekai2.impl.runtime;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;

import java.util.Optional;

/**
 * Tracks the live dedicated or integrated server instance for runtime-only helpers.
 */
public final class ServerRuntimeAccess {
    private static volatile MinecraftServer currentServer;
    private static volatile Optional<MinecraftServer> currentServerOverride;
    private static boolean bootstrapped;

    private ServerRuntimeAccess() {
    }

    public static void bootstrap() {
        if (bootstrapped) {
            return;
        }
        bootstrapped = true;
        ServerLifecycleEvents.SERVER_STARTING.register(server -> currentServer = server);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            if (currentServer == server) {
                currentServer = null;
            }
        });
    }

    public static Optional<MinecraftServer> currentServer() {
        bootstrap();
        Optional<MinecraftServer> override = currentServerOverride;
        if (override != null) {
            return override;
        }
        return Optional.ofNullable(currentServer);
    }

    /**
     * Overrides the live server snapshot used by runtime helpers during tests.
     *
     * @param server replacement server, or {@code null} to force an absent live server
     */
    public static void setCurrentForTesting(MinecraftServer server) {
        currentServerOverride = Optional.ofNullable(server);
    }

    /**
     * Clears the test-only server override and resumes normal live server tracking.
     */
    public static void clearCurrentForTesting() {
        currentServerOverride = null;
    }
}
