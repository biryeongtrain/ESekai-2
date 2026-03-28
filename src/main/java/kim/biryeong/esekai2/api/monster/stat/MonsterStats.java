package kim.biryeong.esekai2.api.monster.stat;

import kim.biryeong.esekai2.api.monster.affix.MonsterAffixes;
import kim.biryeong.esekai2.api.monster.affix.MonsterAffixState;
import kim.biryeong.esekai2.api.monster.level.MonsterLevelContext;
import kim.biryeong.esekai2.api.stat.holder.StatHolder;
import kim.biryeong.esekai2.impl.monster.stat.MonsterStatResolver;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;

/**
 * Public entry points for resolving monster base stat holders from ESekai monster stat definitions.
 */
public final class MonsterStats {
    private MonsterStats() {
    }

    /**
     * Resolves a fresh base stat holder for the provided monster entity type.
     *
     * @param server active server providing registry access
     * @param entityType monster entity type to resolve
     * @return fresh base stat holder when a definition exists, or empty when no definition is present
     */
    public static Optional<StatHolder> resolveBaseHolder(MinecraftServer server, EntityType<?> entityType) {
        return MonsterStatResolver.resolveBaseHolder(server, entityType);
    }

    /**
     * Resolves a fresh base stat holder for the provided living entity.
     *
     * @param entity living entity whose type should be resolved
     * @return fresh base stat holder when a definition exists, or empty when no definition is present
     */
    public static Optional<StatHolder> resolveBaseHolder(LivingEntity entity) {
        return MonsterStatResolver.resolveBaseHolder(entity);
    }

    /**
     * Resolves a fresh monster stat holder that combines static monster baseline data with the provided level context.
     *
     * @param server active server providing registry access
     * @param entityType monster entity type to resolve
     * @param context current monster level context
     * @return fresh scaled stat holder when a definition exists, or empty when no definition is present
     */
    public static Optional<StatHolder> resolveScaledHolder(MinecraftServer server, EntityType<?> entityType, MonsterLevelContext context) {
        return MonsterStatResolver.resolveScaledHolder(server, entityType, context);
    }

    /**
     * Resolves a fresh monster stat holder that combines static monster baseline data with the provided level context.
     *
     * @param entity living entity whose type should be resolved
     * @param context current monster level context
     * @return fresh scaled stat holder when a definition exists, or empty when no definition is present
     */
    public static Optional<StatHolder> resolveScaledHolder(LivingEntity entity, MonsterLevelContext context) {
        return MonsterStatResolver.resolveScaledHolder(entity, context);
    }

    /**
     * Resolves a fresh monster stat holder that combines static baseline data, attached level context, and attached monster affixes.
     *
     * @param entity live monster entity whose attached runtime state should be inspected
     * @return fresh runtime stat holder when baseline and affix state are present, or empty when runtime state is absent
     */
    public static Optional<StatHolder> resolveRuntimeHolder(LivingEntity entity) {
        Optional<MonsterAffixState> state = MonsterAffixes.get(entity);
        if (state.isEmpty()) {
            return Optional.empty();
        }

        Optional<StatHolder> base = MonsterStatResolver.resolveScaledHolder(entity, state.orElseThrow().levelContext());
        if (base.isEmpty()) {
            return Optional.empty();
        }

        StatHolder statHolder = base.orElseThrow();
        for (var rolledAffix : state.orElseThrow().rolledAffixes()) {
            for (var modifier : rolledAffix.modifiers()) {
                statHolder.addModifier(modifier);
            }
        }
        return Optional.of(statHolder);
    }
}
