package kim.biryeong.esekai2.api.skill.support;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kim.biryeong.esekai2.api.skill.definition.graph.SkillAction;
import kim.biryeong.esekai2.api.skill.definition.graph.SkillActionType;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Describes typed field-path overrides applied to a matched skill action.
 *
 * <p>{@code matchingCalculationId} may be provided to narrow the override target when multiple
 * actions of the same type exist in one route. When omitted, the override applies to all matching
 * actions of the declared type.</p>
 *
 * @param actionType action type to match
 * @param matchingCalculationId optional calculation id constraint used for action selection
 * @param fieldOverrides typed field-path overrides applied to the matched action
 */
public record SkillActionOverride(
        SkillActionType actionType,
        String matchingCalculationId,
        List<SkillActionFieldOverride> fieldOverrides
) {
    private static final Codec<SkillActionOverride> BASE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SkillActionType.CODEC.fieldOf("action_type").forGetter(SkillActionOverride::actionType),
            Codec.STRING.optionalFieldOf("matching_calculation_id", "").forGetter(SkillActionOverride::matchingCalculationId),
            SkillActionFieldOverride.CODEC.listOf().optionalFieldOf("field_overrides", List.of())
                    .forGetter(SkillActionOverride::fieldOverrides),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("parameter_overrides", Map.of())
                    .forGetter(SkillActionOverride::legacyParameterOverrides)
    ).apply(instance, SkillActionOverride::fromCodec));

    /**
     * Validated codec used to decode action parameter overrides from datapacks and fixtures.
     */
    public static final Codec<SkillActionOverride> CODEC = BASE_CODEC.validate(SkillActionOverride::validate);

    public SkillActionOverride {
        Objects.requireNonNull(actionType, "actionType");
        Objects.requireNonNull(matchingCalculationId, "matchingCalculationId");
        Objects.requireNonNull(fieldOverrides, "fieldOverrides");
        fieldOverrides = canonicalize(fieldOverrides);
    }

    /**
     * Creates an override from the legacy parameter map representation.
     *
     * @param actionType action type to match
     * @param matchingCalculationId optional calculation id constraint used for action selection
     * @param parameterOverrides legacy parameter map to convert into typed field overrides
     */
    public SkillActionOverride(
            SkillActionType actionType,
            String matchingCalculationId,
            Map<String, String> parameterOverrides
    ) {
        this(actionType, matchingCalculationId, legacyFieldOverrides(parameterOverrides));
    }

    private static SkillActionOverride fromCodec(
            SkillActionType actionType,
            String matchingCalculationId,
            List<SkillActionFieldOverride> fieldOverrides,
            Map<String, String> legacyParameterOverrides
    ) {
        List<SkillActionFieldOverride> merged = new ArrayList<>(legacyParameterOverrides.size() + fieldOverrides.size());
        for (Map.Entry<String, String> entry : legacyParameterOverrides.entrySet()) {
            merged.add(SkillActionFieldOverride.parameter(entry.getKey(), entry.getValue()));
        }
        merged.addAll(fieldOverrides);
        return new SkillActionOverride(actionType, matchingCalculationId, merged);
    }

    /**
     * Returns whether this override should apply to the provided action.
     *
     * @param action action to test
     * @return {@code true} when action type matches and calculation id constraint is satisfied
     */
    public boolean matches(SkillAction action) {
        Objects.requireNonNull(action, "action");
        if (action.type() != actionType && !(action.type().isMobEffectAction() && actionType.isMobEffectAction())) {
            return false;
        }
        if (matchingCalculationId.isBlank()) {
            return true;
        }
        return action.calculationId().equals(matchingCalculationId);
    }

    /**
     * Returns the parameter-specific overrides for compatibility with existing callers.
     *
     * @return typed parameter overrides flattened to their legacy map view
     */
    public Map<String, String> parameterOverrides() {
        Map<String, String> parameterOverrides = new LinkedHashMap<>();
        for (SkillActionFieldOverride fieldOverride : fieldOverrides) {
            Objects.requireNonNull(fieldOverride, "fieldOverrides entry");
            if (fieldOverride.path().type() == SkillActionFieldPathType.PARAMETER) {
                parameterOverrides.put(fieldOverride.path().parameterKey(), fieldOverride.value());
            }
        }
        return Map.copyOf(parameterOverrides);
    }

    /**
     * Returns the calculation-id override when one is present.
     *
     * @return calculation-id replacement or an empty string when not overridden
     */
    public String calculationIdOverride() {
        for (SkillActionFieldOverride fieldOverride : fieldOverrides) {
            if (fieldOverride.path().type() == SkillActionFieldPathType.CALCULATION_ID) {
                return fieldOverride.value();
            }
        }
        return "";
    }

    private Map<String, String> legacyParameterOverrides() {
        return Map.of();
    }

    private static DataResult<SkillActionOverride> validate(SkillActionOverride override) {
        if (override.fieldOverrides().isEmpty()) {
            return DataResult.error(() -> "field_overrides must not be empty");
        }

        if (!override.matchingCalculationId().isBlank() && Identifier.tryParse(override.matchingCalculationId()) == null) {
            return DataResult.error(() -> "matching_calculation_id must be empty or a valid identifier: " + override.matchingCalculationId());
        }

        return DataResult.success(override);
    }

    private static List<SkillActionFieldOverride> canonicalize(List<SkillActionFieldOverride> overrides) {
        Map<SkillActionFieldPath, SkillActionFieldOverride> ordered = new LinkedHashMap<>();
        for (SkillActionFieldOverride override : overrides) {
            Objects.requireNonNull(override, "fieldOverrides entry");
            Objects.requireNonNull(override.path(), "fieldOverrides entry path");
            ordered.put(override.path(), override);
        }
        return List.copyOf(ordered.values());
    }

    private static List<SkillActionFieldOverride> legacyFieldOverrides(Map<String, String> parameterOverrides) {
        Objects.requireNonNull(parameterOverrides, "parameterOverrides");
        List<SkillActionFieldOverride> overrides = new ArrayList<>(parameterOverrides.size());
        for (Map.Entry<String, String> entry : parameterOverrides.entrySet()) {
            overrides.add(SkillActionFieldOverride.parameter(entry.getKey(), entry.getValue()));
        }
        return overrides;
    }
}
