package kim.biryeong.esekai2.api.item.affix;

import kim.biryeong.esekai2.impl.item.affix.ItemAffixComponents;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Objects;

/**
 * Public facade for persisted item affix state.
 */
public final class ItemAffixes {
    private ItemAffixes() {
    }

    /**
     * Returns the full affix state from a stack.
     *
     * @param stack item stack to inspect
     * @return resolved affix state
     */
    public static ItemAffixState get(ItemStack stack) {
        Objects.requireNonNull(stack, "stack");
        ItemAffixState state = stack.get(ItemAffixComponents.ITEM_AFFIX_STATE);
        return state == null ? ItemAffixState.EMPTY : state;
    }

    /**
     * Replaces the full affix state on a stack.
     *
     * @param stack target item stack
     * @param state state to store
     */
    public static void set(ItemStack stack, ItemAffixState state) {
        Objects.requireNonNull(stack, "stack");
        Objects.requireNonNull(state, "state");
        stack.set(ItemAffixComponents.ITEM_AFFIX_STATE, state);
    }

    /**
     * Clears affix state from a stack.
     *
     * @param stack target item stack
     */
    public static void clear(ItemStack stack) {
        Objects.requireNonNull(stack, "stack");
        stack.remove(ItemAffixComponents.ITEM_AFFIX_STATE);
    }

    /**
     * Returns only the rolled affixes stored on the stack.
     *
     * @param stack item stack to inspect
     * @return immutable rolled affix list
     */
    public static List<RolledAffix> getRolledAffixes(ItemStack stack) {
        return get(stack).rolledAffixes();
    }

    /**
     * Stores only the rolled affix list while preserving the facade shape.
     *
     * @param stack target item stack
     * @param rolledAffixes rolled affixes to store
     */
    public static void setRolledAffixes(ItemStack stack, List<RolledAffix> rolledAffixes) {
        Objects.requireNonNull(stack, "stack");
        Objects.requireNonNull(rolledAffixes, "rolledAffixes");
        set(stack, new ItemAffixState(rolledAffixes));
    }
}
