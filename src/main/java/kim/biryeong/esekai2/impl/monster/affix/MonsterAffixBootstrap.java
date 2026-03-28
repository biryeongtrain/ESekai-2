package kim.biryeong.esekai2.impl.monster.affix;

import kim.biryeong.esekai2.Esekai2;
import kim.biryeong.esekai2.api.monster.affix.MonsterAffixDefinition;
import kim.biryeong.esekai2.api.monster.affix.MonsterAffixPoolDefinition;
import kim.biryeong.esekai2.api.monster.affix.MonsterAffixRegistries;
import kim.biryeong.esekai2.api.monster.affix.MonsterAffixState;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;

/**
 * Registers monster affix registries and persistent entity attachments.
 */
public final class MonsterAffixBootstrap {
    public static final AttachmentType<MonsterAffixState> MONSTER_AFFIX_STATE = AttachmentRegistry.createPersistent(
            net.minecraft.resources.Identifier.fromNamespaceAndPath(Esekai2.MOD_ID, "monster_affix_state"),
            MonsterAffixState.CODEC
    );

    private static boolean bootstrapped;

    private MonsterAffixBootstrap() {
    }

    public static void bootstrap() {
        if (bootstrapped) {
            return;
        }

        DynamicRegistries.register(MonsterAffixRegistries.MONSTER_AFFIX, MonsterAffixDefinition.CODEC);
        DynamicRegistries.register(MonsterAffixRegistries.MONSTER_AFFIX_POOL, MonsterAffixPoolDefinition.CODEC);
        MONSTER_AFFIX_STATE.toString();
        bootstrapped = true;
    }

    public static void attach(LivingEntity entity, MonsterAffixState state) {
        entity.setAttached(MONSTER_AFFIX_STATE, state);
    }

    public static Optional<MonsterAffixState> get(LivingEntity entity) {
        return Optional.ofNullable(entity.getAttached(MONSTER_AFFIX_STATE));
    }
}
