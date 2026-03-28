package kim.biryeong.esekai2.api.damage.mitigation;

import kim.biryeong.esekai2.api.damage.breakdown.DamageBreakdown;
import kim.biryeong.esekai2.api.damage.breakdown.DamageType;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Captures the outcome of applying a mitigation layer to an incoming hit.
 *
 * <p>The mitigation delta is keyed by {@link DamageType}. Positive values mean damage was
 * prevented, while negative values mean damage was amplified by effects such as negative
 * resistance.</p>
 *
 * @param incomingDamage original incoming typed damage before mitigation
 * @param mitigatedDamage resulting typed damage after mitigation has been applied
 * @param mitigationDelta per-type signed change where positive prevents damage and negative amplifies it
 */
public record DamageMitigationResult(
        DamageBreakdown incomingDamage,
        DamageBreakdown mitigatedDamage,
        Map<DamageType, Double> mitigationDelta
) {
    public DamageMitigationResult {
        Objects.requireNonNull(incomingDamage, "incomingDamage");
        Objects.requireNonNull(mitigatedDamage, "mitigatedDamage");
        Objects.requireNonNull(mitigationDelta, "mitigationDelta");

        EnumMap<DamageType, Double> copiedDelta = new EnumMap<>(DamageType.class);
        for (Map.Entry<DamageType, Double> entry : mitigationDelta.entrySet()) {
            DamageType type = Objects.requireNonNull(entry.getKey(), "mitigationDelta key");
            double delta = Objects.requireNonNull(entry.getValue(), "mitigationDelta value");

            if (!Double.isFinite(delta)) {
                throw new IllegalArgumentException("mitigationDelta values must be finite numbers");
            }

            if (delta != 0.0) {
                copiedDelta.put(type, delta);
            }
        }

        mitigationDelta = Collections.unmodifiableMap(copiedDelta);
    }

    /**
     * Returns the signed mitigation delta for the requested damage type.
     *
     * @param type damage type to inspect
     * @return positive prevented amount, negative amplified amount, or {@code 0.0}
     */
    public double mitigationDelta(DamageType type) {
        Objects.requireNonNull(type, "type");
        return mitigationDelta.getOrDefault(type, 0.0);
    }
}
