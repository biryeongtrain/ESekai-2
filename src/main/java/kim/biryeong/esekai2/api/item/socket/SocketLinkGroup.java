package kim.biryeong.esekai2.api.item.socket;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Objects;

/**
 * Grouping metadata for linked sockets on one item.
 *
 * @param groupIndex group identifier in stable item state order
 * @param socketIndices socket positions that are linked together
 */
public record SocketLinkGroup(
        int groupIndex,
        List<Integer> socketIndices
) {
    /**
     * Codec used for datapack-driven and persisted socket link groups.
     */
    public static final Codec<SocketLinkGroup> CODEC = RecordCodecBuilder.<SocketLinkGroup>create(instance -> instance.group(
            Codec.INT.fieldOf("group_index").forGetter(SocketLinkGroup::groupIndex),
            Codec.list(Codec.INT).fieldOf("socket_indices").forGetter(SocketLinkGroup::socketIndices)
    ).apply(instance, (groupIndex, socketIndices) -> new SocketLinkGroup(groupIndex, socketIndices))).validate(SocketLinkGroup::validate);

    public SocketLinkGroup {
        if (groupIndex < 0) {
            throw new IllegalArgumentException("groupIndex must be greater than or equal to 0");
        }

        Objects.requireNonNull(socketIndices, "socketIndices");
        socketIndices = List.copyOf(socketIndices);
        for (Integer socketIndex : socketIndices) {
            Objects.requireNonNull(socketIndex, "socketIndices entry");
            if (socketIndex < 0) {
                throw new IllegalArgumentException("socketIndices entries must be greater than or equal to 0");
            }
        }
    }

    private static DataResult<SocketLinkGroup> validate(SocketLinkGroup group) {
        if (group.socketIndices.isEmpty()) {
            return DataResult.error(() -> "socket_indices must not be empty");
        }

        if (group.socketIndices.size() != group.socketIndices.stream().distinct().count()) {
            return DataResult.error(() -> "socket_indices must not contain duplicate sockets");
        }

        return DataResult.success(group);
    }
}
