package kim.biryeong.esekai2.api.skill.execution;

import kim.biryeong.esekai2.api.damage.calculation.HitDamageCalculation;

import java.util.Objects;

/**
 * Prepared damage action.
 */
public record PreparedDamageAction(HitDamageCalculation hitDamageCalculation) implements PreparedSkillAction {
    public PreparedDamageAction {
        Objects.requireNonNull(hitDamageCalculation, "hitDamageCalculation");
    }

    @Override
    public String actionType() {
        return "damage";
    }
}

