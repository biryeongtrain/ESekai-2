package kim.biryeong.esekai2.api.player.skill;

import kim.biryeong.esekai2.impl.player.skill.PlayerActiveSkillService;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;
import java.util.Optional;

/**
 * Public entry points for interacting with server-side selected active skill state.
 */
public final class PlayerActiveSkills {
    private PlayerActiveSkills() {
    }

    /**
     * Returns the currently selected active skill reference for the player.
     *
     * @param player player to inspect
     * @return selected active skill reference when present
     */
    public static Optional<SelectedActiveSkillRef> get(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        return PlayerActiveSkillService.get(player);
    }

    /**
     * Stores a new selected active skill reference for the player.
     *
     * @param player player to update
     * @param selectedSkill selected active skill reference
     * @return stored selection
     */
    public static SelectedActiveSkillRef select(ServerPlayer player, SelectedActiveSkillRef selectedSkill) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(selectedSkill, "selectedSkill");
        return PlayerActiveSkillService.select(player, selectedSkill);
    }

    /**
     * Clears the selected active skill reference from the player.
     *
     * @param player player to update
     * @return removed selection when one existed
     */
    public static Optional<SelectedActiveSkillRef> clear(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        return PlayerActiveSkillService.clear(player);
    }
}
