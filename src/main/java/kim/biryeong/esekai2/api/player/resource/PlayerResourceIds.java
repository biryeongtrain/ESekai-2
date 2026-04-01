package kim.biryeong.esekai2.api.player.resource;

import java.util.Objects;

/**
 * Canonical player resource identifiers used by ESekai's current runtime.
 */
public final class PlayerResourceIds {
    /**
     * Stable identifier for the built-in mana resource.
     */
    public static final String MANA = "mana";

    private PlayerResourceIds() {
    }

    /**
     * Returns whether the provided resource id is syntactically usable by the current runtime.
     *
     * @param resource resource id to validate
     * @return {@code true} when the id is non-null and non-blank
     */
    public static boolean isUsable(String resource) {
        return resource != null && !resource.isBlank();
    }

    /**
     * Returns whether the provided resource id is the built-in mana resource.
     *
     * @param resource resource id to inspect
     * @return {@code true} when the id equals {@link #MANA}
     */
    public static boolean isMana(String resource) {
        return MANA.equals(resource);
    }

    /**
     * Validates that the provided resource id is non-null and non-blank.
     *
     * @param resource resource id to validate
     * @return validated resource id
     */
    public static String requireUsable(String resource) {
        Objects.requireNonNull(resource, "resource");
        if (resource.isBlank()) {
            throw new IllegalArgumentException("resource must not be blank");
        }
        return resource;
    }
}
