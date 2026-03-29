package kim.biryeong.esekai2.api.item.socket;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Runtime socket state persisted on a socket-enabled item stack.
 *
 * @param activeSkill optional active skill configured for this item
 * @param socketCount number of socket slots present on the item
 * @param socketLinkGroups linked socket groups (for support chain metadata)
 * @param socketRefs concrete socket content assignments
 */
public record SocketedSkillItemState(
        Optional<Identifier> activeSkill,
        int socketCount,
        List<SocketLinkGroup> socketLinkGroups,
        List<SocketedSkillRef> socketRefs
) {
    /**
     * Stable empty socket state used for missing component cases.
     */
    public static final SocketedSkillItemState EMPTY = new SocketedSkillItemState(
            Optional.empty(),
            0,
            List.of(),
            List.of()
    );

    /**
     * Codec used by persistence and network payloads for item components.
     */
    public static final Codec<SocketedSkillItemState> BASE_CODEC = RecordCodecBuilder.<SocketedSkillItemState>create(instance -> instance.group(
            Identifier.CODEC.optionalFieldOf("active_skill").forGetter(SocketedSkillItemState::activeSkill),
            Codec.INT.fieldOf("socket_count").forGetter(SocketedSkillItemState::socketCount),
            Codec.list(SocketLinkGroup.CODEC).optionalFieldOf("socket_link_groups", List.of())
                    .forGetter(SocketedSkillItemState::socketLinkGroups),
            Codec.list(SocketedSkillRef.CODEC).optionalFieldOf("socket_refs", List.of())
                    .forGetter(SocketedSkillItemState::socketRefs)
    ).apply(instance, (activeSkill, socketCount, socketLinkGroups, socketRefs) -> new SocketedSkillItemState(
            activeSkill,
            socketCount,
            socketLinkGroups,
            socketRefs
    )));

    /**
     * Public component codec alias.
     */
    public static final Codec<SocketedSkillItemState> CODEC = BASE_CODEC;

    public SocketedSkillItemState {
        if (socketCount < 0) {
            throw new IllegalArgumentException("socketCount must be greater than or equal to 0");
        }

        Objects.requireNonNull(activeSkill, "activeSkill");
        Objects.requireNonNull(socketLinkGroups, "socketLinkGroups");
        socketLinkGroups = List.copyOf(socketLinkGroups);
        for (SocketLinkGroup group : socketLinkGroups) {
            Objects.requireNonNull(group, "socketLinkGroups entry");
        }

        Objects.requireNonNull(socketRefs, "socketRefs");
        socketRefs = List.copyOf(socketRefs);
        for (SocketedSkillRef socketRef : socketRefs) {
            Objects.requireNonNull(socketRef, "socketRefs entry");
            validateSocketRefIndex(socketRef.socketIndex(), "socketRef", socketCount);
        }

        Set<Integer> linkedSocketIndices = socketLinkGroups.stream()
                .flatMap(group -> group.socketIndices().stream())
                .collect(Collectors.toSet());
        for (int socketIndex : linkedSocketIndices) {
            validateSocketRefIndex(socketIndex, "linked socket index", socketCount);
        }

        int uniqueLinkedSocketCount = linkedSocketIndices.size();
        int totalLinkedSockets = socketLinkGroups.stream()
                .mapToInt(group -> group.socketIndices().size())
                .sum();
        if (totalLinkedSockets != uniqueLinkedSocketCount) {
            throw new IllegalArgumentException("linked socket indices must be unique across link groups");
        }
    }

    private static void validateSocketRefIndex(int socketIndex, String description, int socketCount) {
        if (socketIndex < 0) {
            throw new IllegalArgumentException(description + " must be greater than or equal to 0");
        }

        if (socketIndex >= socketCount) {
            throw new IllegalArgumentException(description + " must be less than socketCount");
        }
    }

    /**
     * Returns whether a socket index has at least one assignment.
     *
     * @param socketIndex socket slot index to inspect
     * @return {@code true} when any content is assigned to this socket
     */
    public boolean hasSocketRef(int socketIndex) {
        return !socketRefsForSocket(socketIndex).isEmpty();
    }

    /**
     * Returns all refs attached to a socket index.
     *
     * @param socketIndex socket slot index to inspect
     * @return socket refs assigned to the index
     */
    public List<SocketedSkillRef> socketRefsForSocket(int socketIndex) {
        if (socketIndex < 0 || socketIndex >= socketCount) {
            throw new IllegalArgumentException("socketIndex must be in range [0, socketCount)");
        }

        return socketRefs.stream()
                .filter(ref -> ref.socketIndex() == socketIndex)
                .collect(Collectors.toList());
    }

    /**
     * Returns only support refs associated with this state.
     *
     * @return support refs list
     */
    public List<SocketedSkillRef> supportRefs() {
        return socketRefs.stream()
                .filter(SocketedSkillRef::isSupport)
                .collect(Collectors.toList());
    }

    /**
     * Returns only skill refs associated with this state.
     *
     * @return skill refs list
     */
    public List<SocketedSkillRef> skillRefs() {
        return socketRefs.stream()
                .filter(SocketedSkillRef::isSkill)
                .collect(Collectors.toList());
    }

    /**
     * Returns all linked socket indices represented by configured groups.
     *
     * @return set of linked socket indices
     */
    public Set<Integer> linkedSocketIndices() {
        return socketLinkGroups.stream()
                .flatMap(group -> group.socketIndices().stream())
                .collect(Collectors.toSet());
    }

    /**
     * Returns the socket ref associated with the active skill id when one exists.
     *
     * @return active skill socket ref when present
     */
    public Optional<SocketedSkillRef> activeSkillRef() {
        if (activeSkill.isEmpty()) {
            return Optional.empty();
        }

        return socketRefs.stream()
                .filter(SocketedSkillRef::isSkill)
                .filter(ref -> ref.definitionId().equals(activeSkill.get()))
                .findFirst();
    }

    /**
     * Returns the link group that contains the provided socket index.
     *
     * @param socketIndex socket slot index to inspect
     * @return matching link group when present
     */
    public Optional<SocketLinkGroup> linkGroupForSocket(int socketIndex) {
        validateSocketRefIndex(socketIndex, "socketIndex", socketCount);
        return socketLinkGroups.stream()
                .filter(group -> group.socketIndices().contains(socketIndex))
                .findFirst();
    }

    /**
     * Returns support refs linked to the active skill socket.
     *
     * @return linked support refs in the same link group as the active skill
     */
    public List<SocketedSkillRef> linkedSupportRefsForActiveSkill() {
        Optional<SocketedSkillRef> activeSkillRef = activeSkillRef();
        if (activeSkillRef.isEmpty()) {
            return List.of();
        }

        Optional<SocketLinkGroup> group = linkGroupForSocket(activeSkillRef.get().socketIndex());
        if (group.isEmpty()) {
            return List.of();
        }

        Set<Integer> linkedIndices = Set.copyOf(group.get().socketIndices());
        return socketRefs.stream()
                .filter(SocketedSkillRef::isSupport)
                .filter(ref -> linkedIndices.contains(ref.socketIndex()))
                .toList();
    }

}
