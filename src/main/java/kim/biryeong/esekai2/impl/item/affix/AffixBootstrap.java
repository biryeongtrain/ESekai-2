package kim.biryeong.esekai2.impl.item.affix;

import kim.biryeong.esekai2.api.item.affix.AffixDefinition;
import kim.biryeong.esekai2.api.item.affix.AffixRegistries;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;

/**
 * Registers the affix dynamic registry once during mod initialization.
 */
public final class AffixBootstrap {
    private static boolean bootstrapped;

    private AffixBootstrap() {
    }

    public static void bootstrap() {
        if (bootstrapped) {
            return;
        }

        DynamicRegistries.register(AffixRegistries.AFFIX, AffixDefinition.CODEC);
        bootstrapped = true;
    }
}
