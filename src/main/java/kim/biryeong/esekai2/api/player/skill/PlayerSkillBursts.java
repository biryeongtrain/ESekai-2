package kim.biryeong.esekai2.api.player.skill;

import kim.biryeong.esekai2.impl.player.skill.PlayerSkillBurstService;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;
import java.util.Optional;

/**
 * Public facade for persistent player skill burst state used by {@code times_to_cast}.
 */
public final class PlayerSkillBursts {
    private PlayerSkillBursts() {
    }

    /**
     * Returns the player's current tracked burst state.
     *
     * @param player player whose burst state should be resolved
     * @param gameTime current absolute game time used to prune expired entries
     * @return current burst snapshot
     */
    public static PlayerSkillBurstState get(ServerPlayer player, long gameTime) {
        Objects.requireNonNull(player, "player");
        return PlayerSkillBurstService.get(player, gameTime);
    }

    /**
     * Returns the active burst entry for one skill when present.
     *
     * @param player player whose burst state should be queried
     * @param skillId skill identifier to query
     * @param gameTime current absolute game time used to prune expired entries
     * @return current burst entry for the skill when still active
     */
    public static Optional<PlayerSkillBurstState.SkillBurstEntry> entry(
            ServerPlayer player,
            Identifier skillId,
            long gameTime
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(skillId, "skillId");
        return PlayerSkillBurstService.entry(player, skillId, gameTime);
    }

    /**
     * Compatibility alias for callers that expect the currently active burst entry for one skill.
     *
     * @param player player whose burst state should be queried
     * @param skillId skill identifier to query
     * @param gameTime current absolute game time used to prune expired entries
     * @return current burst entry for the skill when still active
     */
    public static Optional<PlayerSkillBurstState.SkillBurstEntry> activeBurst(
            ServerPlayer player,
            Identifier skillId,
            long gameTime
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(skillId, "skillId");
        return entry(player, skillId, gameTime);
    }

    /**
     * Returns the remaining follow-up casts available for one skill.
     *
     * @param player player whose burst state should be queried
     * @param skillId skill identifier to query
     * @param gameTime current absolute game time used to prune expired entries
     * @return remaining follow-up casts while the burst is active
     */
    public static int remainingCasts(ServerPlayer player, Identifier skillId, long gameTime) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(skillId, "skillId");
        return entry(player, skillId, gameTime)
                .map(PlayerSkillBurstState.SkillBurstEntry::remainingCasts)
                .orElse(0);
    }

    /**
     * Returns whether the provided skill still has an active burst window.
     *
     * @param player player whose burst state should be queried
     * @param skillId skill identifier to query
     * @param gameTime current absolute game time used to prune expired entries
     * @return {@code true} when the skill has an active burst window
     */
    public static boolean hasActiveBurst(ServerPlayer player, Identifier skillId, long gameTime) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(skillId, "skillId");
        return entry(player, skillId, gameTime).isPresent();
    }

    /**
     * Returns whether the provided skill still has an active burst window but no follow-up casts left.
     *
     * @param player player whose burst state should be queried
     * @param skillId skill identifier to query
     * @param gameTime current absolute game time used to prune expired entries
     * @return {@code true} when the skill has an exhausted active burst window
     */
    public static boolean isExhausted(ServerPlayer player, Identifier skillId, long gameTime) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(skillId, "skillId");
        return entry(player, skillId, gameTime)
                .map(entry -> entry.remainingCasts() <= 0)
                .orElse(false);
    }

    /**
     * Returns whether the next cast is allowed as an opener or follow-up for this skill.
     *
     * @param player player whose burst state should be queried
     * @param skillId skill identifier to query
     * @param timesToCast total casts allowed in one burst window, opener included
     * @param gameTime current absolute game time used to prune expired entries
     * @return {@code true} when the next cast is allowed at the current burst state
     */
    public static boolean canCast(ServerPlayer player, Identifier skillId, int timesToCast, long gameTime) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(skillId, "skillId");
        return PlayerSkillBurstService.canCast(player, skillId, timesToCast, gameTime);
    }

    /**
     * Records one successful cast for the provided skill and refreshes its burst window.
     *
     * @param player player whose burst state should be updated
     * @param skillId skill identifier that just executed successfully
     * @param timesToCast total casts allowed in one burst window, opener included
     * @param gameTime current absolute game time
     * @param expiresAtGameTime absolute game time when the refreshed burst window should close
     * @return updated burst snapshot
     */
    public static PlayerSkillBurstState recordSuccessfulCast(
            ServerPlayer player,
            Identifier skillId,
            int timesToCast,
            long gameTime,
            long expiresAtGameTime
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(skillId, "skillId");
        return PlayerSkillBurstService.recordSuccessfulCast(player, skillId, timesToCast, gameTime, expiresAtGameTime);
    }
}
