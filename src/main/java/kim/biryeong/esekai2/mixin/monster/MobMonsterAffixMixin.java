package kim.biryeong.esekai2.mixin.monster;

import kim.biryeong.esekai2.impl.monster.affix.MonsterAffixRuntimeInitializer;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

/**
 * Initializes monster affix state when a mob completes vanilla spawn finalization.
 */
@Mixin(Mob.class)
public abstract class MobMonsterAffixMixin {
    @Inject(method = "finalizeSpawn", at = @At("RETURN"))
    private void esekai2$initializeMonsterAffixes(
            ServerLevelAccessor level,
            DifficultyInstance difficulty,
            EntitySpawnReason spawnReason,
            SpawnGroupData spawnGroupData,
            CallbackInfoReturnable<SpawnGroupData> cir
    ) {
        Mob mob = (Mob) (Object) this;
        MonsterAffixRuntimeInitializer.initialize(mob, new Random(mob.getRandom().nextLong()));
    }
}
