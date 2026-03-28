package kim.biryeong.esekai2.api.item.affix;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

/**
 * Distinguishes the current PoE-style affix categories used by ESekai items.
 */
public enum AffixKind {
    PREFIX("prefix"),
    SUFFIX("suffix");

    /**
     * Codec used by datapacks and fixtures that refer to an affix kind.
     */
    public static final Codec<AffixKind> CODEC = Codec.STRING.comapFlatMap(AffixKind::bySerializedName, AffixKind::serializedName);

    private final String serializedName;

    AffixKind(String serializedName) {
        this.serializedName = serializedName;
    }

    /**
     * Returns the stable serialized id used in datapacks.
     *
     * @return lower-case serialized affix kind id
     */
    public String serializedName() {
        return serializedName;
    }

    private static DataResult<AffixKind> bySerializedName(String serializedName) {
        for (AffixKind kind : values()) {
            if (kind.serializedName.equals(serializedName)) {
                return DataResult.success(kind);
            }
        }

        return DataResult.error(() -> "Unknown affix kind: " + serializedName);
    }
}
