package kim.biryeong.esekai2.api.skill.execution;

import kim.biryeong.esekai2.api.damage.calculation.HitDamageCalculation;
import kim.biryeong.esekai2.api.skill.definition.graph.SkillPredicate;

import java.util.List;
import java.util.Objects;

/**
 * Prepared damage action.
 *
 * @param hitDamageCalculation resolved hit damage calculation
 * @param calculationId optional reference id carried from the skill schema
 * @param enPreds action-local execution predicates
 */
public record PreparedDamageAction(
        HitDamageCalculation hitDamageCalculation,
        String calculationId,
        List<SkillPredicate> enPreds
) implements PreparedSkillAction {
    public PreparedDamageAction {
        Objects.requireNonNull(hitDamageCalculation, "hitDamageCalculation");
        Objects.requireNonNull(calculationId, "calculationId");
        Objects.requireNonNull(enPreds, "enPreds");
        enPreds = List.copyOf(enPreds);
    }

    public PreparedDamageAction(HitDamageCalculation hitDamageCalculation) {
        this(hitDamageCalculation, "", List.of());
    }

    public PreparedDamageAction(HitDamageCalculation hitDamageCalculation, String calculationId) {
        this(hitDamageCalculation, calculationId, List.of());
    }

    @Override
    public String actionType() {
        return "damage";
    }
}
