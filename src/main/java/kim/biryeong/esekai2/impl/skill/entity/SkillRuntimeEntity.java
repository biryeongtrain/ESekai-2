package kim.biryeong.esekai2.impl.skill.entity;

import net.minecraft.resources.Identifier;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;
import java.util.UUID;

/**
 * Shared server-side carrier state for skill runtime entities.
 */
public abstract class SkillRuntimeEntity extends Entity {
    private String skillId = "";
    private String componentId = "";
    private String executionKind = "";
    private String payloadId = "";
    private int lifeTicks;
    private int ageTicks;
    private boolean gravity;
    private UUID ownerUuid;
    private UUID lastHitEntityUuid;
    private Vec3 lastImpactPosition = Vec3.ZERO;

    protected SkillRuntimeEntity(EntityType<? extends SkillRuntimeEntity> entityType, Level level) {
        super(entityType, level);
    }

    public void configure(
            Identifier skillId,
            String componentId,
            String executionKind,
            String payloadId,
            int lifeTicks,
            boolean gravity
    ) {
        this.skillId = Objects.requireNonNull(skillId, "skillId").toString();
        this.componentId = Objects.requireNonNull(componentId, "componentId");
        this.executionKind = Objects.requireNonNull(executionKind, "executionKind");
        this.payloadId = Objects.requireNonNull(payloadId, "payloadId");
        this.lifeTicks = Math.max(0, lifeTicks);
        this.gravity = gravity;
    }

    public String skillId() {
        return skillId;
    }

    public String componentId() {
        return componentId;
    }

    public String executionKind() {
        return executionKind;
    }

    public String payloadId() {
        return payloadId;
    }

    public int lifeTicks() {
        return lifeTicks;
    }

    public int ageTicks() {
        return ageTicks;
    }

    public boolean gravity() {
        return gravity;
    }

    public UUID lastHitEntityUuid() {
        return lastHitEntityUuid;
    }

    public UUID ownerUuid() {
        return ownerUuid;
    }

    public Vec3 lastImpactPosition() {
        return lastImpactPosition;
    }

    protected void recordHit(Entity hitEntity, Vec3 impactPosition) {
        this.lastHitEntityUuid = hitEntity == null ? null : hitEntity.getUUID();
        this.lastImpactPosition = impactPosition == null ? Vec3.ZERO : impactPosition;
    }

    public void setOwnerUuid(UUID ownerUuid) {
        this.ownerUuid = ownerUuid;
    }

    protected void expireCarrier() {
        onCarrierExpired();
        discard();
    }

    protected void onCarrierExpired() {
    }

    protected boolean shouldExpireByLifetime() {
        return lifeTicks > 0 && ageTicks >= lifeTicks;
    }

    protected void advanceAge() {
        ageTicks++;
    }

    protected void setGravityEnabled(boolean gravity) {
        this.gravity = gravity;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        // Runtime carriers are ephemeral and intentionally do not persist across saves.
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        // Runtime carriers are ephemeral and intentionally do not persist across saves.
    }

    @Override
    public boolean hurtServer(ServerLevel world, DamageSource source, float amount) {
        return false;
    }
}
