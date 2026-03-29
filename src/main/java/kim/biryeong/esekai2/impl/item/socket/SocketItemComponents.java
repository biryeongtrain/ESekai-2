package kim.biryeong.esekai2.impl.item.socket;

import kim.biryeong.esekai2.api.item.socket.SocketedSkillItemState;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

/**
 * Item data component registration for socketed skill item state.
 */
public final class SocketItemComponents {
    public static final DataComponentType<SocketedSkillItemState> SOCKETED_ITEM_STATE = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath("esekai2", "socketed_item_state"),
            DataComponentType.<SocketedSkillItemState>builder()
                    .persistent(SocketedSkillItemState.CODEC)
                    .cacheEncoding()
                    .build()
    );

    private SocketItemComponents() {
    }
}
