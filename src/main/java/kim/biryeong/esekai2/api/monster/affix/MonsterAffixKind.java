package kim.biryeong.esekai2.api.monster.affix;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

/**
 * Distinguishes monster affix categories used during weighted rolling.
 */
public enum MonsterAffixKind {
    PREFIX("prefix"),
    SUFFIX("suffix");

    /**
     * Codec used by datapacks and test fixtures.
     */
    public static final Codec<MonsterAffixKind> CODEC = Codec.STRING.comapFlatMap(
            MonsterAffixKind::bySerializedName,
            MonsterAffixKind::serializedName
    );

    private final String serializedName;

    MonsterAffixKind(String serializedName) {
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

    private static DataResult<MonsterAffixKind> bySerializedName(String serializedName) {
        for (MonsterAffixKind kind : values()) {
            if (kind.serializedName.equals(serializedName)) {
                return DataResult.success(kind);
            }
        }

        return DataResult.error(() -> "Unknown monster affix kind: " + serializedName);
    }
}
