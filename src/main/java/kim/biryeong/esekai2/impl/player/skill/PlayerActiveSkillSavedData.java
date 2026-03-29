package kim.biryeong.esekai2.impl.player.skill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kim.biryeong.esekai2.api.player.skill.SelectedActiveSkillRef;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistent server-side storage of selected active skill references keyed by player UUID.
 */
public final class PlayerActiveSkillSavedData extends SavedData {
    private static final Codec<Map<UUID, SelectedActiveSkillRef>> STATE_MAP_CODEC =
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, SelectedActiveSkillRef.CODEC);

    private static final Codec<PlayerActiveSkillSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            STATE_MAP_CODEC.optionalFieldOf("players", Map.of()).forGetter(PlayerActiveSkillSavedData::states)
    ).apply(instance, PlayerActiveSkillSavedData::new));

    public static final SavedDataType<PlayerActiveSkillSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath("esekai2", "player_selected_active_skills"),
            PlayerActiveSkillSavedData::new,
            CODEC,
            DataFixTypes.LEVEL
    );

    private final Map<UUID, SelectedActiveSkillRef> states;

    public PlayerActiveSkillSavedData() {
        this(Map.of());
    }

    public PlayerActiveSkillSavedData(Map<UUID, SelectedActiveSkillRef> states) {
        Objects.requireNonNull(states, "states");
        this.states = new LinkedHashMap<>(states);
    }

    public Map<UUID, SelectedActiveSkillRef> states() {
        return Map.copyOf(states);
    }

    public Optional<SelectedActiveSkillRef> get(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        return Optional.ofNullable(states.get(playerId));
    }

    public void put(UUID playerId, SelectedActiveSkillRef state) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(state, "state");
        states.put(playerId, state);
        setDirty();
    }

    public Optional<SelectedActiveSkillRef> remove(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        SelectedActiveSkillRef removed = states.remove(playerId);
        if (removed != null) {
            setDirty();
        }
        return Optional.ofNullable(removed);
    }
}
