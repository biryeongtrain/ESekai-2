package kim.biryeong.esekai2.api.skill.execution;

import kim.biryeong.esekai2.api.damage.calculation.DamageCalculationResult;
import kim.biryeong.esekai2.api.skill.execution.PreparedApplyAilmentAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedApplyBuffAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedApplyDotAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedDamageAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedProjectileAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedRemoveEffectAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedSandstormParticleAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedSkillAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedSoundAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedSummonAtSightAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedSummonBlockAction;
import net.minecraft.world.entity.Entity;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * World-facing side-effect hooks used by the skill execution runtime.
 */
public interface SkillExecutionHooks {
    boolean playSound(SkillExecutionContext context, List<Entity> targets, PreparedSoundAction action);

    Optional<DamageCalculationResult> applyDamage(SkillExecutionContext context, List<Entity> targets, PreparedDamageAction action);

    default boolean applyEffect(SkillExecutionContext context, List<Entity> targets, PreparedApplyBuffAction action) {
        return false;
    }

    default boolean applyBuff(SkillExecutionContext context, List<Entity> targets, PreparedApplyBuffAction action) {
        return applyEffect(context, targets, action);
    }

    default boolean removeEffect(SkillExecutionContext context, List<Entity> targets, PreparedRemoveEffectAction action) {
        return false;
    }

    default boolean applyDamageOverTime(SkillExecutionContext context, List<Entity> targets, PreparedApplyDotAction action) {
        return false;
    }

    default boolean applyAilment(
            SkillExecutionContext context,
            List<Entity> targets,
            PreparedApplyAilmentAction action,
            Map<UUID, DamageCalculationResult> latestDamageResults
    ) {
        return false;
    }

    Optional<Entity> spawnProjectile(SkillExecutionContext context, List<Entity> targets, PreparedProjectileAction action);

    Optional<Entity> spawnSummonAtSight(SkillExecutionContext context, List<Entity> targets, PreparedSummonAtSightAction action);

    boolean placeBlock(SkillExecutionContext context, List<Entity> targets, PreparedSummonBlockAction action);

    boolean emitSandstormParticle(SkillExecutionContext context, List<Entity> targets, PreparedSandstormParticleAction action);

    /**
     * Dispatches one prepared action to the concrete hook implementation.
     *
     * @param context runtime execution context
     * @param targets resolved runtime targets for the route
     * @param action prepared action to execute
     * @return {@code true} when the action produced a world side effect
     */
    default boolean execute(SkillExecutionContext context, List<Entity> targets, PreparedSkillAction action) {
        return switch (action.actionType()) {
            case "sound" -> playSound(context, targets, (PreparedSoundAction) action);
            case "damage" -> applyDamage(context, targets, (PreparedDamageAction) action).isPresent();
            case "apply_effect", "apply_buff" -> applyBuff(context, targets, (PreparedApplyBuffAction) action);
            case "remove_effect" -> removeEffect(context, targets, (PreparedRemoveEffectAction) action);
            case "apply_dot" -> applyDamageOverTime(context, targets, (PreparedApplyDotAction) action);
            case "apply_ailment" -> applyAilment(context, targets, (PreparedApplyAilmentAction) action, Map.of());
            case "projectile" -> spawnProjectile(context, targets, (PreparedProjectileAction) action).isPresent();
            case "summon_at_sight" -> spawnSummonAtSight(context, targets, (PreparedSummonAtSightAction) action).isPresent();
            case "summon_block" -> placeBlock(context, targets, (PreparedSummonBlockAction) action);
            case "sandstorm_particle" -> emitSandstormParticle(context, targets, (PreparedSandstormParticleAction) action);
            default -> false;
        };
    }
}
