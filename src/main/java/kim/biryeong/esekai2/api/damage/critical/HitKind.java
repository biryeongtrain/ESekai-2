package kim.biryeong.esekai2.api.damage.critical;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

/**
 * Distinguishes the minimum hit categories needed by ESekai's critical strike foundation.
 */
public enum HitKind {
    /**
     * Attack-based hit that uses attack critical strike stats.
     */
    ATTACK,
    /**
     * Spell-based hit that uses spell critical strike stats.
     */
    SPELL;

    /**
     * Codec used by datapacks and fixtures that refer to a hit kind.
     */
    public static final Codec<HitKind> CODEC = Codec.STRING.comapFlatMap(HitKind::bySerializedName, HitKind::serializedName);

    /**
     * Returns the stable serialized id used in datapacks.
     *
     * @return lower-case serialized hit kind id
     */
    public String serializedName() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    private static DataResult<HitKind> bySerializedName(String serializedName) {
        for (HitKind hitKind : values()) {
            if (hitKind.serializedName().equals(serializedName)) {
                return DataResult.success(hitKind);
            }
        }

        return DataResult.error(() -> "Unknown hit kind: " + serializedName);
    }
}
