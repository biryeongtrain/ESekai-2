package kim.biryeong.esekai2.api.skill.execution;

import kim.biryeong.esekai2.api.skill.definition.graph.SkillPredicate;

import java.util.List;
import java.util.Objects;

/**
 * Prepared healing action used by {@code heal} skill nodes.
 */
public record PreparedHealAction(
        double amount,
        List<SkillPredicate> enPreds
) implements PreparedSkillAction {
    public PreparedHealAction {
        Objects.requireNonNull(enPreds, "enPreds");
        enPreds = List.copyOf(enPreds);
        if (!Double.isFinite(amount) || amount <= 0.0) {
            throw new IllegalArgumentException("amount must be finite and > 0");
        }
    }

    @Override
    public String actionType() {
        return "heal";
    }
}
