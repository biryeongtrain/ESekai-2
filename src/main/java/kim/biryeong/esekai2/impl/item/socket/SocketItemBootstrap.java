package kim.biryeong.esekai2.impl.item.socket;

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
        SocketItemComponents.SOCKETED_ITEM_STATE.toString();
        bootstrapped = true;
    }
}
