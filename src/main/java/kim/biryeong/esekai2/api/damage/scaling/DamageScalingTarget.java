package kim.biryeong.esekai2.api.damage.scaling;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import kim.biryeong.esekai2.api.damage.breakdown.DamageType;

import java.util.Objects;
import java.util.Optional;

/**
 * Selects which damage types a scaling entry should affect.
 *
 * @param damageType optional specific damage type; empty means the scaling targets all damage types
 */
public record DamageScalingTarget(Optional<DamageType> damageType) {
    private static final DamageScalingTarget ALL = new DamageScalingTarget(Optional.empty());

    /**
     * Codec used to serialize and deserialize scaling targets as either {@code all} or a damage type id.
     */
    public static final Codec<DamageScalingTarget> CODEC = Codec.STRING.comapFlatMap(DamageScalingTarget::fromSerializedName, DamageScalingTarget::serializedName);

    public DamageScalingTarget {
        Objects.requireNonNull(damageType, "damageType");
    }

    /**
     * Returns a scaling target that applies to every typed damage component.
     *
     * @return target representing all damage types
     */
    public static DamageScalingTarget all() {
        return ALL;
    }

    /**
     * Returns a scaling target that applies only to the provided damage type.
     *
     * @param damageType specific damage type to target
     * @return target representing the provided damage type
     */
    public static DamageScalingTarget of(DamageType damageType) {
        return new DamageScalingTarget(Optional.of(Objects.requireNonNull(damageType, "damageType")));
    }

    /**
     * Returns whether this target applies to every damage type.
     *
     * @return {@code true} when the target matches all damage types
     */
    public boolean isAll() {
        return damageType.isEmpty();
    }

    /**
     * Returns whether this target applies to the provided damage type.
     *
     * @param damageType damage type being inspected
     * @return {@code true} when the target should affect the provided damage type
     */
    public boolean matches(DamageType damageType) {
        Objects.requireNonNull(damageType, "damageType");
        return this.damageType.map(damageType::equals).orElse(true);
    }

    private String serializedName() {
        return damageType.map(DamageType::id).orElse("all");
    }

    private static DataResult<DamageScalingTarget> fromSerializedName(String name) {
        if ("all".equals(name)) {
            return DataResult.success(all());
        }

        return DamageType.CODEC.parse(com.mojang.serialization.JavaOps.INSTANCE, name)
                .map(DamageScalingTarget::of);
    }
}
