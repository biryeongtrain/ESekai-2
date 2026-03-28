package kim.biryeong.esekai2.impl.skill.entity;

import kim.biryeong.esekai2.Esekai2;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityDimensions;

/**
 * Holds the runtime carrier entity types used by data-driven skills.
 */
public final class SkillRuntimeEntityTypes {
    public static final Identifier PROJECTILE_ID = Identifier.fromNamespaceAndPath(Esekai2.MOD_ID, "skill_projectile");
    public static final Identifier ANCHORED_ID = Identifier.fromNamespaceAndPath(Esekai2.MOD_ID, "skill_anchored");

    public static final EntityType<SkillProjectileEntity> PROJECTILE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            PROJECTILE_ID,
            FabricEntityTypeBuilder.create(MobCategory.MISC, SkillProjectileEntity::new)
                    .dimensions(EntityDimensions.fixed(0.25F, 0.25F))
                    .trackRangeBlocks(64)
                    .trackedUpdateRate(1)
                    .disableSaving()
                    .build(ResourceKeyIds.projectileKey())
    );

    public static final EntityType<SkillAnchoredEntity> ANCHORED = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ANCHORED_ID,
            FabricEntityTypeBuilder.create(MobCategory.MISC, SkillAnchoredEntity::new)
                    .dimensions(EntityDimensions.fixed(0.5F, 0.5F))
                    .trackRangeBlocks(32)
                    .trackedUpdateRate(1)
                    .disableSaving()
                    .build(ResourceKeyIds.anchoredKey())
    );

    private static boolean bootstrapped;

    private SkillRuntimeEntityTypes() {
    }

    public static void bootstrap() {
        if (bootstrapped) {
            return;
        }

        PROJECTILE.toString();
        ANCHORED.toString();
        bootstrapped = true;
    }

    private static final class ResourceKeyIds {
        private static net.minecraft.resources.ResourceKey<EntityType<?>> projectileKey() {
            return net.minecraft.resources.ResourceKey.create(BuiltInRegistries.ENTITY_TYPE.key(), PROJECTILE_ID);
        }

        private static net.minecraft.resources.ResourceKey<EntityType<?>> anchoredKey() {
            return net.minecraft.resources.ResourceKey.create(BuiltInRegistries.ENTITY_TYPE.key(), ANCHORED_ID);
        }
    }
}
