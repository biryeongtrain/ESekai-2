package kim.biryeong.esekai2.api.item.level;

import kim.biryeong.esekai2.api.level.LevelRules;
import kim.biryeong.esekai2.api.monster.level.MonsterLevelContext;
import kim.biryeong.esekai2.api.monster.level.MonsterLevels;
import kim.biryeong.esekai2.impl.item.level.ItemLevelComponents;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;
import java.util.OptionalInt;

/**
 * Public entry points for storing and deriving item levels.
 */
public final class ItemLevels {
    private ItemLevels() {
    }

    /**
     * Returns the stored item level when present on the stack.
     *
     * @param stack item stack to inspect
     * @return stored item level when present
     */
    public static OptionalInt get(ItemStack stack) {
        Objects.requireNonNull(stack, "stack");
        Integer level = stack.get(ItemLevelComponents.ITEM_LEVEL);
        return level == null ? OptionalInt.empty() : OptionalInt.of(level);
    }

    /**
     * Stores an item level on the stack.
     *
     * @param stack item stack to mutate
     * @param level item level to store
     */
    public static void set(ItemStack stack, int level) {
        Objects.requireNonNull(stack, "stack");
        stack.set(ItemLevelComponents.ITEM_LEVEL, LevelRules.requireValidLevel(level, "level"));
    }

    /**
     * Removes the stored item level from the stack while preserving unrelated custom data.
     *
     * @param stack item stack to mutate
     */
    public static void clear(ItemStack stack) {
        Objects.requireNonNull(stack, "stack");
        stack.remove(ItemLevelComponents.ITEM_LEVEL);
    }

    /**
     * Derives an item level from the provided monster level context using the current rarity offsets.
     *
     * @param context monster level context to inspect
     * @return derived item level
     */
    public static int deriveFromMonster(MonsterLevelContext context) {
        return MonsterLevels.resolveDroppedItemLevel(context);
    }
}
