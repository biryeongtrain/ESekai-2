package kim.biryeong.esekai2.impl.ailment;

import eu.pb4.polymer.core.api.other.PolymerMobEffect;
import kim.biryeong.esekai2.api.ailment.AilmentType;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Custom ailment identity effect backed by attachment payload state.
 */
final class AilmentMobEffect extends MobEffect implements PolymerMobEffect {
    private static final Identifier CHILL_MOVEMENT_SPEED_MODIFIER_ID = Identifier.fromNamespaceAndPath("esekai2", "ailment_chill_movement_speed");
    private static final Identifier FREEZE_MOVEMENT_SPEED_MODIFIER_ID = Identifier.fromNamespaceAndPath("esekai2", "ailment_freeze_movement_speed");
    private static final Identifier STUN_MOVEMENT_SPEED_MODIFIER_ID = Identifier.fromNamespaceAndPath("esekai2", "ailment_stun_movement_speed");
    private final AilmentType type;

    AilmentMobEffect(AilmentType type, int color) {
        super(MobEffectCategory.HARMFUL, color);
        this.type = type;
        switch (type) {
            case CHILL -> addAttributeModifier(
                    Attributes.MOVEMENT_SPEED,
                    CHILL_MOVEMENT_SPEED_MODIFIER_ID,
                    -0.01,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            );
            case FREEZE -> addAttributeModifier(
                    Attributes.MOVEMENT_SPEED,
                    FREEZE_MOVEMENT_SPEED_MODIFIER_ID,
                    -1.0,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            );
            case STUN -> addAttributeModifier(
                    Attributes.MOVEMENT_SPEED,
                    STUN_MOVEMENT_SPEED_MODIFIER_ID,
                    -1.0,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            );
            default -> {
            }
        }
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity livingEntity, int amplifier) {
        AilmentRuntime.tick(level, livingEntity, type);
        return AilmentRuntime.hasActivePayload(livingEntity, type);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}
