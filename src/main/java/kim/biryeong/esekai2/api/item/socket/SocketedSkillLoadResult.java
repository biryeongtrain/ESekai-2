package kim.biryeong.esekai2.api.item.socket;

import kim.biryeong.esekai2.api.skill.definition.SkillDefinition;
import kim.biryeong.esekai2.api.skill.support.SkillSupportDefinition;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Resolved skill/support definitions loaded from one socketed item stack.
 *
 * @param activeSkill resolved active skill definition when the stack declares a valid one
 * @param linkedSupports resolved linked support definitions from the active skill link group
 * @param warnings non-fatal resolution warnings such as missing support definitions
 */
public record SocketedSkillLoadResult(
        Optional<SkillDefinition> activeSkill,
        List<SkillSupportDefinition> linkedSupports,
        List<String> warnings
) {
    public SocketedSkillLoadResult {
        Objects.requireNonNull(activeSkill, "activeSkill");
        Objects.requireNonNull(linkedSupports, "linkedSupports");
        Objects.requireNonNull(warnings, "warnings");
        linkedSupports = List.copyOf(linkedSupports);
        warnings = List.copyOf(warnings);
    }

    /**
     * Returns whether the item resolved a valid active skill definition.
     *
     * @return {@code true} when an active skill definition is present
     */
    public boolean hasActiveSkill() {
        return activeSkill.isPresent();
    }
}
