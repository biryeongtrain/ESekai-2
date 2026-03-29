package kim.biryeong.esekai2.api.player.skill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kim.biryeong.esekai2.api.item.socket.SocketedEquipmentSlot;
import net.minecraft.resources.Identifier;

import java.util.Objects;

/**
 * Persistent selection reference for one equipped active skill.
 *
 * @param equipmentSlot equipped item source that owns the selected active skill
 * @param skillId selected active skill identifier
 */
public record SelectedActiveSkillRef(
        SocketedEquipmentSlot equipmentSlot,
        Identifier skillId
) {
    /**
     * Codec used by server-side persistence.
     */
    public static final Codec<SelectedActiveSkillRef> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SocketedEquipmentSlot.CODEC.fieldOf("equipment_slot").forGetter(SelectedActiveSkillRef::equipmentSlot),
            Identifier.CODEC.fieldOf("skill_id").forGetter(SelectedActiveSkillRef::skillId)
    ).apply(instance, SelectedActiveSkillRef::new));

    public SelectedActiveSkillRef {
        Objects.requireNonNull(equipmentSlot, "equipmentSlot");
        Objects.requireNonNull(skillId, "skillId");
    }
}
