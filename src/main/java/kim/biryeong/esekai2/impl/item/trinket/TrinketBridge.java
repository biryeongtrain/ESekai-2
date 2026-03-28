package kim.biryeong.esekai2.impl.item.trinket;

/**
 * Internal seam for future Trinkets-backed item integration.
 *
 * <p>This package intentionally stays implementation-only until the item affix system is ready
 * to push rolled modifiers into concrete trinket slot behavior.</p>
 */
public final class TrinketBridge {
    private TrinketBridge() {
    }

    /**
     * Returns whether the Trinkets integration seam is available.
     *
     * @return {@code true} to signal that the project is wired for Trinkets-backed item support
     */
    public static boolean isEnabled() {
        return true;
    }
}
