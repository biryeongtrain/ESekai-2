package kim.biryeong.esekai2.impl.gametest.damage;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import kim.biryeong.esekai2.api.damage.breakdown.DamageBreakdown;
import kim.biryeong.esekai2.api.damage.breakdown.DamagePortion;
import kim.biryeong.esekai2.api.damage.breakdown.DamageType;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;

import java.io.IOException;

public final class DamageBreakdownGameTests {
    private static final Identifier TEST_BREAKDOWN_RESOURCE_ID = Identifier.fromNamespaceAndPath("esekai-gametest", "gametest/damage_breakdown/test_breakdown.json");

    /**
     * Verifies that typed damage breakdowns merge duplicate entries and discard zero-valued components.
     */
    @GameTest
    public void typedDamageBreakdownAggregatesEntries(GameTestHelper helper) {
        DamageBreakdown breakdown = DamageBreakdown.empty()
                .plus(new DamagePortion(DamageType.PHYSICAL, 12.0))
                .plus(new DamagePortion(DamageType.FIRE, 7.5))
                .plus(new DamagePortion(DamageType.PHYSICAL, 3.0))
                .with(DamageType.COLD, 0.0)
                .with(DamageType.FIXED, 2.5);

        helper.assertValueEqual(breakdown.amount(DamageType.PHYSICAL), 15.0, "Physical damage should merge duplicate additions");
        helper.assertValueEqual(breakdown.amount(DamageType.FIRE), 7.5, "Fire damage should be stored in the aggregate");
        helper.assertValueEqual(breakdown.amount(DamageType.COLD), 0.0, "Zero-valued entries should not stay materialized");
        helper.assertValueEqual(breakdown.amount(DamageType.FIXED), 2.5, "Fixed damage should participate in the same aggregate model");
        helper.assertValueEqual(breakdown.entries().size(), 3, "Aggregate should only retain non-zero typed entries");
        helper.assertValueEqual(breakdown.totalAmount(), 25.0, "Total amount should sum every stored typed contribution");
        helper.succeed();
    }

    /**
     * Verifies that merging two damage breakdowns adds matching types and preserves unique entries.
     */
    @GameTest
    public void typedDamageBreakdownMergesOtherBreakdowns(GameTestHelper helper) {
        DamageBreakdown left = DamageBreakdown.of(DamageType.PHYSICAL, 10.0)
                .plus(new DamagePortion(DamageType.FIRE, 4.0));
        DamageBreakdown right = DamageBreakdown.of(DamageType.PHYSICAL, 6.0)
                .plus(new DamagePortion(DamageType.CHAOS, 2.0));

        DamageBreakdown merged = left.plus(right);

        helper.assertValueEqual(merged.amount(DamageType.PHYSICAL), 16.0, "Merged breakdown should add matching damage types together");
        helper.assertValueEqual(merged.amount(DamageType.FIRE), 4.0, "Merged breakdown should preserve unique left-hand damage entries");
        helper.assertValueEqual(merged.amount(DamageType.CHAOS), 2.0, "Merged breakdown should preserve unique right-hand damage entries");
        helper.succeed();
    }

    /**
     * Verifies that the damage breakdown codec round-trips a canonical typed damage aggregate.
     */
    @GameTest
    public void typedDamageBreakdownCodecRoundTrips(GameTestHelper helper) {
        DamageBreakdown decoded = decodeFixture(helper);

        helper.assertValueEqual(decoded.amount(DamageType.PHYSICAL), 12.0, "Fixture should decode physical damage");
        helper.assertValueEqual(decoded.amount(DamageType.FIRE), 7.5, "Fixture should decode fire damage");
        helper.assertValueEqual(decoded.amount(DamageType.FIXED), 3.0, "Fixture should decode fixed damage");
        helper.assertValueEqual(decoded.amount(DamageType.COLD), 0.0, "Missing types should decode as zero");

        JsonElement encoded = DamageBreakdown.CODEC.encodeStart(JsonOps.INSTANCE, decoded)
                .getOrThrow(message -> new IllegalStateException("Failed to encode damage breakdown fixture: " + message));
        DamageBreakdown roundTripped = DamageBreakdown.CODEC.parse(JsonOps.INSTANCE, encoded)
                .getOrThrow(message -> new IllegalStateException("Failed to decode encoded damage breakdown fixture: " + message));

        helper.assertTrue(roundTripped.entries().equals(decoded.entries()), "Encoded and decoded damage breakdown should preserve canonical entries");
        helper.assertValueEqual(roundTripped.totalAmount(), 22.5, "Round-tripped aggregate total should match the fixture");
        helper.succeed();
    }

    private static DamageBreakdown decodeFixture(GameTestHelper helper) {
        try (var reader = helper.getLevel().getServer().getResourceManager().openAsReader(TEST_BREAKDOWN_RESOURCE_ID)) {
            return DamageBreakdown.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(reader))
                    .getOrThrow(message -> new IllegalStateException("Failed to decode damage breakdown fixture: " + message));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load damage breakdown fixture", exception);
        }
    }
}
