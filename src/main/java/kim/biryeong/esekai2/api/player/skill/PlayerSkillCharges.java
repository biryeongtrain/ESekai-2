package kim.biryeong.esekai2.api.player.skill;

import kim.biryeong.esekai2.impl.player.skill.PlayerSkillChargeService;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;
import java.util.Optional;

/**
 * Public facade for persistent player skill charge state.
 */
public final class PlayerSkillCharges {
    private PlayerSkillCharges() {
    }

    /**
     * Returns the player's resolved charge entry for one skill.
     *
     * @param player player whose charge state should be resolved
     * @param skillId skill identifier to resolve
     * @param maxCharges configured maximum charges
     * @param gameTime current absolute game time used to lazily resolve pending recharge
     * @return resolved per-skill charge entry
     */
    public static PlayerSkillChargeState.SkillChargeEntry get(
            ServerPlayer player,
            Identifier skillId,
            int maxCharges,
            long gameTime
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(skillId, "skillId");
        return PlayerSkillChargeService.get(player, skillId, maxCharges, gameTime);
    }

    /**
     * Returns the currently available charges for one skill.
     *
     * @param player player whose charges should be queried
     * @param skillId skill identifier to query
     * @param maxCharges configured maximum charges
     * @param gameTime current absolute game time used to lazily resolve pending recharge
     * @return currently available charges
     */
    public static int availableCharges(
            ServerPlayer player,
            Identifier skillId,
            int maxCharges,
            long gameTime
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(skillId, "skillId");
        return PlayerSkillChargeService.availableCharges(player, skillId, maxCharges, gameTime);
    }

    /**
     * Compatibility alias for legacy callers that still ask for current charges.
     *
     * @param player player whose charges should be queried
     * @param skillId skill identifier to query
     * @param maxCharges configured maximum charges
     * @param gameTime current absolute game time used to lazily resolve pending recharge
     * @return currently available charges
     */
    public static int currentCharges(
            ServerPlayer player,
            Identifier skillId,
            int maxCharges,
            long gameTime
    ) {
        return availableCharges(player, skillId, maxCharges, gameTime);
    }

    /**
     * Replaces the currently available charges for one skill and clears pending recharge for that skill.
     *
     * @param player player whose charge state should be updated
     * @param skillId skill identifier to update
     * @param currentCharges replacement current charges
     * @param maxCharges configured maximum charges
     * @param gameTime current absolute game time used for normalization
     * @return updated charge snapshot
     */
    public static PlayerSkillChargeState setAvailableCharges(
            ServerPlayer player,
            Identifier skillId,
            int currentCharges,
            int maxCharges,
            long gameTime
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(skillId, "skillId");
        return PlayerSkillChargeService.setAvailableCharges(player, skillId, currentCharges, maxCharges, gameTime);
    }

    /**
     * Attempts to consume one charge and enqueue its recharge completion time.
     *
     * @param player player whose charge state should be updated
     * @param skillId skill identifier to consume from
     * @param maxCharges configured maximum charges
     * @param gameTime current absolute game time used to lazily resolve pending recharge first
     * @param chargeReadyGameTime absolute game time when the consumed charge becomes available again
     * @return updated resolved charge entry when a charge was available
     */
    public static Optional<PlayerSkillChargeState.SkillChargeEntry> consume(
            ServerPlayer player,
            Identifier skillId,
            int maxCharges,
            long gameTime,
            long chargeReadyGameTime
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(skillId, "skillId");
        return PlayerSkillChargeService.consume(player, skillId, maxCharges, gameTime, chargeReadyGameTime);
    }

    /**
     * Compatibility alias for legacy callers that provide recharge duration instead of an absolute ready time.
     *
     * @param player player whose charge state should be updated
     * @param skillId skill identifier to consume from
     * @param maxCharges configured maximum charges
     * @param rechargeTicks ticks until the consumed charge becomes available again
     * @param gameTime current absolute game time used to lazily resolve pending recharge first
     * @return updated player charge state when a charge was available
     */
    public static Optional<PlayerSkillChargeState> spendCharge(
            ServerPlayer player,
            Identifier skillId,
            int maxCharges,
            long rechargeTicks,
            long gameTime
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(skillId, "skillId");
        long chargeReadyGameTime = Math.max(0L, gameTime + Math.max(0L, rechargeTicks));
        Optional<PlayerSkillChargeState.SkillChargeEntry> consumed = consume(player, skillId, maxCharges, gameTime, chargeReadyGameTime);
        if (consumed.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(PlayerSkillChargeService.snapshot(player));
    }
}
