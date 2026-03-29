package kim.biryeong.esekai2.impl.skill.entity;

import kim.biryeong.esekai2.impl.skill.execution.SkillRuntimeManager;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
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

        BlockHitResult blockHit = level().clip(new ClipContext(start, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 segmentEnd = blockHit.getType() == HitResult.Type.BLOCK ? blockHit.getLocation() : next;

        HitCandidate entityHit = findEntityHit(start, segmentEnd);
        if (entityHit != null) {
            recordHit(entityHit.entity(), entityHit.impactPosition());
            SkillRuntimeManager.handleProjectileHit(this, entityHit.entity(), entityHit.impactPosition());
            return;
        }

        if (blockHit.getType() == HitResult.Type.BLOCK) {
            recordHit(null, segmentEnd);
            SkillRuntimeManager.handleProjectileHit(this, null, segmentEnd);
            return;
        }
    }

    private HitCandidate findEntityHit(Vec3 start, Vec3 end) {
        AABB sweep = new AABB(start, end).inflate(1.0D);
        List<Entity> hits = level().getEntities(this, sweep, entity ->
                entity.isAlive()
                        && entity != this
                        && (ownerUuid() == null || !ownerUuid().equals(entity.getUUID()))
        );

        HitCandidate bestHit = null;
        double bestDistanceSqr = Double.POSITIVE_INFINITY;

        for (Entity hit : hits) {
            AABB bounds = hit.getBoundingBox().inflate(0.5D);
            Vec3 impactPosition = bounds.clip(start, end).orElse(null);
            if (impactPosition == null && bounds.contains(start)) {
                impactPosition = start;
            }
            if (impactPosition == null) {
                continue;
            }

            double distanceSqr = start.distanceToSqr(impactPosition);
            if (distanceSqr < bestDistanceSqr) {
                bestDistanceSqr = distanceSqr;
                bestHit = new HitCandidate(hit, impactPosition);
            }
        }

        return bestHit;
    }

    private record HitCandidate(Entity entity, Vec3 impactPosition) {
    }
}
