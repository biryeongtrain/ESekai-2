package kim.biryeong.esekai2.api.item.socket;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.Objects;

/**
 * Reference stored in a socket position.
 *
 * @param socketIndex zero-based socket position on the item
 * @param type content category loaded into this socket
 * @param definitionId target definition identifier (skill or support reference)
 */
public record SocketedSkillRef(
        int socketIndex,
        SocketSlotType type,
        Identifier definitionId
) {
    /**
     * Codec used to persist socket references.
     */
    public static final Codec<SocketedSkillRef> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("socket_index").forGetter(SocketedSkillRef::socketIndex),
            SocketSlotType.CODEC.fieldOf("type").forGetter(SocketedSkillRef::type),
            Identifier.CODEC.fieldOf("definition_id").forGetter(SocketedSkillRef::definitionId)
    ).apply(instance, (index, type, id) -> new SocketedSkillRef(index, type, id)));

    public SocketedSkillRef {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(definitionId, "definitionId");
        if (socketIndex < 0) {
            throw new IllegalArgumentException("socketIndex must be greater than or equal to 0");
        }
    }

    /**
     * Returns whether this ref points to a skill definition.
     *
     * @return {@code true} for skill socket entries
     */
    public boolean isSkill() {
        return type == SocketSlotType.SKILL;
    }

    /**
     * Returns whether this ref points to a support definition.
     *
     * @return {@code true} for support socket entries
     */
    public boolean isSupport() {
        return type == SocketSlotType.SUPPORT;
    }
}
