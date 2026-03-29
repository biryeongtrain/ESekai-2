package kim.biryeong.esekai2.api.item.socket;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.world.entity.EquipmentSlot;

/**
 * Equipment slots that may expose socketed active skills for server-side casting.
 */
public enum SocketedEquipmentSlot {
    MAIN_HAND("main_hand", EquipmentSlot.MAINHAND),
    OFF_HAND("off_hand", EquipmentSlot.OFFHAND),
    HEAD("head", EquipmentSlot.HEAD),
    CHEST("chest", EquipmentSlot.CHEST),
    LEGS("legs", EquipmentSlot.LEGS),
    FEET("feet", EquipmentSlot.FEET);

    /**
     * Codec used by persistent selected-skill state.
     */
    public static final Codec<SocketedEquipmentSlot> CODEC = Codec.STRING.comapFlatMap(
            SocketedEquipmentSlot::bySerializedName,
            SocketedEquipmentSlot::serializedName
    );

    private final String serializedName;
    private final EquipmentSlot equipmentSlot;

    SocketedEquipmentSlot(String serializedName, EquipmentSlot equipmentSlot) {
        this.serializedName = serializedName;
        this.equipmentSlot = equipmentSlot;
    }

    /**
     * Returns the persistent serialized name for this slot.
     *
     * @return stable serialized slot name
     */
    public String serializedName() {
        return serializedName;
    }

    /**
     * Returns the corresponding vanilla equipment slot.
     *
     * @return vanilla equipment slot used to read the equipped item
     */
    public EquipmentSlot equipmentSlot() {
        return equipmentSlot;
    }

    private static DataResult<SocketedEquipmentSlot> bySerializedName(String serializedName) {
        for (SocketedEquipmentSlot slot : values()) {
            if (slot.serializedName.equals(serializedName)) {
                return DataResult.success(slot);
            }
        }

        return DataResult.error(() -> "Unknown socketed equipment slot: " + serializedName);
    }
}
