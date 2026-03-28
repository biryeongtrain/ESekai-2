package kim.biryeong.esekai2.impl.skill.execution;

import kim.biryeong.esekai2.api.skill.execution.PreparedSkillUse;
import kim.biryeong.esekai2.api.skill.execution.SkillExecutionContext;
import kim.biryeong.esekai2.api.skill.execution.Skills;
import kim.biryeong.esekai2.impl.skill.entity.SkillRuntimeEntity;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Tracks active skill runtime components and dispatches their lifecycle events.
 */
public final class SkillRuntimeManager {
    private static final Map<UUID, ActiveSkillComponent> ACTIVE = new LinkedHashMap<>();
    private static boolean bootstrapped;

    private SkillRuntimeManager() {
    }

    public static void bootstrap() {
        if (bootstrapped) {
            return;
        }

        ServerTickEvents.END_SERVER_TICK.register(SkillRuntimeManager::tickServer);
        bootstrapped = true;
    }

    public static void register(
            Entity runtimeEntity,
            SkillExecutionContext castContext,
            String componentId,
            int lifeTicks,
            @Nullable BlockPos blockPos,
            @Nullable BlockState previousBlockState
    ) {
        Objects.requireNonNull(runtimeEntity, "runtimeEntity");
        Objects.requireNonNull(castContext, "castContext");
        Objects.requireNonNull(componentId, "componentId");

        ACTIVE.put(runtimeEntity.getUUID(), new ActiveSkillComponent(
                runtimeEntity,
                castContext.preparedUse(),
                castContext.caster(),
                castContext.target(),
                componentId,
                Math.max(0, lifeTicks),
                blockPos,
                previousBlockState
        ));
    }

    public static void handleProjectileHit(SkillRuntimeEntity runtimeEntity, @Nullable Entity hitEntity, Vec3 impactPosition) {
        Objects.requireNonNull(runtimeEntity, "runtimeEntity");
        Objects.requireNonNull(impactPosition, "impactPosition");

        ActiveSkillComponent component = ACTIVE.remove(runtimeEntity.getUUID());
        if (component == null) {
            runtimeEntity.discard();
            return;
        }

        SkillExecutionContext onHitContext = component.context(runtimeEntity, Optional.ofNullable(hitEntity), impactPosition, Optional.of(BlockPos.containing(impactPosition)));
        if (component.hasPreparedComponent()) {
            Skills.executeOnHit(onHitContext, component.componentId());
            Skills.executeOnEntityExpire(onHitContext, component.componentId());
        }

        component.restoreBlockIfPresent(onHitContext.level());
        runtimeEntity.discard();
    }

    private static void tickServer(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            tickWorld(level);
        }
    }

    private static void tickWorld(ServerLevel level) {
        List<UUID> removals = new ArrayList<>();
        List<ActiveSkillComponent> snapshot = new ArrayList<>(ACTIVE.values());

        for (ActiveSkillComponent component : snapshot) {
            if (!ACTIVE.containsKey(component.entity().getUUID())) {
                continue;
            }

            if (component.entity().level() != level) {
                continue;
            }

            if (component.entity().isRemoved()) {
                expireRemovedComponent(component);
                removals.add(component.entity().getUUID());
                continue;
            }

            int age = component.advanceAge();
            SkillExecutionContext tickContext = component.context(
                    component.entity(),
                    Optional.empty(),
                    component.entity().position(),
                    Optional.of(component.entity().blockPosition())
            );

            if (component.hasPreparedComponent()) {
                Skills.executeTick(tickContext, component.componentId(), age);
            }

            if (component.lifeTicks() > 0 && age >= component.lifeTicks()) {
                if (component.hasPreparedComponent()) {
                    Skills.executeOnEntityExpire(tickContext, component.componentId());
                }
                component.restoreBlockIfPresent(level);
                component.entity().discard();
                removals.add(component.entity().getUUID());
            }
        }

        for (UUID removal : removals) {
            ACTIVE.remove(removal);
        }
    }

    private static void expireRemovedComponent(ActiveSkillComponent component) {
        ServerLevel level = (ServerLevel) component.entity().level();
        SkillExecutionContext expireContext = component.context(
                component.entity(),
                Optional.empty(),
                component.entity().position(),
                Optional.of(component.entity().blockPosition())
        );
        if (component.hasPreparedComponent()) {
            Skills.executeOnEntityExpire(expireContext, component.componentId());
        }
        component.restoreBlockIfPresent(level);
    }

    private static final class ActiveSkillComponent {
        private final Entity entity;
        private final PreparedSkillUse preparedUse;
        private final Entity caster;
        private final Optional<Entity> castTarget;
        private final String componentId;
        private final int lifeTicks;
        private final @Nullable BlockPos blockPos;
        private final @Nullable BlockState previousBlockState;
        private int ageTicks;

        private ActiveSkillComponent(
                Entity entity,
                PreparedSkillUse preparedUse,
                Entity caster,
                Optional<Entity> castTarget,
                String componentId,
                int lifeTicks,
                @Nullable BlockPos blockPos,
                @Nullable BlockState previousBlockState
        ) {
            this.entity = entity;
            this.preparedUse = preparedUse;
            this.caster = caster;
            this.castTarget = castTarget;
            this.componentId = componentId;
            this.lifeTicks = lifeTicks;
            this.blockPos = blockPos;
            this.previousBlockState = previousBlockState;
        }

        private Entity entity() {
            return entity;
        }

        private Entity caster() {
            return caster;
        }

        private String componentId() {
            return componentId;
        }

        private int lifeTicks() {
            return lifeTicks;
        }

        private boolean hasPreparedComponent() {
            return preparedUse.components().containsKey(componentId);
        }

        private int advanceAge() {
            ageTicks++;
            return ageTicks;
        }

        private SkillExecutionContext context(
                Entity source,
                Optional<Entity> target,
                Vec3 impactPosition,
                Optional<BlockPos> impactBlockPos
        ) {
            return new SkillExecutionContext(
                    preparedUse,
                    (ServerLevel) source.level(),
                    caster,
                    source,
                    target.isPresent() ? target : castTarget,
                    caster.position(),
                    impactPosition,
                    impactBlockPos.or(() -> Optional.ofNullable(blockPos))
            );
        }

        private void restoreBlockIfPresent(ServerLevel level) {
            if (blockPos != null && previousBlockState != null) {
                level.setBlock(blockPos, previousBlockState, 3);
            }
        }
    }
}
