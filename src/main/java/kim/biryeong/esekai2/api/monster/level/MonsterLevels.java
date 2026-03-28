package kim.biryeong.esekai2.api.monster.level;

import kim.biryeong.esekai2.impl.monster.level.MonsterLevelResolver;
import net.minecraft.server.MinecraftServer;

/**
 * Public entry points for resolving monster level scaling and derived item levels.
 */
public final class MonsterLevels {
    private MonsterLevels() {
    }

    /**
     * Resolves the runtime monster level profile for the provided context.
     *
     * @param server active server providing registry access
     * @param context monster level context to resolve
     * @return resolved monster level profile
     */
    public static MonsterLevelProfile resolveProfile(MinecraftServer server, MonsterLevelContext context) {
        return MonsterLevelResolver.resolveProfile(server, context);
    }

    /**
     * Resolves the derived item level for drops produced by the provided monster context.
     *
     * @param context monster context used to derive dropped item level
     * @return derived item level clamped into the shared range
     */
    public static int resolveDroppedItemLevel(MonsterLevelContext context) {
        return MonsterLevelResolver.resolveDroppedItemLevel(context);
    }
}
