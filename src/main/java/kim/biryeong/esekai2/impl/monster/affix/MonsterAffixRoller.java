package kim.biryeong.esekai2.impl.monster.affix;

import kim.biryeong.esekai2.api.monster.affix.MonsterAffixDefinition;
import kim.biryeong.esekai2.api.monster.affix.MonsterAffixKind;
import kim.biryeong.esekai2.api.monster.affix.MonsterAffixModifierDefinition;
import kim.biryeong.esekai2.api.monster.affix.MonsterAffixPoolDefinition;
import kim.biryeong.esekai2.api.monster.affix.MonsterAffixRollProfile;
import kim.biryeong.esekai2.api.monster.affix.MonsterAffixState;
import kim.biryeong.esekai2.api.monster.affix.RolledMonsterAffix;
import kim.biryeong.esekai2.api.monster.level.MonsterLevelContext;
import kim.biryeong.esekai2.api.stat.modifier.StatModifier;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntityType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.random.RandomGenerator;

/**
 * Internal helper for selecting and rolling monster affixes.
 */
public final class MonsterAffixRoller {
    private MonsterAffixRoller() {
    }

    public static Optional<MonsterAffixState> rollState(
            MinecraftServer server,
            EntityType<?> entityType,
            MonsterLevelContext context,
            MonsterAffixRollProfile profile,
            RandomGenerator random
    ) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(entityType, "entityType");
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(profile, "profile");
        Objects.requireNonNull(random, "random");

        Optional<MonsterAffixPoolDefinition> pool = findPool(server, entityType);
        if (pool.isEmpty()) {
            return Optional.empty();
        }

        Map<Identifier, MonsterAffixDefinition> candidates = resolveCandidateAffixes(server, pool.orElseThrow());
        List<RolledMonsterAffix> rolled = new ArrayList<>();
        rolled.addAll(selectAndRoll(candidates, context, MonsterAffixKind.PREFIX, profile.prefixCount(), random));
        rolled.addAll(selectAndRoll(candidates, context, MonsterAffixKind.SUFFIX, profile.suffixCount(), random));
        return Optional.of(new MonsterAffixState(context, rolled));
    }

    public static Optional<MonsterLevelContext> defaultSpawnContext(MinecraftServer server, EntityType<?> entityType) {
        return findPool(server, entityType).map(MonsterAffixPoolDefinition::defaultSpawnContext);
    }

    private static Optional<MonsterAffixPoolDefinition> findPool(MinecraftServer server, EntityType<?> entityType) {
        MonsterAffixPoolDefinition matchedDefinition = null;
        for (MonsterAffixPoolDefinition definition : MonsterAffixRegistryAccess.poolRegistry(server)) {
            if (!definition.entityType().equals(entityType)) {
                continue;
            }

            if (matchedDefinition != null) {
                throw new IllegalStateException("duplicate monster affix pools found for entity type: " + entityType);
            }

            matchedDefinition = definition;
        }

        return Optional.ofNullable(matchedDefinition);
    }

    private static Map<Identifier, MonsterAffixDefinition> resolveCandidateAffixes(MinecraftServer server, MonsterAffixPoolDefinition pool) {
        Map<Identifier, MonsterAffixDefinition> resolved = new LinkedHashMap<>();
        var registry = MonsterAffixRegistryAccess.affixRegistry(server);
        for (Identifier candidateId : pool.candidateAffixIds()) {
            MonsterAffixDefinition definition = registry.getOptional(candidateId)
                    .orElseThrow(() -> new IllegalStateException("Unknown monster affix referenced by pool: " + candidateId));
            resolved.put(candidateId, definition);
        }
        return resolved;
    }

    private static List<RolledMonsterAffix> selectAndRoll(
            Map<Identifier, MonsterAffixDefinition> candidates,
            MonsterLevelContext context,
            MonsterAffixKind kind,
            int count,
            RandomGenerator random
    ) {
        if (count <= 0) {
            return List.of();
        }

        List<Map.Entry<Identifier, MonsterAffixDefinition>> eligible = new ArrayList<>();
        for (Map.Entry<Identifier, MonsterAffixDefinition> entry : candidates.entrySet()) {
            MonsterAffixDefinition definition = entry.getValue();
            if (definition.kind() != kind) {
                continue;
            }

            if (!definition.isAvailableAtLevel(context.level())) {
                continue;
            }

            if (!definition.supports(context.rarity())) {
                continue;
            }

            eligible.add(entry);
        }

        if (eligible.isEmpty()) {
            return List.of();
        }

        List<RolledMonsterAffix> result = new ArrayList<>();
        List<Map.Entry<Identifier, MonsterAffixDefinition>> remaining = new ArrayList<>(eligible);
        while (!remaining.isEmpty() && result.size() < count) {
            Map.Entry<Identifier, MonsterAffixDefinition> selected = selectWeighted(remaining, random);
            remaining.remove(selected);
            result.add(rollAffix(selected.getKey(), selected.getValue(), random));
        }

        return List.copyOf(result);
    }

    private static Map.Entry<Identifier, MonsterAffixDefinition> selectWeighted(
            List<Map.Entry<Identifier, MonsterAffixDefinition>> candidates,
            RandomGenerator random
    ) {
        double totalWeight = 0.0;
        for (Map.Entry<Identifier, MonsterAffixDefinition> candidate : candidates) {
            totalWeight += candidate.getValue().weight();
        }

        double roll = random.nextDouble(totalWeight);
        double cumulative = 0.0;
        for (Map.Entry<Identifier, MonsterAffixDefinition> candidate : candidates) {
            cumulative += candidate.getValue().weight();
            if (roll < cumulative) {
                return candidate;
            }
        }

        return candidates.getLast();
    }

    private static RolledMonsterAffix rollAffix(
            Identifier affixId,
            MonsterAffixDefinition definition,
            RandomGenerator random
    ) {
        List<StatModifier> modifiers = new ArrayList<>(definition.modifierRanges().size());
        for (MonsterAffixModifierDefinition modifierRange : definition.modifierRanges()) {
            modifiers.add(new StatModifier(
                    modifierRange.stat(),
                    modifierRange.operation(),
                    rolledValue(modifierRange, random),
                    affixId
            ));
        }

        return new RolledMonsterAffix(affixId, definition.kind(), modifiers);
    }

    private static double rolledValue(MonsterAffixModifierDefinition definition, RandomGenerator random) {
        if (definition.minValue() == definition.maxValue()) {
            return definition.minValue();
        }

        return random.nextDouble(definition.minValue(), definition.maxValue());
    }
}
