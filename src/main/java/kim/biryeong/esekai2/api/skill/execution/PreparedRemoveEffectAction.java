package kim.biryeong.esekai2.api.skill.execution;

import kim.biryeong.esekai2.api.skill.definition.graph.SkillPredicate;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Objects;

/**
 * Prepared effect-removal action used by {@code remove_effect} skill nodes.
 */
public record PreparedRemoveEffectAction(
        Identifier effectId,
        String serializedActionType,
        List<SkillPredicate> enPreds
) implements PreparedSkillAction {
    public PreparedRemoveEffectAction {
        Objects.requireNonNull(effectId, "effectId");
        Objects.requireNonNull(serializedActionType, "serializedActionType");
        Objects.requireNonNull(enPreds, "enPreds");
        enPreds = List.copyOf(enPreds);
        if (serializedActionType.isBlank()) {
            throw new IllegalArgumentException("serializedActionType must not be blank");
        }
    }

    public PreparedRemoveEffectAction(Identifier effectId, String serializedActionType) {
        this(effectId, serializedActionType, List.of());
    }

    @Override
    public String actionType() {
        return serializedActionType;
    }
}
