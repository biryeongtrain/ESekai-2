package kim.biryeong.esekai2.api.skill.execution;

import kim.biryeong.esekai2.api.skill.definition.graph.SkillPredicate;

import java.util.List;
import java.util.Objects;

/**
 * Prepared resource delta action used by {@code resource_delta} skill nodes.
 */
public record PreparedResourceDeltaAction(
        String resource,
        double amount,
        List<SkillPredicate> enPreds
) implements PreparedSkillAction {
    public static final String MANA_RESOURCE = "mana";

    public PreparedResourceDeltaAction {
        Objects.requireNonNull(resource, "resource");
        Objects.requireNonNull(enPreds, "enPreds");
        enPreds = List.copyOf(enPreds);
        if (resource.isBlank()) {
            throw new IllegalArgumentException("resource must not be blank");
        }
        if (!Double.isFinite(amount) || amount == 0.0) {
            throw new IllegalArgumentException("amount must be finite and != 0");
        }
    }

    @Override
    public String actionType() {
        return "resource_delta";
    }
}
