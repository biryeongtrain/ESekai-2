package kim.biryeong.esekai2.api.skill.execution;

/**
 * Resolves prepared skill configuration snapshots for one active prepared use.
 */
public interface SkillPreparedStateLookup {
    /**
     * Shared lookup that reports no prepared-state values.
     */
    SkillPreparedStateLookup EMPTY = new SkillPreparedStateLookup() {
    };

    /**
     * Returns the resolved prepared resource cost.
     *
     * @return resolved prepared resource cost, or {@code 0.0} when unavailable
     */
    default double resourceCost() {
        return 0.0;
    }

    /**
     * Returns the resolved prepared use time in ticks.
     *
     * @return resolved prepared use time, or {@code 0} when unavailable
     */
    default int useTimeTicks() {
        return 0;
    }

    /**
     * Returns the resolved prepared cooldown in ticks.
     *
     * @return resolved prepared cooldown, or {@code 0} when unavailable
     */
    default int cooldownTicks() {
        return 0;
    }

    /**
     * Returns the configured maximum charges for the prepared skill.
     *
     * @return prepared maximum charges, or {@code 0} when unavailable
     */
    default int maxCharges() {
        return 0;
    }

    /**
     * Returns the configured burst cast count for the prepared skill.
     *
     * @return prepared total casts in one burst window, or {@code 0} when unavailable
     */
    default int timesToCast() {
        return 0;
    }

    /**
     * Returns a lookup that reports no prepared-state values.
     *
     * @return empty lookup
     */
    static SkillPreparedStateLookup empty() {
        return EMPTY;
    }

    /**
     * Creates one immutable prepared-state snapshot lookup.
     *
     * @param resourceCost resolved prepared resource cost
     * @param useTimeTicks resolved prepared use time
     * @param cooldownTicks resolved prepared cooldown
     * @param maxCharges configured maximum charges
     * @param timesToCast configured total casts in one burst window
     * @return immutable prepared-state snapshot lookup
     */
    static SkillPreparedStateLookup of(
            double resourceCost,
            int useTimeTicks,
            int cooldownTicks,
            int maxCharges,
            int timesToCast
    ) {
        return new Snapshot(resourceCost, useTimeTicks, cooldownTicks, maxCharges, timesToCast);
    }

    /**
     * Immutable prepared-state snapshot implementation.
     *
     * @param resourceCost resolved prepared resource cost
     * @param useTimeTicks resolved prepared use time
     * @param cooldownTicks resolved prepared cooldown
     * @param maxCharges configured maximum charges
     * @param timesToCast configured total casts in one burst window
     */
    record Snapshot(
            double resourceCost,
            int useTimeTicks,
            int cooldownTicks,
            int maxCharges,
            int timesToCast
    ) implements SkillPreparedStateLookup {
        public Snapshot {
            if (!Double.isFinite(resourceCost) || resourceCost < 0.0) {
                throw new IllegalArgumentException("resourceCost must be finite and >= 0");
            }
            if (useTimeTicks < 0) {
                throw new IllegalArgumentException("useTimeTicks must be >= 0");
            }
            if (cooldownTicks < 0) {
                throw new IllegalArgumentException("cooldownTicks must be >= 0");
            }
            if (maxCharges < 0) {
                throw new IllegalArgumentException("maxCharges must be >= 0");
            }
            if (timesToCast < 0) {
                throw new IllegalArgumentException("timesToCast must be >= 0");
            }
        }
    }
}
