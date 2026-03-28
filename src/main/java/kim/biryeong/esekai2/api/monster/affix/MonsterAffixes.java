package kim.biryeong.esekai2.api.monster.affix;

import kim.biryeong.esekai2.api.config.monster.MonsterAffixCountConfig;
import kim.biryeong.esekai2.api.monster.affix.MonsterAffixRollProfile;
import kim.biryeong.esekai2.api.monster.level.MonsterLevelContext;
import kim.biryeong.esekai2.impl.config.monster.MonsterAffixConfigManager;
import kim.biryeong.esekai2.impl.monster.affix.MonsterAffixBootstrap;
import kim.biryeong.esekai2.impl.monster.affix.MonsterAffixRoller;
import kim.biryeong.esekai2.impl.monster.affix.MonsterAffixRuntimeInitializer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.Optional;
import java.util.random.RandomGenerator;

/**
 * Public entry points for rolling monster affixes and accessing live runtime state.
 */
public final class MonsterAffixes {
    private MonsterAffixes() {
    }

    /**
     * Ensures the monster affix registries and attachment types are initialized.
     */
    public static void bootstrap() {
        MonsterAffixBootstrap.bootstrap();
    }

    /**
     * Rolls a monster affix state using the current server config counts.
     *
     * @param server active server providing registry access
     * @param entityType monster type to roll for
     * @param context level and rarity context used for eligibility
     * @param random random source used for weighted selection and stat rolls
     * @return rolled state when a monster affix pool exists, or empty when no pool is defined
     */
    public static Optional<MonsterAffixState> rollState(
            MinecraftServer server,
            EntityType<?> entityType,
            MonsterLevelContext context,
            RandomGenerator random
    ) {
        MonsterAffixCountConfig counts = MonsterAffixConfigManager.currentCounts();
        MonsterAffixRollProfile profile = new MonsterAffixRollProfile(
                counts.profile(context.rarity()).prefixCount(),
                counts.profile(context.rarity()).suffixCount()
        );
        return MonsterAffixRoller.rollState(server, entityType, context, profile, random);
    }

    /**
     * Stores the provided monster affix state on the target entity.
     *
     * @param entity target live entity
     * @param state runtime affix state to attach
     */
    public static void attach(LivingEntity entity, MonsterAffixState state) {
        MonsterAffixBootstrap.attach(entity, state);
    }

    /**
     * Returns the currently attached monster affix state for the provided entity.
     *
     * @param entity target live entity
     * @return attached runtime affix state when present
     */
    public static Optional<MonsterAffixState> get(LivingEntity entity) {
        if (!entity.hasAttached(MonsterAffixBootstrap.MONSTER_AFFIX_STATE)
                && entity instanceof Mob mob
                && entity.level().getServer() != null) {
            MonsterAffixRuntimeInitializer.initialize(mob, new java.util.Random(mob.getRandom().nextLong()));
        }

        return MonsterAffixBootstrap.get(entity);
    }
}
