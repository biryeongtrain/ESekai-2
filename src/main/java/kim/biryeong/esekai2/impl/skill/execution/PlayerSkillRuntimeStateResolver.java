package kim.biryeong.esekai2.impl.skill.execution;

import kim.biryeong.esekai2.api.player.skill.PlayerSkillBursts;
import kim.biryeong.esekai2.api.player.skill.PlayerSkillCharges;
import kim.biryeong.esekai2.api.player.skill.PlayerSkillCooldowns;
import kim.biryeong.esekai2.api.skill.execution.SkillExecutionContext;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;

/**
 * Shared read-only resolver for player-bound runtime skill state.
 */
public final class PlayerSkillRuntimeStateResolver {
    private PlayerSkillRuntimeStateResolver() {
    }

    public static long cooldownRemainingTicks(SkillExecutionContext context) {
        Objects.requireNonNull(context, "context");
        ServerPlayer player = resolvePlayer(context);
        Identifier skillId = resolveSkillId(context);
        if (player == null || skillId == null) {
            return 0L;
        }
        return PlayerSkillCooldowns.remainingTicks(player, skillId, context.level().getGameTime());
    }

    public static int availableCharges(SkillExecutionContext context) {
        Objects.requireNonNull(context, "context");
        ServerPlayer player = resolvePlayer(context);
        Identifier skillId = resolveSkillId(context);
        int maxCharges = Math.max(0, context.preparedUse().skill().config().charges());
        if (player == null || skillId == null || maxCharges <= 0) {
            return 0;
        }
        return PlayerSkillCharges.availableCharges(player, skillId, maxCharges, context.level().getGameTime());
    }

    public static int burstRemaining(SkillExecutionContext context) {
        Objects.requireNonNull(context, "context");
        ServerPlayer player = resolvePlayer(context);
        Identifier skillId = resolveSkillId(context);
        int timesToCast = Math.max(1, context.preparedUse().skill().config().timesToCast());
        if (player == null || skillId == null || timesToCast <= 1) {
            return 0;
        }
        return PlayerSkillBursts.remainingCasts(player, skillId, context.level().getGameTime());
    }

    private static ServerPlayer resolvePlayer(SkillExecutionContext context) {
        if (context.caster() instanceof ServerPlayer casterPlayer) {
            return casterPlayer;
        }
        if (context.source() instanceof ServerPlayer sourcePlayer) {
            return sourcePlayer;
        }
        return null;
    }

    private static Identifier resolveSkillId(SkillExecutionContext context) {
        return Identifier.tryParse(context.preparedUse().skill().identifier());
    }
}
