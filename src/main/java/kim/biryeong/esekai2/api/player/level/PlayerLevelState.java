package kim.biryeong.esekai2.api.player.level;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kim.biryeong.esekai2.api.level.LevelRules;

/**
 * Persistent player level state stored by ESekai's level progression system.
 *
 * @param level current player level
 * @param experienceInLevel experience accumulated inside the current level
 * @param totalExperience total lifetime experience recorded by the system
 */
public record PlayerLevelState(
        int level,
        long experienceInLevel,
        long totalExperience
) {
    private static final Codec<PlayerLevelState> BASE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("level").forGetter(PlayerLevelState::level),
            Codec.LONG.fieldOf("experience_in_level").forGetter(PlayerLevelState::experienceInLevel),
            Codec.LONG.fieldOf("total_experience").forGetter(PlayerLevelState::totalExperience)
    ).apply(instance, PlayerLevelState::new));

    /**
     * Validated codec used to decode player level state from saved data and test fixtures.
     */
    public static final Codec<PlayerLevelState> CODEC = BASE_CODEC.validate(PlayerLevelState::validate);

    /**
     * Default starting state used for new players.
     */
    public static final PlayerLevelState DEFAULT = new PlayerLevelState(LevelRules.MIN_LEVEL, 0L, 0L);

    public PlayerLevelState {
        LevelRules.requireValidLevel(level, "level");
        if (experienceInLevel < 0L) {
            throw new IllegalArgumentException("experienceInLevel must be greater than or equal to 0");
        }
        if (totalExperience < 0L) {
            throw new IllegalArgumentException("totalExperience must be greater than or equal to 0");
        }
        if (level == LevelRules.MAX_LEVEL && experienceInLevel != 0L) {
            throw new IllegalArgumentException("level 100 must keep experienceInLevel equal to 0");
        }
    }

    private static DataResult<PlayerLevelState> validate(PlayerLevelState state) {
        if (!LevelRules.isValidLevel(state.level())) {
            return DataResult.error(() -> "level must be between 1 and 100");
        }

        if (state.experienceInLevel() < 0L) {
            return DataResult.error(() -> "experience_in_level must be greater than or equal to 0");
        }

        if (state.totalExperience() < 0L) {
            return DataResult.error(() -> "total_experience must be greater than or equal to 0");
        }

        if (state.level() == LevelRules.MAX_LEVEL && state.experienceInLevel() != 0L) {
            return DataResult.error(() -> "level 100 must keep experience_in_level equal to 0");
        }

        return DataResult.success(state);
    }
}
