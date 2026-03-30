package kim.biryeong.esekai2.impl.ailment;

import eu.pb4.polymer.core.api.other.PolymerMobEffect;
import kim.biryeong.esekai2.api.ailment.AilmentType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Custom ailment identity effect backed by attachment payload state.
 */
final class AilmentMobEffect extends MobEffect implements PolymerMobEffect {
    private final AilmentType type;

    AilmentMobEffect(AilmentType type, int color) {
        super(MobEffectCategory.HARMFUL, color);
        this.type = type;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity livingEntity, int amplifier) {
        return AilmentRuntime.tick(level, livingEntity, type);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}
