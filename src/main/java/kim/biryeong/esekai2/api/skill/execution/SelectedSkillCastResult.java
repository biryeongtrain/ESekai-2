package kim.biryeong.esekai2.api.skill.execution;

import kim.biryeong.esekai2.api.player.skill.SelectedActiveSkillRef;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Failure-safe result for casting one selected active skill from player equipment.
 *
 * @param success whether selection resolution, preparation, and cast execution succeeded
 * @param warnings warnings or failure reasons gathered during the request
 * @param selection selection reference used for the request when present
 * @param preparedUse prepared skill snapshot when preparation succeeded
 * @param executionResult execution result when the cast reached runtime execution
 */
public record SelectedSkillCastResult(
        boolean success,
        List<String> warnings,
        Optional<SelectedActiveSkillRef> selection,
        Optional<PreparedSkillUse> preparedUse,
        Optional<SkillExecutionResult> executionResult
) {
    public SelectedSkillCastResult {
        Objects.requireNonNull(warnings, "warnings");
        Objects.requireNonNull(selection, "selection");
        Objects.requireNonNull(preparedUse, "preparedUse");
        Objects.requireNonNull(executionResult, "executionResult");
        warnings = List.copyOf(warnings);
    }
}
