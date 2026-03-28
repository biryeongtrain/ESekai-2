package kim.biryeong.esekai2.api.damage.breakdown;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable aggregate of one or more typed damage contributions.
 *
 * <p>This model is the canonical input and output shape for later combat calculations. It stores
 * one amount per {@link DamageType}, automatically merges duplicate additions, and does not retain
 * zero-value entries.</p>
 */
public final class DamageBreakdown {
    private static final Codec<Map<DamageType, Double>> BASE_CODEC = Codec.unboundedMap(DamageType.CODEC, Codec.DOUBLE);
    private static final DamageBreakdown EMPTY = new DamageBreakdown(new EnumMap<>(DamageType.class));

    /**
     * Validated codec used to decode and encode typed damage aggregates.
     */
    public static final Codec<DamageBreakdown> CODEC = BASE_CODEC.comapFlatMap(DamageBreakdown::fromCodecMap, DamageBreakdown::entries);

    private final Map<DamageType, Double> amounts;

    private DamageBreakdown(Map<DamageType, Double> amounts) {
        this.amounts = Collections.unmodifiableMap(new EnumMap<>(amounts));
    }

    /**
     * Returns an empty damage aggregate with no typed contributions.
     *
     * @return empty damage aggregate
     */
    public static DamageBreakdown empty() {
        return EMPTY;
    }

    /**
     * Returns a damage aggregate containing one typed contribution.
     *
     * @param type hardcoded damage category to store
     * @param amount non-negative finite amount assigned to the category
     * @return new damage aggregate containing the provided contribution
     */
    public static DamageBreakdown of(DamageType type, double amount) {
        return empty().with(type, amount);
    }

    /**
     * Returns the stored amount for the requested damage type.
     *
     * @param type damage type to query
     * @return stored amount, or {@code 0.0} when the type is not present
     */
    public double amount(DamageType type) {
        Objects.requireNonNull(type, "type");
        return amounts.getOrDefault(type, 0.0);
    }

    /**
     * Returns a copy of this aggregate with the requested type set to the provided amount.
     *
     * <p>Zero removes the stored entry from the aggregate.</p>
     *
     * @param type damage type to replace
     * @param amount non-negative finite amount to store
     * @return copied aggregate with the provided amount applied
     */
    public DamageBreakdown with(DamageType type, double amount) {
        validateAmount(amount);

        EnumMap<DamageType, Double> updated = new EnumMap<>(DamageType.class);
        updated.putAll(amounts);

        if (amount == 0.0) {
            updated.remove(Objects.requireNonNull(type, "type"));
        } else {
            updated.put(Objects.requireNonNull(type, "type"), amount);
        }

        return createCanonical(updated);
    }

    /**
     * Returns a copy of this aggregate with one typed portion added to the existing amount.
     *
     * @param portion typed damage portion to merge into this aggregate
     * @return copied aggregate including the additional portion
     */
    public DamageBreakdown plus(DamagePortion portion) {
        Objects.requireNonNull(portion, "portion");
        return with(portion.type(), amount(portion.type()) + portion.amount());
    }

    /**
     * Returns a copy of this aggregate with every entry from another aggregate merged into it.
     *
     * @param other aggregate to merge into this one
     * @return copied aggregate including the merged entries
     */
    public DamageBreakdown plus(DamageBreakdown other) {
        Objects.requireNonNull(other, "other");

        DamageBreakdown merged = this;
        for (Map.Entry<DamageType, Double> entry : other.amounts.entrySet()) {
            merged = merged.with(entry.getKey(), merged.amount(entry.getKey()) + entry.getValue());
        }
        return merged;
    }

    /**
     * Returns whether this aggregate has no stored typed damage entries.
     *
     * @return {@code true} when no damage entries are present
     */
    public boolean isEmpty() {
        return amounts.isEmpty();
    }

    /**
     * Returns the total amount represented by all stored damage types.
     *
     * @return sum of every stored typed contribution
     */
    public double totalAmount() {
        double total = 0.0;
        for (double amount : amounts.values()) {
            total += amount;
        }
        return total;
    }

    /**
     * Returns an immutable view of the currently stored typed damage entries.
     *
     * @return immutable canonical damage map keyed by damage type
     */
    public Map<DamageType, Double> entries() {
        return amounts;
    }

    private static DataResult<DamageBreakdown> fromCodecMap(Map<DamageType, Double> map) {
        try {
            return DataResult.success(createCanonical(map));
        } catch (IllegalArgumentException exception) {
            return DataResult.error(exception::getMessage);
        }
    }

    private static DamageBreakdown createCanonical(Map<DamageType, Double> map) {
        EnumMap<DamageType, Double> canonical = new EnumMap<>(DamageType.class);

        for (Map.Entry<DamageType, Double> entry : map.entrySet()) {
            DamageType type = Objects.requireNonNull(entry.getKey(), "type");
            double amount = Objects.requireNonNull(entry.getValue(), "amount");
            validateAmount(amount);

            if (amount != 0.0) {
                canonical.put(type, amount);
            }
        }

        if (canonical.isEmpty()) {
            return EMPTY;
        }

        return new DamageBreakdown(canonical);
    }

    private static void validateAmount(double amount) {
        if (!Double.isFinite(amount)) {
            throw new IllegalArgumentException("amount must be a finite number");
        }

        if (amount < 0.0) {
            throw new IllegalArgumentException("amount must be greater than or equal to zero");
        }
    }
}
