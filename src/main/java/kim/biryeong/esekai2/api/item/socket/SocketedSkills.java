package kim.biryeong.esekai2.api.item.socket;

import kim.biryeong.esekai2.api.skill.definition.SkillDefinition;
import kim.biryeong.esekai2.api.skill.support.SkillSupportDefinition;
import kim.biryeong.esekai2.impl.item.socket.SocketItemComponents;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Public facade for socketed-skill item component access.
 */
public final class SocketedSkills {
    private SocketedSkills() {
    }

    /**
     * Returns the full socket state from a stack.
     *
     * @param stack item stack to inspect
     * @return resolved socketed skill item state
     */
    public static SocketedSkillItemState get(ItemStack stack) {
        Objects.requireNonNull(stack, "stack");
        SocketedSkillItemState state = stack.get(SocketItemComponents.SOCKETED_ITEM_STATE);
        return state == null ? SocketedSkillItemState.EMPTY : state;
    }

    /**
     * Replaces the full socket state on a stack.
     *
     * @param stack target item stack
     * @param state state to store
     */
    public static void set(ItemStack stack, SocketedSkillItemState state) {
        Objects.requireNonNull(stack, "stack");
        Objects.requireNonNull(state, "state");
        stack.set(SocketItemComponents.SOCKETED_ITEM_STATE, state);
    }

    /**
     * Clears socket state from a stack.
     *
     * @param stack target item stack
     */
    public static void clear(ItemStack stack) {
        Objects.requireNonNull(stack, "stack");
        stack.remove(SocketItemComponents.SOCKETED_ITEM_STATE);
    }

    /**
     * Reads the active skill id from the stack.
     *
     * @param stack item stack to inspect
     * @return optional active skill identifier
     */
    public static Optional<Identifier> getActiveSkill(ItemStack stack) {
        return get(stack).activeSkill();
    }

    /**
     * Stores only the active skill id while preserving other socket metadata.
     *
     * @param stack target item stack
     * @param activeSkill active skill to store
     */
    public static void setActiveSkill(ItemStack stack, Optional<Identifier> activeSkill) {
        Objects.requireNonNull(stack, "stack");
        Objects.requireNonNull(activeSkill, "activeSkill");
        SocketedSkillItemState state = get(stack);
        set(stack, new SocketedSkillItemState(
                activeSkill,
                state.socketCount(),
                state.socketLinkGroups(),
                state.socketRefs()
        ));
    }

    /**
     * Returns the socket slot count configured for this item stack.
     *
     * @param stack target item stack
     * @return socket slot count
     */
    public static int getSocketCount(ItemStack stack) {
        return get(stack).socketCount();
    }

    /**
     * Stores only socket count while preserving other socket metadata.
     *
     * @param stack target item stack
     * @param socketCount socket count to store
     */
    public static void setSocketCount(ItemStack stack, int socketCount) {
        Objects.requireNonNull(stack, "stack");
        SocketedSkillItemState state = get(stack);
        set(stack, new SocketedSkillItemState(
                state.activeSkill(),
                socketCount,
                state.socketLinkGroups(),
                state.socketRefs()
        ));
    }

    /**
     * Returns all persisted link groups for this stack.
     *
     * @param stack target item stack
     * @return link groups
     */
    public static List<SocketLinkGroup> getSocketLinkGroups(ItemStack stack) {
        return get(stack).socketLinkGroups();
    }

    /**
     * Stores link groups while preserving count and refs.
     *
     * @param stack target item stack
     * @param socketLinkGroups link groups to store
     */
    public static void setSocketLinkGroups(ItemStack stack, List<SocketLinkGroup> socketLinkGroups) {
        Objects.requireNonNull(stack, "stack");
        Objects.requireNonNull(socketLinkGroups, "socketLinkGroups");
        SocketedSkillItemState state = get(stack);
        set(stack, new SocketedSkillItemState(
                state.activeSkill(),
                state.socketCount(),
                socketLinkGroups,
                state.socketRefs()
        ));
    }

    /**
     * Returns all socket refs for this stack.
     *
     * @param stack target item stack
     * @return socket refs
     */
    public static List<SocketedSkillRef> getSocketRefs(ItemStack stack) {
        return get(stack).socketRefs();
    }

    /**
     * Stores socket refs while preserving other metadata.
     *
     * @param stack target item stack
     * @param socketRefs socket refs to store
     */
    public static void setSocketRefs(ItemStack stack, List<SocketedSkillRef> socketRefs) {
        Objects.requireNonNull(stack, "stack");
        Objects.requireNonNull(socketRefs, "socketRefs");
        SocketedSkillItemState state = get(stack);
        set(stack, new SocketedSkillItemState(
                state.activeSkill(),
                state.socketCount(),
                state.socketLinkGroups(),
                socketRefs
        ));
    }

