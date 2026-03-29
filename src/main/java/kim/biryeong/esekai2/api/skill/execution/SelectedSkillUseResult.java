package kim.biryeong.esekai2.api.skill.execution;

import kim.biryeong.esekai2.api.player.skill.SelectedActiveSkillRef;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Failure-safe result for preparing one selected active skill from player equipment.
 *
 * @param success whether selection resolution and preparation succeeded
 * @param warnings warnings or failure reasons gathered during selection resolution
 * @param selection selection reference used for the request when present
 * @param preparedUse prepared skill snapshot when preparation succeeded
 */
public record SelectedSkillUseResult(
        boolean success,
        List<String> warnings,
        Optional<SelectedActiveSkillRef> selection,
        Optional<PreparedSkillUse> preparedUse
) {
    public SelectedSkillUseResult {
        Objects.requireNonNull(warnings, "warnings");
        Objects.requireNonNull(selection, "selection");
        Objects.requireNonNull(preparedUse, "preparedUse");
        warnings = List.copyOf(warnings);
    }
}
