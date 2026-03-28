package kim.biryeong.esekai2.impl.monster.level;

import kim.biryeong.esekai2.api.level.LevelRules;
import kim.biryeong.esekai2.api.monster.level.*;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;

import java.util.Objects;

/**
 * Shared internal helper for resolving monster level table rows into runtime profiles.
 */
public final class MonsterLevelResolver {
    private static final Identifier DEFAULT_MONSTER_LEVEL_ID = Identifier.fromNamespaceAndPath("esekai2", "default");

    private MonsterLevelResolver() {
    }

    public static MonsterLevelProfile resolveProfile(MinecraftServer server, MonsterLevelContext context) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(context, "context");

        MonsterLevelEntry entry = definition(server).entry(context.level());
        double lifeMultiplier = 1.0 + rarityLifeBonusPercent(entry, context.rarity()) / 100.0;
        double damageMultiplier = 1.0;
        double bossItemQuantityBonus = 0.0;
        double bossItemRarityBonus = 0.0;

        if (context.mapMonster()) {
            lifeMultiplier *= 1.0 + entry.mapLifeBonusPercent() / 100.0;
            damageMultiplier += entry.mapDamageBonusPercent() / 100.0;

            if (context.bossMonster()) {
                lifeMultiplier *= 1.0 + entry.bossLifeBonusPercent() / 100.0;
                damageMultiplier += entry.bossDamageBonusPercent() / 100.0;
                bossItemQuantityBonus = entry.bossItemQuantityBonusPercent();
                bossItemRarityBonus = entry.bossItemRarityBonusPercent();
            }
        }

        return new MonsterLevelProfile(
                context.level(),
                entry.damage(),
                entry.life(),
                entry.summonLife(),
                entry.accuracyRating(),
                entry.evasionRating(),
                lifeMultiplier,
                damageMultiplier,
                entry.experiencePoints(),
                resolveDroppedItemLevel(context),
                bossItemQuantityBonus,
                bossItemRarityBonus
        );
    }

    public static int resolveDroppedItemLevel(MonsterLevelContext context) {
        Objects.requireNonNull(context, "context");
        int offset = switch (context.rarity()) {
            case NORMAL -> 0;
            case MAGIC -> 1;
            case RARE, UNIQUE -> 2;
        };
        return LevelRules.clamp(context.level() + offset);
    }

    private static MonsterLevelDefinition definition(MinecraftServer server) {
        Registry<MonsterLevelDefinition> registry = server.registryAccess().lookupOrThrow(MonsterLevelRegistries.MONSTER_LEVEL);
        return registry.getOptional(DEFAULT_MONSTER_LEVEL_ID)
                .orElseThrow(() -> new IllegalStateException("Missing monster level definition: " + DEFAULT_MONSTER_LEVEL_ID));
    }

    private static double rarityLifeBonusPercent(MonsterLevelEntry entry, MonsterRarity rarity) {
        return switch (rarity) {
            case NORMAL, UNIQUE -> 0.0;
            case MAGIC -> entry.magicLifeBonusPercent();
            case RARE -> entry.rareLifeBonusPercent();
        };
    }
}