    /**
     * Resolves the active skill definition from the item state when present in the provided registry.
     *
     * @param stack item stack to inspect
     * @param skillRegistry registry used to resolve active skill ids
     * @return active skill definition when the stack declares a resolvable skill id
     */
    public static Optional<SkillDefinition> resolveActiveSkill(ItemStack stack, Registry<SkillDefinition> skillRegistry) {
        Objects.requireNonNull(stack, "stack");
        Objects.requireNonNull(skillRegistry, "skillRegistry");
        return resolveDefinitions(stack, skillRegistry, null).activeSkill();
    }

    /**
     * Resolves support definitions linked to the active skill socket.
     *
     * @param stack item stack to inspect
     * @param supportRegistry registry used to resolve support ids
     * @return linked support definitions in link-group order
     */
    public static List<SkillSupportDefinition> resolveLinkedSupports(
            ItemStack stack,
            Registry<SkillSupportDefinition> supportRegistry
    ) {
        Objects.requireNonNull(stack, "stack");
        Objects.requireNonNull(supportRegistry, "supportRegistry");
        return linkedSupportRefsInSocketOrder(get(stack)).stream()
                .map(SocketedSkillRef::definitionId)
                .map(supportRegistry::getOptional)
                .flatMap(Optional::stream)
                .toList();
    }

    /**
     * Resolves both the active skill and linked support definitions from a socketed stack.
     *
     * @param stack item stack to inspect
     * @param skillRegistry registry used to resolve active skill ids
     * @param supportRegistry registry used to resolve linked supports; may be {@code null} when only active skill resolution is needed
     * @return resolved active skill/support bundle with non-fatal warnings
     */
    public static SocketedSkillLoadResult resolveDefinitions(
            ItemStack stack,
            Registry<SkillDefinition> skillRegistry,
            Registry<SkillSupportDefinition> supportRegistry
    ) {
        Objects.requireNonNull(stack, "stack");
        Objects.requireNonNull(skillRegistry, "skillRegistry");

        SocketedSkillItemState state = get(stack);
        List<String> warnings = new ArrayList<>();

        Optional<Identifier> activeSkillId = state.activeSkill();
        if (activeSkillId.isEmpty()) {
            warnings.add("Socketed item does not declare an active skill");
            return new SocketedSkillLoadResult(Optional.empty(), List.of(), warnings);
        }

        if (state.activeSkillRef().isEmpty()) {
            warnings.add("Socketed item declares active skill " + activeSkillId.orElseThrow()
                    + " but has no matching skill socket ref");
            return new SocketedSkillLoadResult(Optional.empty(), List.of(), warnings);
        }

        Optional<SkillDefinition> activeSkill = skillRegistry.getOptional(activeSkillId.orElseThrow());
        if (activeSkill.isEmpty()) {
            warnings.add("Missing skill definition for socketed active skill: " + activeSkillId.orElseThrow());
            return new SocketedSkillLoadResult(Optional.empty(), List.of(), warnings);
        }

        if (supportRegistry == null) {
            return new SocketedSkillLoadResult(activeSkill, List.of(), warnings);
        }

        List<SkillSupportDefinition> linkedSupports = new ArrayList<>();
        for (SocketedSkillRef supportRef : linkedSupportRefsInSocketOrder(state)) {
            Optional<SkillSupportDefinition> support = supportRegistry.getOptional(supportRef.definitionId());
            if (support.isPresent()) {
                linkedSupports.add(support.orElseThrow());
                continue;
            }

            warnings.add("Missing support definition for socketed support: " + supportRef.definitionId());
        }

        return new SocketedSkillLoadResult(activeSkill, List.copyOf(linkedSupports), warnings);
    }

    /**
     * Returns the currently equipped item stack for one supported socket-bearing equipment slot.
     *
     * @param player player to inspect
     * @param equipmentSlot selected equipment slot
     * @return equipped stack reference from the player's current inventory state
     */
    public static ItemStack getEquippedStack(ServerPlayer player, SocketedEquipmentSlot equipmentSlot) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(equipmentSlot, "equipmentSlot");
        return player.getItemBySlot(equipmentSlot.equipmentSlot());
    }

    private static List<SocketedSkillRef> linkedSupportRefsInSocketOrder(SocketedSkillItemState state) {
        return state.linkedSupportRefsForActiveSkill().stream()
                .sorted(Comparator.comparingInt(SocketedSkillRef::socketIndex))
                .toList();
    }
}
