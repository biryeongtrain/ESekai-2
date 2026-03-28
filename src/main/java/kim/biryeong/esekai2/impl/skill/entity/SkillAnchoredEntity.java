package kim.biryeong.esekai2.impl.skill.entity;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Minimal anchored carrier for summon/block lifecycle events.
 */
public final class SkillAnchoredEntity extends SkillRuntimeEntity {
    public SkillAnchoredEntity(EntityType<? extends SkillAnchoredEntity> entityType, Level level) {
        super(entityType, level);
    }

    public static SkillAnchoredEntity create(Level level, Identifier skillId, String componentId, String payloadId, int lifeTicks, boolean gravity) {
        SkillAnchoredEntity entity = new SkillAnchoredEntity(SkillRuntimeEntityTypes.ANCHORED, level);
        entity.configure(skillId, componentId, "anchored", payloadId, lifeTicks, gravity);
        entity.setPos(0.0, 0.0, 0.0);
        return entity;
    }

    @Override
    public void tick() {
        if (isRemoved()) {
            return;
        }

        super.tick();
    }
}
