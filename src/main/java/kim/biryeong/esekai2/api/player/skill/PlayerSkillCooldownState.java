package kim.biryeong.esekai2.api.player.skill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;

/**
 * Persistent player cooldown snapshot keyed by skill definition identifier.
 *
 * @param readyGameTimes absolute game time when each tracked skill becomes available again
 */
public record PlayerSkillCooldownState(Map<Identifier, Long> readyGameTimes) {
    /**
     * Codec used by player cooldown saved data.
     */
    public static final Codec<PlayerSkillCooldownState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Identifier.CODEC, Codec.LONG).optionalFieldOf("ready_game_times", Map.of())
                    .forGetter(PlayerSkillCooldownState::readyGameTimes)
    ).apply(instance, PlayerSkillCooldownState::new));

    public PlayerSkillCooldownState {
        Objects.requireNonNull(readyGameTimes, "readyGameTimes");
        readyGameTimes = Map.copyOf(readyGameTimes);
    }

    /**
     * Returns whether the provided skill is still on cooldown at the given absolute game time.
     *
     * @param skillId skill identifier to query
     * @param gameTime current absolute game time
     * @return {@code true} when the skill is still cooling down
     */
    public boolean isOnCooldown(Identifier skillId, long gameTime) {
        Objects.requireNonNull(skillId, "skillId");
        return readyGameTimes.getOrDefault(skillId, 0L) > gameTime;
    }

    /**
     * Returns the absolute ready time for the provided skill when present.
     *
     * @param skillId skill identifier to query
     * @return ready time when tracked
     */
    public OptionalLong readyGameTime(Identifier skillId) {
        Objects.requireNonNull(skillId, "skillId");
        Long ready = readyGameTimes.get(skillId);
        return ready == null ? OptionalLong.empty() : OptionalLong.of(ready);
    }

    /**
     * Returns a copy with expired cooldown entries removed.
     *
     * @param gameTime current absolute game time
     * @return pruned cooldown state
     */
    public PlayerSkillCooldownState pruneExpired(long gameTime) {
        Map<Identifier, Long> pruned = new LinkedHashMap<>();
        for (Map.Entry<Identifier, Long> entry : readyGameTimes.entrySet()) {
            if (entry.getValue() > gameTime) {
                pruned.put(entry.getKey(), entry.getValue());
            }
        }
        return new PlayerSkillCooldownState(pruned);
    }

    /**
     * Returns a copy with one skill cooldown replaced.
     *
     * @param skillId skill identifier to update
     * @param readyGameTime absolute ready time for the skill
     * @return updated cooldown state
     */
    public PlayerSkillCooldownState withCooldown(Identifier skillId, long readyGameTime) {
        Objects.requireNonNull(skillId, "skillId");
        if (readyGameTime < 0L) {
            throw new IllegalArgumentException("readyGameTime must be >= 0");
        }
        Map<Identifier, Long> updated = new LinkedHashMap<>(readyGameTimes);
        updated.put(skillId, readyGameTime);
        return new PlayerSkillCooldownState(updated);
    }
}
