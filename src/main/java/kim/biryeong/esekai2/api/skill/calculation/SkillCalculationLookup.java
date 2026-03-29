package kim.biryeong.esekai2.api.skill.calculation;

import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;

import java.util.Objects;
import java.util.Optional;

/**
 * Resolves datapack-backed skill calculations during skill preparation.
 */
@FunctionalInterface
public interface SkillCalculationLookup {
    /**
     * Shared lookup that never resolves any calculation entries.
     */
    SkillCalculationLookup EMPTY = calculationId -> Optional.empty();

    /**
     * Resolves one referenced calculation entry.
     *
     * @param calculationId stable identifier referenced by a skill action
     * @return resolved calculation entry when available
     */
    Optional<SkillCalculationDefinition> resolve(Identifier calculationId);

    /**
     * Returns a lookup that never resolves any entries.
     *
     * @return empty lookup
     */
    static SkillCalculationLookup empty() {
        return EMPTY;
    }

    /**
     * Adapts a dynamic registry into a calculation lookup.
     *
     * @param registry skill calculation registry to query
     * @return lookup backed by the provided registry
     */
    static SkillCalculationLookup fromRegistry(Registry<SkillCalculationDefinition> registry) {
        Objects.requireNonNull(registry, "registry");
        return calculationId -> registry.getOptional(Objects.requireNonNull(calculationId, "calculationId"));
    }
}
