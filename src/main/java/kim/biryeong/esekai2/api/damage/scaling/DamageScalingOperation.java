package kim.biryeong.esekai2.api.damage.scaling;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

/**
 * Declares the supported scaling buckets used by ESekai hit damage calculations.
 */
public enum DamageScalingOperation {
    /**
     * Adds a flat typed amount before percentage-based scaling is applied.
     */
    ADD("add"),
    /**
     * Adds into the shared additive percentage bucket.
     */
    INCREASED("increased"),
    /**
     * Multiplies damage in a separate multiplicative percentage bucket.
     */
    MORE("more");

    /**
     * Codec used to serialize and deserialize stable damage scaling operation identifiers.
     */
    public static final Codec<DamageScalingOperation> CODEC = Codec.STRING.comapFlatMap(DamageScalingOperation::fromId, DamageScalingOperation::id);

    private final String id;

    DamageScalingOperation(String id) {
        this.id = id;
    }

    /**
     * Returns the stable serialized identifier used for this scaling operation.
     *
     * @return lower-case stable identifier
     */
    public String id() {
        return id;
    }

    private static DataResult<DamageScalingOperation> fromId(String id) {
        for (DamageScalingOperation operation : values()) {
            if (operation.id.equals(id)) {
                return DataResult.success(operation);
            }
        }

        return DataResult.error(() -> "Unknown damage scaling operation: " + id);
    }
}
