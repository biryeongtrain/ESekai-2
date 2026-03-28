package kim.biryeong.esekai2.api.level;

/**
 * Shared level validation rules used by ESekai's current level systems.
 */
public final class LevelRules {
    public static final int MIN_LEVEL = 1;
    public static final int MAX_LEVEL = 100;

    private LevelRules() {
    }

    /**
     * Returns whether the provided level is inside ESekai's supported range.
     *
     * @param level level to inspect
     * @return {@code true} when the level is within the shared range
     */
    public static boolean isValidLevel(int level) {
        return level >= MIN_LEVEL && level <= MAX_LEVEL;
    }

    /**
     * Validates and returns the provided level.
     *
     * @param level level to validate
     * @param label field label used in exception messages
     * @return the validated level
     */
    public static int requireValidLevel(int level, String label) {
        if (!isValidLevel(level)) {
            throw new IllegalArgumentException(label + " must be between " + MIN_LEVEL + " and " + MAX_LEVEL);
        }

        return level;
    }

    /**
     * Validates and returns the provided level using the generic {@code level} label.
     *
     * @param level level to validate
     * @return validated level
     */
    public static int validateLevel(int level) {
        return requireValidLevel(level, "level");
    }

    /**
     * Clamps the provided level into ESekai's supported range.
     *
     * @param level level to clamp
     * @return clamped level
     */
    public static int clamp(int level) {
        return Math.max(MIN_LEVEL, Math.min(MAX_LEVEL, level));
    }
}
