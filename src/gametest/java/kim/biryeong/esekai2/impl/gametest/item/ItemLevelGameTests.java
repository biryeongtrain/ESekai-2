package kim.biryeong.esekai2.impl.gametest.item;

import kim.biryeong.esekai2.api.item.level.ItemLevels;
import kim.biryeong.esekai2.api.monster.level.MonsterLevelContext;
import kim.biryeong.esekai2.api.monster.level.MonsterRarity;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class ItemLevelGameTests {
    /**
     * Verifies that item level helpers can store, read, and clear a stack level value.
     */
    @GameTest
    public void itemLevelCanBeStoredAndCleared(GameTestHelper helper) {
        ItemStack stack = new ItemStack(Items.STONE);

        ItemLevels.set(stack, 68);
        helper.assertValueEqual(ItemLevels.get(stack).orElseThrow(), 68, "Stored item level should be readable from the stack");

        ItemLevels.clear(stack);
        helper.assertTrue(ItemLevels.get(stack).isEmpty(), "Clearing the item level should remove the stored value");
        helper.succeed();
    }

    /**
     * Verifies that invalid stored item levels are rejected.
     */
    @GameTest
    public void itemLevelRejectsInvalidValue(GameTestHelper helper) {
        try {
            ItemLevels.set(new ItemStack(Items.STONE), 0);
            throw helper.assertionException("Item level helpers should reject values below the shared level minimum");
        } catch (IllegalArgumentException expected) {
            helper.succeed();
        }
    }

    /**
     * Verifies that item level derivation uses monster rarity offsets and clamps to the shared maximum.
     */
    @GameTest
    public void itemLevelDerivesFromMonsterRarity(GameTestHelper helper) {
        helper.assertValueEqual(ItemLevels.deriveFromMonster(new MonsterLevelContext(68, MonsterRarity.NORMAL, false, false)), 68, "Normal monsters should derive same-level items");
        helper.assertValueEqual(ItemLevels.deriveFromMonster(new MonsterLevelContext(68, MonsterRarity.MAGIC, false, false)), 69, "Magic monsters should derive one higher item level");
        helper.assertValueEqual(ItemLevels.deriveFromMonster(new MonsterLevelContext(68, MonsterRarity.RARE, false, false)), 70, "Rare monsters should derive two higher item levels");
        helper.assertValueEqual(ItemLevels.deriveFromMonster(new MonsterLevelContext(100, MonsterRarity.UNIQUE, false, false)), 100, "Derived item level should clamp at one hundred");
        helper.succeed();
    }
}
