package kim.biryeong.esekai2.impl.monster.stat;

import kim.biryeong.esekai2.api.monster.level.MonsterLevelContext;
import kim.biryeong.esekai2.api.monster.level.MonsterLevelProfile;
import kim.biryeong.esekai2.api.monster.level.MonsterLevels;
import kim.biryeong.esekai2.api.monster.level.MonsterScaledStatAxis;
import kim.biryeong.esekai2.api.monster.stat.MonsterRegistries;
import kim.biryeong.esekai2.api.monster.stat.MonsterStatDefinition;
import kim.biryeong.esekai2.api.stat.holder.StatHolder;
import kim.biryeong.esekai2.api.stat.holder.StatHolders;
import kim.biryeong.esekai2.impl.stat.registry.StatRegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;

/**
 * Resolves monster stat definitions into fresh runtime stat holders.
 */
public final class MonsterStatResolver {
    private MonsterStatResolver() {
    }

    public static Optional<StatHolder> resolveBaseHolder(MinecraftServer server, EntityType<?> entityType) {
        Optional<MonsterStatDefinition> definition = findDefinition(server.registryAccess().lookupOrThrow(MonsterRegistries.MONSTER_STAT), entityType);
        if (definition.isEmpty()) {
            return Optional.empty();
        }

        StatHolder statHolder = StatHolders.create(StatRegistryAccess.statRegistry(server));
        for (var entry : definition.orElseThrow().baseStats().entrySet()) {
            statHolder.setBaseValue(entry.getKey(), entry.getValue());
        }

        return Optional.of(statHolder);
    }

    public static Optional<StatHolder> resolveBaseHolder(LivingEntity entity) {
        if (entity.level().getServer() == null) {
            return Optional.empty();
        }

        return resolveBaseHolder(entity.level().getServer(), entity.getType());
    }

    public static Optional<StatHolder> resolveScaledHolder(MinecraftServer server, EntityType<?> entityType, MonsterLevelContext context) {
        Optional<MonsterStatDefinition> definition = findDefinition(server.registryAccess().lookupOrThrow(MonsterRegistries.MONSTER_STAT), entityType);
        if (definition.isEmpty()) {
            return Optional.empty();
        }

        MonsterLevelProfile levelProfile = MonsterLevels.resolveProfile(server, context);
        StatHolder statHolder = StatHolders.create(StatRegistryAccess.statRegistry(server));

        for (var entry : definition.orElseThrow().baseStats().entrySet()) {
            statHolder.setBaseValue(entry.getKey(), entry.getValue());
        }

        for (var entry : definition.orElseThrow().scaledStats().entrySet()) {
            statHolder.setBaseValue(entry.getKey(), resolveScaledValue(levelProfile, entry.getValue()));
        }

        return Optional.of(statHolder);
    }

    public static Optional<StatHolder> resolveScaledHolder(LivingEntity entity, MonsterLevelContext context) {
        if (entity.level().getServer() == null) {
            return Optional.empty();
        }

        return resolveScaledHolder(entity.level().getServer(), entity.getType(), context);
    }

    private static Optional<MonsterStatDefinition> findDefinition(Registry<MonsterStatDefinition> registry, EntityType<?> entityType) {
        MonsterStatDefinition matchedDefinition = null;

        for (MonsterStatDefinition definition : registry) {
            if (!definition.entityType().equals(entityType)) {
                continue;
            }

            if (matchedDefinition != null) {
                throw new IllegalStateException("duplicate monster stat definitions found for entity type: " + entityType);
            }

            matchedDefinition = definition;
        }

        return Optional.ofNullable(matchedDefinition);
    }

    private static double resolveScaledValue(MonsterLevelProfile profile, MonsterScaledStatAxis axis) {
        return switch (axis) {
            case LIFE -> profile.baseLife() * profile.effectiveLifeMultiplier();
            case ACCURACY -> profile.baseAccuracy();
            case EVADE -> profile.baseEvasion();
        };
    }
}
