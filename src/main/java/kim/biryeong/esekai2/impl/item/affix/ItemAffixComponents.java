package kim.biryeong.esekai2.impl.item.affix;

import kim.biryeong.esekai2.api.item.affix.ItemAffixState;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

/**
 * Item data component registration for persisted rolled item affix state.
 */
public final class ItemAffixComponents {
    public static final DataComponentType<ItemAffixState> ITEM_AFFIX_STATE = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath("esekai2", "item_affix_state"),
            DataComponentType.<ItemAffixState>builder()
                    .persistent(ItemAffixState.CODEC)
                    .cacheEncoding()
                    .build()
    );

    private ItemAffixComponents() {
    }
}
