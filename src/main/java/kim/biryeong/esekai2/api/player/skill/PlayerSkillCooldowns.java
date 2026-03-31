package kim.biryeong.esekai2.api.player.skill;

import kim.biryeong.esekai2.impl.player.skill.PlayerSkillCooldownService;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;
import java.util.OptionalLong;

/**
 * Public facade for persistent player skill cooldown state.
 */
public final class PlayerSkillCooldowns {
    private PlayerSkillCooldowns() {
    }

    /**
     * Returns the player's current tracked cooldown state.
     *
     * @param player player whose cooldown state should be resolved
     * @param gameTime current absolute game time used to prune expired entries
     * @return current cooldown snapshot
     */
    public static PlayerSkillCooldownState get(ServerPlayer player, long gameTime) {
        return PlayerSkillCooldownService.get(player, gameTime);
    }

    /**
     * Returns whether the provided skill is cooling down for the player.
     *
     * @param player player whose cooldown state should be queried
     * @param skillId skill identifier to query
     * @param gameTime current absolute game time
     * @return {@code true} when the skill is still cooling down
     */
    public static boolean isOnCooldown(ServerPlayer player, Identifier skillId, long gameTime) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(skillId, "skillId");
        return PlayerSkillCooldownService.isOnCooldown(player, skillId, gameTime);
    }

    /**
     * Returns whether the provided skill is cooling down for the player.
     *
     * @param player player whose cooldown state should be queried
     * @param skillId serialized skill identifier to query
     * @param gameTime current absolute game time
     * @return {@code true} when the skill is still cooling down
     */
    public static boolean isOnCooldown(ServerPlayer player, String skillId, long gameTime) {
        Objects.requireNonNull(skillId, "skillId");
        Identifier parsed = Identifier.tryParse(skillId);
        return parsed != null && isOnCooldown(player, parsed, gameTime);
    }

    /**
     * Returns the absolute ready time for one tracked skill when present.
     *
     * @param player player whose cooldown state should be queried
     * @param skillId skill identifier to query
     * @param gameTime current absolute game time used to prune expired entries
     * @return ready time when tracked
     */
    public static OptionalLong readyGameTime(ServerPlayer player, Identifier skillId, long gameTime) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(skillId, "skillId");
        return PlayerSkillCooldownService.readyGameTime(player, skillId, gameTime);
    }

    /**
     * Returns remaining cooldown ticks for one tracked skill.
     *
     * @param player player whose cooldown state should be queried
     * @param skillId skill identifier to query
     * @param gameTime current absolute game time
     * @return remaining cooldown ticks, or {@code 0} when not cooling down
     */
    public static long remainingTicks(ServerPlayer player, Identifier skillId, long gameTime) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(skillId, "skillId");
        return PlayerSkillCooldownService.readyGameTime(player, skillId, gameTime)
                .stream()
                .map(readyGameTime -> Math.max(0L, readyGameTime - gameTime))
                .findFirst()
                .orElse(0L);
    }

    /**
     * Returns remaining cooldown ticks for one tracked skill.
     *
     * @param player player whose cooldown state should be queried
     * @param skillId serialized skill identifier to query
     * @param gameTime current absolute game time
     * @return remaining cooldown ticks, or {@code 0} when not cooling down
     */
    public static long remainingTicks(ServerPlayer player, String skillId, long gameTime) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(skillId, "skillId");
        Identifier parsed = Identifier.tryParse(skillId);
        if (parsed == null) {
            return 0L;
        }
        return remainingTicks(player, parsed, gameTime);
    }

    /**
     * Starts or replaces the cooldown for one skill.
     *
     * @param player player whose cooldown should be updated
     * @param skillId skill identifier to update
     * @param readyGameTime absolute ready time for the skill
     * @return updated cooldown snapshot
     */
    public static PlayerSkillCooldownState start(ServerPlayer player, Identifier skillId, long readyGameTime) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(skillId, "skillId");
        return PlayerSkillCooldownService.start(player, skillId, readyGameTime);
    }

    /**
     * Starts or replaces the cooldown for one skill.
     *
     * @param player player whose cooldown should be updated
     * @param skillId serialized skill identifier to update
     * @param readyGameTime absolute ready time for the skill
     * @return updated cooldown snapshot
     */
    public static PlayerSkillCooldownState startCooldown(ServerPlayer player, String skillId, long readyGameTime) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(skillId, "skillId");
        Identifier parsed = Identifier.tryParse(skillId);
        if (parsed == null) {
            return get(player, readyGameTime);
        }
        return PlayerSkillCooldownService.start(player, parsed, readyGameTime);
    }
}
