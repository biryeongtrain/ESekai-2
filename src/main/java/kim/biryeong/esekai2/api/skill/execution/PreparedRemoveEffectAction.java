package kim.biryeong.esekai2.api.skill.execution;

import kim.biryeong.esekai2.api.skill.definition.graph.SkillPredicate;
import kim.biryeong.esekai2.api.skill.effect.SkillEffectPurgeMode;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Prepared effect-removal action used by {@code remove_effect} skill nodes.
 */
public record PreparedRemoveEffectAction(
        List<Identifier> effectIds,
        Optional<SkillEffectPurgeMode> purgeMode,
        String serializedActionType,
        List<SkillPredicate> enPreds
) implements PreparedSkillAction {
    public PreparedRemoveEffectAction {
        Objects.requireNonNull(effectIds, "effectIds");
        Objects.requireNonNull(purgeMode, "purgeMode");
        Objects.requireNonNull(serializedActionType, "serializedActionType");
        Objects.requireNonNull(enPreds, "enPreds");
        effectIds = List.copyOf(effectIds);
        purgeMode = Objects.requireNonNullElse(purgeMode, Optional.empty());
        enPreds = List.copyOf(enPreds);
        if (effectIds.isEmpty() && purgeMode.isEmpty()) {
            throw new IllegalArgumentException("remove_effect requires explicit effectIds or a purgeMode");
        }
        if (serializedActionType.isBlank()) {
            throw new IllegalArgumentException("serializedActionType must not be blank");
        }
    }

    public PreparedRemoveEffectAction(Identifier effectId, String serializedActionType) {
        this(List.of(effectId), Optional.empty(), serializedActionType, List.of());
    }

    /**
     * Returns the first configured effect id for legacy callers that still expect a scalar surface.
     *
     * @return first configured remove-effect target
     */
    public Identifier effectId() {
        if (effectIds.isEmpty()) {
            throw new IllegalStateException("remove_effect action does not carry explicit effect ids");
        }
        return effectIds.getFirst();
    }

    @Override
    public String actionType() {
        return serializedActionType;
    }
}
