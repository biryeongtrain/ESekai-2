package kim.biryeong.esekai2.impl.skill.entity;

import kim.biryeong.esekai2.impl.skill.execution.SkillRuntimeManager;
import net.minecraft.resources.Identifier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Minimal server-side projectile carrier for skill runtime events.
 */
public final class SkillProjectileEntity extends SkillRuntimeEntity {
    private Vec3 velocity = Vec3.ZERO;

    public SkillProjectileEntity(EntityType<? extends SkillProjectileEntity> entityType, Level level) {
        super(entityType, level);
    }

    public static SkillProjectileEntity create(Level level, Identifier skillId, String componentId, String payloadId, int lifeTicks, boolean gravity, Vec3 velocity) {
        SkillProjectileEntity entity = new SkillProjectileEntity(SkillRuntimeEntityTypes.PROJECTILE, level);
        entity.configure(skillId, componentId, "projectile", payloadId, lifeTicks, gravity);
        entity.setPos(0.0, 0.0, 0.0);
        entity.setVelocity(velocity);
        return entity;
    }

    public void setVelocity(Vec3 velocity) {
        this.velocity = velocity == null ? Vec3.ZERO : velocity;
        setDeltaMovement(this.velocity);
    }

    @Override
    public void tick() {
        if (isRemoved()) {
            return;
        }

        super.tick();

        if (gravity()) {
            velocity = velocity.add(0.0, -0.04, 0.0);
        }

        Vec3 motion = velocity;
        if (motion.lengthSqr() == 0.0) {
            motion = getDeltaMovement();
        }

        if (motion.lengthSqr() == 0.0) {
            return;
        }

        Vec3 start = position();
        Vec3 next = start.add(motion);
        move(MoverType.SELF, motion);
        setDeltaMovement(motion);

        BlockPos blockPos = BlockPos.containing(next);
        if (!level().getBlockState(blockPos).isAir()) {
            recordHit(null, next);
            SkillRuntimeManager.handleProjectileHit(this, null, next);
            return;
        }

        AABB sweep = new AABB(
                Math.min(start.x, next.x),
                Math.min(start.y, next.y),
                Math.min(start.z, next.z),
                Math.max(start.x, next.x),
                Math.max(start.y, next.y),
                Math.max(start.z, next.z)
        ).inflate(0.5D);
        List<Entity> hits = level().getEntities(this, sweep, entity ->
                entity.isAlive()
                        && entity != this
                        && (ownerUuid() == null || !ownerUuid().equals(entity.getUUID()))
        );
        if (!hits.isEmpty()) {
            recordHit(hits.getFirst(), next);
            SkillRuntimeManager.handleProjectileHit(this, hits.getFirst(), next);
            return;
        }
    }
}
