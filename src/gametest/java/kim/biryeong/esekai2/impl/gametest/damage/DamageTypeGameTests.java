package kim.biryeong.esekai2.impl.gametest.damage;

import kim.biryeong.esekai2.api.damage.breakdown.DamageType;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

public final class DamageTypeGameTests {
    /**
     * Verifies that the hardcoded damage type list and stable serialized identifiers stay fixed.
     */
    @GameTest
    public void damageTypesExposeStableIds(GameTestHelper helper) {
        helper.assertValueEqual(DamageType.values().length, 6, "Damage type list should contain exactly six hardcoded entries");
        helper.assertValueEqual(DamageType.PHYSICAL.id(), "physical", "Physical damage id should be stable");
        helper.assertValueEqual(DamageType.FIRE.id(), "fire", "Fire damage id should be stable");
        helper.assertValueEqual(DamageType.COLD.id(), "cold", "Cold damage id should be stable");
        helper.assertValueEqual(DamageType.LIGHTNING.id(), "lightning", "Lightning damage id should be stable");
        helper.assertValueEqual(DamageType.CHAOS.id(), "chaos", "Chaos damage id should be stable");
        helper.assertValueEqual(DamageType.FIXED.id(), "fixed", "Fixed damage id should be stable");
        helper.succeed();
    }

    /**
     * Verifies that fixed damage is flagged to bypass later mitigation layers.
     */
    @GameTest
    public void fixedDamageBypassesMitigation(GameTestHelper helper) {
        helper.assertTrue(DamageType.FIXED.bypassesMitigation(), "Fixed damage should bypass mitigation");
        helper.succeed();
    }

    /**
     * Verifies that every non-fixed damage type remains subject to mitigation.
     */
    @GameTest
    public void nonFixedDamageDoesNotBypassMitigation(GameTestHelper helper) {
        helper.assertTrue(!DamageType.PHYSICAL.bypassesMitigation(), "Physical damage should not bypass mitigation");
        helper.assertTrue(!DamageType.FIRE.bypassesMitigation(), "Fire damage should not bypass mitigation");
        helper.assertTrue(!DamageType.COLD.bypassesMitigation(), "Cold damage should not bypass mitigation");
        helper.assertTrue(!DamageType.LIGHTNING.bypassesMitigation(), "Lightning damage should not bypass mitigation");
        helper.assertTrue(!DamageType.CHAOS.bypassesMitigation(), "Chaos damage should not bypass mitigation");
        helper.succeed();
    }
}
