package kim.biryeong.esekai2.impl.ailment;

import kim.biryeong.esekai2.Esekai2;
import kim.biryeong.esekai2.api.ailment.AilmentState;
import kim.biryeong.esekai2.api.ailment.AilmentType;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;

/**
 * Registers ailment attachments and custom MobEffect identities.
 */
public final class AilmentBootstrap {
    public static final AttachmentType<AilmentState> AILMENT_STATE = AttachmentRegistry.createPersistent(
            net.minecraft.resources.Identifier.fromNamespaceAndPath(Esekai2.MOD_ID, "ailment_state"),
            AilmentState.CODEC
    );

    public static final MobEffect IGNITE = register(AilmentType.IGNITE, 0xF97316);
    public static final MobEffect SHOCK = register(AilmentType.SHOCK, 0xFACC15);
    public static final MobEffect POISON = register(AilmentType.POISON, 0x16A34A);
    public static final MobEffect BLEED = register(AilmentType.BLEED, 0x991B1B);

    private static boolean bootstrapped;

    private AilmentBootstrap() {
    }

    public static void bootstrap() {
        if (bootstrapped) {
            return;
        }
        AILMENT_STATE.toString();
        bootstrapped = true;
    }

    public static Optional<AilmentState> get(LivingEntity entity) {
        return Optional.ofNullable(entity.getAttached(AILMENT_STATE));
    }

    public static void attach(LivingEntity entity, AilmentState state) {
        entity.setAttached(AILMENT_STATE, state);
    }

    public static MobEffect effect(AilmentType type) {
        return switch (type) {
            case IGNITE -> IGNITE;
            case SHOCK -> SHOCK;
            case POISON -> POISON;
            case BLEED -> BLEED;
        };
    }

    private static MobEffect register(AilmentType type, int color) {
        return Registry.register(
                BuiltInRegistries.MOB_EFFECT,
                type.effectId(),
                new AilmentMobEffect(type, color)
        );
    }
}
