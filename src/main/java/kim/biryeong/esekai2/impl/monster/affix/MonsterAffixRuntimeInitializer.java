package kim.biryeong.esekai2.impl.monster.affix;

import kim.biryeong.esekai2.api.config.monster.MonsterAffixCountConfig;
import kim.biryeong.esekai2.api.monster.affix.MonsterAffixRollProfile;
import kim.biryeong.esekai2.api.monster.affix.MonsterAffixState;
import kim.biryeong.esekai2.api.monster.level.MonsterLevelContext;
import kim.biryeong.esekai2.impl.config.monster.MonsterAffixConfigManager;
import net.minecraft.world.entity.Mob;

import java.util.Optional;
import java.util.random.RandomGenerator;

/**
 * Initializes persistent monster affix state for live mobs when a pool definition exists.
 */
public final class MonsterAffixRuntimeInitializer {
    private MonsterAffixRuntimeInitializer() {
    }

    public static void initialize(Mob mob, RandomGenerator random) {
        if (mob.hasAttached(MonsterAffixBootstrap.MONSTER_AFFIX_STATE)) {
            return;
        }

        if (mob.level().getServer() == null) {
            return;
        }

        Optional<MonsterLevelContext> defaultContext = MonsterAffixRoller.defaultSpawnContext(mob.level().getServer(), mob.getType());
        if (defaultContext.isEmpty()) {
            return;
        }

        MonsterAffixCountConfig counts = MonsterAffixConfigManager.currentCounts();
        MonsterLevelContext context = defaultContext.orElseThrow();
        MonsterAffixRollProfile profile = new MonsterAffixRollProfile(
                counts.profile(context.rarity()).prefixCount(),
                counts.profile(context.rarity()).suffixCount()
        );

        Optional<MonsterAffixState> rolled = MonsterAffixRoller.rollState(mob.level().getServer(), mob.getType(), context, profile, random);

        if (rolled.isPresent()) {
            MonsterAffixBootstrap.attach(mob, rolled.orElseThrow());
        }
    }
}
