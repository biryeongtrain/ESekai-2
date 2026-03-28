package kim.biryeong.esekai2.impl.item.level;

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

        ItemLevelComponents.ITEM_LEVEL.toString();
        bootstrapped = true;
    }
}
