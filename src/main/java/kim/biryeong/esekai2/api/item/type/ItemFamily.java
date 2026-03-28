package kim.biryeong.esekai2.api.item.type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

/**
 * Coarse item family axis used by ESekai's current affix foundation.
 */
public enum ItemFamily {
    WEAPON("weapon"),
    ARMOUR("armour"),
    TRINKET("trinket");

    /**
     * Codec used by datapacks and fixtures that refer to an item family.
     */
    public static final Codec<ItemFamily> CODEC = Codec.STRING.comapFlatMap(ItemFamily::bySerializedName, ItemFamily::serializedName);

    private final String serializedName;

    ItemFamily(String serializedName) {
        this.serializedName = serializedName;
    }

    /**
     * Returns the stable serialized id used in datapacks.
     *
     * @return lower-case serialized family id
     */
    public String serializedName() {
        return serializedName;
    }

    private static DataResult<ItemFamily> bySerializedName(String serializedName) {
        for (ItemFamily family : values()) {
            if (family.serializedName.equals(serializedName)) {
                return DataResult.success(family);
            }
        }

        return DataResult.error(() -> "Unknown item family: " + serializedName);
    }
}
