package kim.biryeong.esekai2.impl.item.socket;

import eu.pb4.polymer.core.api.other.PolymerComponent;

/**
 * Forces eager initialization of socket data components.
 */
public final class SocketItemBootstrap {
    private static boolean bootstrapped;

    private SocketItemBootstrap() {
    }

    public static void bootstrap() {
        if (bootstrapped) {
            return;
        }
        PolymerComponent.registerDataComponent(SocketItemComponents.SOCKETED_ITEM_STATE);
        bootstrapped = true;
    }
}
