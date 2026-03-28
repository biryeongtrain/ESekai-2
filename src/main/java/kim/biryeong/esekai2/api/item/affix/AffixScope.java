package kim.biryeong.esekai2.api.item.affix;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

/**
 * Distinguishes whether an affix is meant to apply locally to the item or globally to its owner.
 */
public enum AffixScope {
    LOCAL("local"),
    GLOBAL("global");

    /**
     * Codec used by datapacks and fixtures that refer to an affix scope.
     */
    public static final Codec<AffixScope> CODEC = Codec.STRING.comapFlatMap(AffixScope::bySerializedName, AffixScope::serializedName);

    private final String serializedName;

    AffixScope(String serializedName) {
        this.serializedName = serializedName;
    }

    /**
     * Returns the stable serialized id used in datapacks.
     *
     * @return lower-case serialized affix scope id
     */
    public String serializedName() {
        return serializedName;
    }

    private static DataResult<AffixScope> bySerializedName(String serializedName) {
        for (AffixScope scope : values()) {
            if (scope.serializedName.equals(serializedName)) {
                return DataResult.success(scope);
            }
        }

        return DataResult.error(() -> "Unknown affix scope: " + serializedName);
    }
}
