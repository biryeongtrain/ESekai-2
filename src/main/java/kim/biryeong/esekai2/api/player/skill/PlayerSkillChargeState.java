package kim.biryeong.esekai2.api.player.skill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Persistent player charge snapshot keyed by skill definition identifier.
 *
 * @param skills charge state tracked for each skill definition
 */
public record PlayerSkillChargeState(Map<Identifier, SkillChargeEntry> skills) {
    /**
     * Codec used by player charge saved data.
     */
    public static final Codec<PlayerSkillChargeState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Identifier.CODEC, SkillChargeEntry.CODEC).optionalFieldOf("skills", Map.of())
                    .forGetter(PlayerSkillChargeState::skills)
    ).apply(instance, PlayerSkillChargeState::new));

    public PlayerSkillChargeState {
        Objects.requireNonNull(skills, "skills");
        skills = Map.copyOf(skills);
    }

    /**
     * Returns the current tracked entry for one skill when present.
     *
     * @param skillId skill identifier to inspect
     * @return current tracked entry
     */
    public Optional<SkillChargeEntry> entry(Identifier skillId) {
        Objects.requireNonNull(skillId, "skillId");
        return Optional.ofNullable(skills.get(skillId));
    }

    /**
     * Returns a copy with one skill entry replaced or removed.
     *
     * @param skillId skill identifier to update
     * @param entry replacement charge entry, or {@code null} to remove the key
     * @return updated charge snapshot
     */
    public PlayerSkillChargeState withEntry(Identifier skillId, SkillChargeEntry entry) {
        Objects.requireNonNull(skillId, "skillId");
        Map<Identifier, SkillChargeEntry> updated = new LinkedHashMap<>(skills);
        if (entry == null) {
            updated.remove(skillId);
        } else {
            updated.put(skillId, entry);
        }
        return new PlayerSkillChargeState(updated);
    }

    /**
     * One per-skill charge snapshot.
     *
     * @param currentCharges currently available charges
     * @param pendingReadyGameTimes absolute ready times for each consumed charge that is still recharging
     */
    public record SkillChargeEntry(int currentCharges, List<Long> pendingReadyGameTimes) {
        /**
         * Codec used by player charge saved data.
         */
        public static final Codec<SkillChargeEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("current_charges").forGetter(entry -> entry.currentCharges),
                Codec.LONG.listOf().optionalFieldOf("pending_ready_game_times", List.of())
                        .forGetter(entry -> entry.pendingReadyGameTimes)
        ).apply(instance, SkillChargeEntry::new));

        public SkillChargeEntry {
            Objects.requireNonNull(pendingReadyGameTimes, "pendingReadyGameTimes");
            pendingReadyGameTimes = List.copyOf(pendingReadyGameTimes);
        }

        /**
         * Resolves this charge entry against current runtime constraints.
         *
         * @param maxCharges configured maximum charges for the skill
         * @param gameTime current absolute game time
         * @return normalized charge entry with matured recharges applied
         */
        public SkillChargeEntry resolve(int maxCharges, long gameTime) {
            int sanitizedMaxCharges = Math.max(0, maxCharges);
            if (sanitizedMaxCharges == 0) {
                return new SkillChargeEntry(0, List.of());
            }

            int resolvedCurrent = Math.max(0, currentCharges);
            List<Long> pending = new ArrayList<>();
            for (Long readyGameTime : pendingReadyGameTimes) {
                if (readyGameTime == null) {
                    continue;
                }
                long ready = Math.max(0L, readyGameTime);
                if (ready <= gameTime) {
                    resolvedCurrent = Math.min(sanitizedMaxCharges, resolvedCurrent + 1);
                } else {
                    pending.add(ready);
                }
            }

            pending.sort(Long::compareTo);
            resolvedCurrent = Math.min(sanitizedMaxCharges, resolvedCurrent);
            int allowedPending = Math.max(0, sanitizedMaxCharges - resolvedCurrent);
            if (pending.size() > allowedPending) {
                pending = new ArrayList<>(pending.subList(0, allowedPending));
            }
            return new SkillChargeEntry(resolvedCurrent, List.copyOf(pending));
        }

        /**
         * Returns a copy with one consumed charge scheduled to recharge.
         *
         * @param maxCharges configured maximum charges
         * @param rechargeReadyGameTime absolute ready time for the consumed charge
         * @param gameTime current absolute game time
         * @return updated charge entry
         */
        public SkillChargeEntry consume(int maxCharges, long rechargeReadyGameTime, long gameTime) {
            SkillChargeEntry resolved = resolve(maxCharges, gameTime);
            if (resolved.currentCharges() <= 0) {
                return resolved;
            }

            List<Long> updatedPending = new ArrayList<>(resolved.pendingReadyGameTimes());
            updatedPending.add(Math.max(0L, rechargeReadyGameTime));
            updatedPending.sort(Long::compareTo);
            return new SkillChargeEntry(resolved.currentCharges() - 1, List.copyOf(updatedPending)).resolve(maxCharges, gameTime);
        }
    }
}
