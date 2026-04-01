package kim.biryeong.esekai2.api.skill.execution;

/**
 * Snapshot of SELF-scoped player runtime state for the skill currently being prepared or executed.
 *
 * @param cooldownRemainingTicks remaining cooldown ticks for the current skill
 * @param availableCharges currently available charges for the current skill
 * @param remainingBurstCasts remaining burst follow-up casts for the current skill
 */
public record SkillResolvedRuntimeState(
        double cooldownRemainingTicks,
        double availableCharges,
        double remainingBurstCasts
) {
    public SkillResolvedRuntimeState {
        cooldownRemainingTicks = sanitize(cooldownRemainingTicks);
        availableCharges = sanitize(availableCharges);
        remainingBurstCasts = sanitize(remainingBurstCasts);
    }

    private static double sanitize(double value) {
        if (!Double.isFinite(value) || value < 0.0) {
            return 0.0;
        }
        return value;
    }
}
