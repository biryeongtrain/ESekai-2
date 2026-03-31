package kim.biryeong.esekai2.api.player.skill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Persistent player burst snapshot keyed by skill definition identifier.
 *
 * @param skills active burst state tracked for each skill definition
 */
public record PlayerSkillBurstState(Map<Identifier, SkillBurstEntry> skills) {
    /**
     * Codec used by player burst saved data.
     */
    public static final Codec<PlayerSkillBurstState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Identifier.CODEC, SkillBurstEntry.CODEC).optionalFieldOf("skills", Map.of())
                    .forGetter(PlayerSkillBurstState::skills)
    ).apply(instance, PlayerSkillBurstState::new));

    public PlayerSkillBurstState {
        Objects.requireNonNull(skills, "skills");
        skills = Map.copyOf(skills);
    }

    /**
     * Returns the current tracked entry for one skill when present.
     *
     * @param skillId skill identifier to inspect
     * @return tracked burst entry
     */
    public Optional<SkillBurstEntry> entry(Identifier skillId) {
        Objects.requireNonNull(skillId, "skillId");
        return Optional.ofNullable(skills.get(skillId));
    }

    /**
     * Returns a copy with expired burst entries removed.
     *
     * @param gameTime current absolute game time
     * @return pruned burst state
     */
    public PlayerSkillBurstState pruneExpired(long gameTime) {
        Map<Identifier, SkillBurstEntry> pruned = new LinkedHashMap<>();
        for (Map.Entry<Identifier, SkillBurstEntry> entry : skills.entrySet()) {
            if (entry.getValue().expiresAtGameTime() > gameTime) {
                pruned.put(entry.getKey(), entry.getValue());
            }
        }
        return new PlayerSkillBurstState(pruned);
    }

    /**
     * Returns a copy with all entries removed except the provided skill identifier.
     *
     * @param skillId skill identifier to preserve
     * @return updated burst snapshot containing at most one entry
     */
    public PlayerSkillBurstState clearExcept(Identifier skillId) {
        Objects.requireNonNull(skillId, "skillId");
        SkillBurstEntry preserved = skills.get(skillId);
        if (preserved == null) {
            return new PlayerSkillBurstState(Map.of());
        }
        return new PlayerSkillBurstState(Map.of(skillId, preserved));
    }

    /**
     * Returns a copy with one skill entry replaced or removed.
     *
     * @param skillId skill identifier to update
     * @param entry replacement burst entry, or {@code null} to remove the key
     * @return updated burst snapshot
     */
    public PlayerSkillBurstState withEntry(Identifier skillId, SkillBurstEntry entry) {
        Objects.requireNonNull(skillId, "skillId");
        Map<Identifier, SkillBurstEntry> updated = new LinkedHashMap<>(skills);
        if (entry == null) {
            updated.remove(skillId);
        } else {
            updated.put(skillId, entry);
        }
        return new PlayerSkillBurstState(updated);
    }

    /**
     * One per-skill burst snapshot.
     *
     * @param remainingCasts remaining follow-up casts available before the burst exhausts
     * @param expiresAtGameTime absolute game time when the burst window closes
     */
    public record SkillBurstEntry(int remainingCasts, long expiresAtGameTime) {
        /**
         * Codec used by player burst saved data.
         */
        public static final Codec<SkillBurstEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("remaining_casts").forGetter(SkillBurstEntry::remainingCasts),
                Codec.LONG.fieldOf("expires_at_game_time").forGetter(SkillBurstEntry::expiresAtGameTime)
        ).apply(instance, SkillBurstEntry::new));

        public SkillBurstEntry {
            if (remainingCasts < 0) {
                throw new IllegalArgumentException("remainingCasts must be >= 0");
            }
            if (expiresAtGameTime < 0L) {
                throw new IllegalArgumentException("expiresAtGameTime must be >= 0");
            }
        }
    }
}
