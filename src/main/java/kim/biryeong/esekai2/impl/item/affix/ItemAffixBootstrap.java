package kim.biryeong.esekai2.impl.item.affix;

import eu.pb4.polymer.core.api.other.PolymerComponent;
import net.minecraft.core.component.DataComponentType;

/**
 * Forces eager initialization of item affix data components during mod bootstrap.
 */
public final class ItemAffixBootstrap {
    private static boolean bootstrapped;

    private ItemAffixBootstrap() {
    }

    public static void bootstrap() {
        if (bootstrapped) {
            return;
        }

        DataComponentType<?> itemAffixState = ItemAffixComponents.ITEM_AFFIX_STATE;
        PolymerComponent.registerDataComponent(itemAffixState);
        bootstrapped = true;
    }
}
