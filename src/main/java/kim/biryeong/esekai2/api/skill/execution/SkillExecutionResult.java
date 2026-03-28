package kim.biryeong.esekai2.api.skill.execution;

import java.util.List;
import java.util.Objects;

/**
 * Aggregate result for a skill execution pass.
 *
 * @param executedActions number of actions that produced a side effect or completed successfully
 * @param skippedActions number of actions that were skipped because the hook could not execute them
 * @param warnings runtime warnings raised while executing the prepared graph
 */
public record SkillExecutionResult(
        int executedActions,
        int skippedActions,
        List<String> warnings
) {
    public SkillExecutionResult {
        if (executedActions < 0) {
            throw new IllegalArgumentException("executedActions must be >= 0");
        }
        if (skippedActions < 0) {
            throw new IllegalArgumentException("skippedActions must be >= 0");
        }
        Objects.requireNonNull(warnings, "warnings");
        warnings = List.copyOf(warnings);
    }
}
