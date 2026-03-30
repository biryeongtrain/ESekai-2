package kim.biryeong.esekai2.impl.item.level;

import eu.pb4.polymer.core.api.other.PolymerComponent;
import kim.biryeong.esekai2.impl.item.socket.SocketItemBootstrap;

/**
 * Forces eager initialization of item level data components during mod bootstrap.
 */
public final class ItemLevelBootstrap {
    private static boolean bootstrapped;

    private ItemLevelBootstrap() {
    }

    public static void bootstrap() {
        if (bootstrapped) {
            return;
        }

        PolymerComponent.registerDataComponent(ItemLevelComponents.ITEM_LEVEL);
        SocketItemBootstrap.bootstrap();
        bootstrapped = true;
    }
}
