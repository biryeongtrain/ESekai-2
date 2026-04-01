package kim.biryeong.esekai2.impl.stat.runtime;

import kim.biryeong.esekai2.api.monster.stat.MonsterStats;
import kim.biryeong.esekai2.api.player.stat.PlayerCombatStats;
import kim.biryeong.esekai2.api.stat.holder.StatHolder;
import kim.biryeong.esekai2.api.stat.holder.StatHolders;
import kim.biryeong.esekai2.impl.stat.registry.StatRegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.Objects;
import java.util.Optional;

/**
 * Shared runtime combat stat resolution for live entities across skill and ailment execution paths.
 */
public final class LivingEntityCombatStatResolver {
    private LivingEntityCombatStatResolver() {
    }

    /**
     * Resolves the runtime combat stat holder for the provided entity when one exists.
     *
     * <p>Players resolve to their shared {@code PlayerCombatStats} holder. Monsters prefer runtime holders with
     * affixes and fall back to baseline monster stats when runtime state is absent.</p>
     *
     * @param entity entity whose combat stats should be resolved
     * @return resolved runtime combat stat holder when one exists
     */
    public static Optional<StatHolder> resolve(Entity entity) {
        Objects.requireNonNull(entity, "entity");
        if (!(entity instanceof LivingEntity livingEntity)) {
            return Optional.empty();
        }
        return resolve(livingEntity);
    }

    /**
     * Resolves the runtime combat stat holder for the provided living entity when one exists.
     *
     * @param entity living entity whose combat stats should be resolved
     * @return resolved runtime combat stat holder when one exists
     */
    public static Optional<StatHolder> resolve(LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");
        if (entity instanceof ServerPlayer player) {
            return Optional.of(PlayerCombatStats.get(player));
        }
        return MonsterStats.resolveRuntimeHolder(entity)
                .or(() -> MonsterStats.resolveBaseHolder(entity));
    }

    /**
     * Resolves a live entity to a runtime combat holder, falling back to a fresh empty holder when unavailable.
     *
     * @param server active server providing registry access
     * @param entity optional runtime entity
     * @return resolved runtime holder or a fresh empty fallback
     */
    public static StatHolder resolveEntityOrFresh(MinecraftServer server, Optional<? extends Entity> entity) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(entity, "entity");
        return entity.flatMap(LivingEntityCombatStatResolver::resolve)
                .orElseGet(() -> empty(server));
    }

    /**
     * Creates a fresh empty combat stat holder backed by the active stat registry.
     *
     * @param server active server providing registry access
     * @return empty stat holder
     */
    public static StatHolder empty(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        return StatHolders.create(StatRegistryAccess.statRegistry(server));
    }
}
