package kim.biryeong.esekai2.api.item.socket;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

/**
 * Slot content categories that can be socketed onto an item.
 */
public enum SocketSlotType {
    SKILL("skill"),
    SUPPORT("support");

    /**
     * Codec used by datapacks and persisted item state for socket content kind.
     */
    public static final Codec<SocketSlotType> CODEC = Codec.STRING.comapFlatMap(SocketSlotType::bySerializedName, SocketSlotType::serializedName);

    private final String serializedName;

    SocketSlotType(String serializedName) {
        this.serializedName = serializedName;
    }

    /**
     * Returns the normalized serialized name used in JSON fixtures.
     *
     * @return serialized slot type string
     */
    public String serializedName() {
        return serializedName;
    }

    private static DataResult<SocketSlotType> bySerializedName(String serializedName) {
        for (SocketSlotType type : values()) {
            if (type.serializedName.equals(serializedName)) {
                return DataResult.success(type);
            }
        }

        return DataResult.error(() -> "Unknown socket slot type: " + serializedName);
    }
}
