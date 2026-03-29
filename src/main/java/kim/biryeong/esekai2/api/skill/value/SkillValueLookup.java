package kim.biryeong.esekai2.api.skill.value;

import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;

import java.util.Objects;
import java.util.Optional;

/**
 * Resolves datapack-backed skill values during skill preparation and execution.
 */
@FunctionalInterface
public interface SkillValueLookup {
    /**
     * Shared lookup that never resolves any value entries.
     */
    SkillValueLookup EMPTY = valueId -> Optional.empty();

    /**
     * Resolves one referenced reusable value entry.
     *
     * @param valueId stable identifier referenced by a skill payload
     * @return resolved value entry when available
     */
    Optional<SkillValueDefinition> resolve(Identifier valueId);

    /**
     * Returns a lookup that never resolves any entries.
     *
     * @return empty lookup
     */
    static SkillValueLookup empty() {
        return EMPTY;
    }

    /**
     * Adapts a dynamic registry into a value lookup.
     *
     * @param registry skill value registry to query
     * @return lookup backed by the provided registry
     */
    static SkillValueLookup fromRegistry(Registry<SkillValueDefinition> registry) {
        Objects.requireNonNull(registry, "registry");
        return valueId -> registry.getOptional(Objects.requireNonNull(valueId, "valueId"));
    }
}
