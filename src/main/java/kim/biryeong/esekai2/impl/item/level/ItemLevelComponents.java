package kim.biryeong.esekai2.impl.item.level;

import com.mojang.serialization.Codec;
import eu.pb4.polymer.common.api.PolymerCommonUtils;
import eu.pb4.polymer.core.api.item.PolymerItemUtils;
import eu.pb4.polymer.core.api.other.PolymerComponent;
import eu.pb4.polymer.core.impl.other.PolymerComponentImpl;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import kim.biryeong.esekai2.api.level.LevelRules;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;

/**
 * Item-level component registration used by ESekai's current item level helpers.
 */
public final class ItemLevelComponents {
    public static final DataComponentType<Integer> ITEM_LEVEL = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath("esekai2", "item_level"),
            DataComponentType.<Integer>builder()
                    .persistent(Codec.intRange(LevelRules.MIN_LEVEL, LevelRules.MAX_LEVEL))
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .cacheEncoding()
                    .build()
    );

    private ItemLevelComponents() {
    }

}
