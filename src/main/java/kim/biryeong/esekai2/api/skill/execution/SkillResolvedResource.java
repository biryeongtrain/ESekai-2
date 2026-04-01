package kim.biryeong.esekai2.api.skill.execution;

/**
 * Snapshot of one resolved named resource for a skill subject.
 *
 * @param currentAmount resolved current amount
 * @param maxAmount resolved maximum amount
 */
public record SkillResolvedResource(
        double currentAmount,
        double maxAmount
) {
}
