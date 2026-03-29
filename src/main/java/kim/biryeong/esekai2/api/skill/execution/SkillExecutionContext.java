package kim.biryeong.esekai2.api.skill.execution;

import kim.biryeong.esekai2.api.skill.definition.graph.SkillTargetSelector;
import kim.biryeong.esekai2.api.skill.definition.graph.SkillTargetType;
import kim.biryeong.esekai2.api.skill.definition.graph.SkillPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Runtime execution context layered on top of a prepared skill use.
 *
 * @param preparedUse prepared skill snapshot to execute
 * @param level server world used for all side effects
 * @param caster original entity that initiated the skill use
 * @param source current source entity for this execution pass
 * @param target optional primary target, when the cast has one
 * @param origin world-space cast origin
 * @param impactPosition world-space impact or fallback position
 * @param impactBlockPos optional block position for block-based effects
 */
public record SkillExecutionContext(
        PreparedSkillUse preparedUse,
        ServerLevel level,
        Entity caster,
        Entity source,
        Optional<Entity> target,
        Vec3 origin,
        Vec3 impactPosition,
        Optional<BlockPos> impactBlockPos
) {
    public SkillExecutionContext {
        Objects.requireNonNull(preparedUse, "preparedUse");
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(caster, "caster");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(impactPosition, "impactPosition");
        Objects.requireNonNull(impactBlockPos, "impactBlockPos");
    }

    public static SkillExecutionContext forCast(
            PreparedSkillUse preparedUse,
            ServerLevel level,
            Entity caster,
            Optional<Entity> target
    ) {
        Objects.requireNonNull(preparedUse, "preparedUse");
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(caster, "caster");
        Objects.requireNonNull(target, "target");

        Vec3 origin = caster.getEyePosition();
        Vec3 impact = target.map(Entity::position).orElse(origin);
        return new SkillExecutionContext(
                preparedUse,
                level,
                caster,
                caster,
                target,
                origin,
                impact,
                target.map(Entity::blockPosition)
        );
    }

    /**
     * Returns the primary target or falls back to the caster when no target exists.
     *
     * @return primary target entity
     */
    public Entity primaryTarget() {
        return target.orElse(source);
    }

    /**
     * Resolves a selector set into a stable, deduplicated entity list.
     *
     * @param selectors target selectors attached to a prepared rule
     * @return resolved entity list
     */
    public List<Entity> resolveTargets(Set<SkillTargetSelector> selectors) {
        Objects.requireNonNull(selectors, "selectors");
        if (selectors.isEmpty()) {
            return List.of(primaryTarget());
        }

        LinkedHashSet<Entity> resolved = new LinkedHashSet<>();
        for (SkillTargetSelector selector : selectors) {
            if (!matches(selector.enPreds())) {
                continue;
            }
            resolved.addAll(resolveTargets(selector));
        }

        if (resolved.isEmpty()) {
            resolved.add(primaryTarget());
        }

        return List.copyOf(resolved);
    }

    /**
     * Resolves a single selector against the current world state.
     *
     * @param selector selector to evaluate
     * @return resolved entity list
     */
    public List<Entity> resolveTargets(SkillTargetSelector selector) {
        Objects.requireNonNull(selector, "selector");

        return switch (selector.type()) {
            case SELF -> List.of(source);
            case TARGET -> target.map(List::of).orElseGet(() -> List.of(source));
            case AOE -> resolveAreaTargets(selector);
        };
    }

    /**
     * Resolves the block position used by block placement or particle anchor fallbacks.
     *
     * @return preferred block position for the current execution
     */
    public BlockPos resolveImpactBlockPos() {
        return impactBlockPos.orElseGet(() -> BlockPos.containing(impactPosition));
    }

    private List<Entity> resolveAreaTargets(SkillTargetSelector selector) {
        double radius = readRadius(selector, this);
        if (!Double.isFinite(radius) || radius <= 0.0) {
            return List.of(primaryTarget());
        }

        Vec3 center = target.map(Entity::position).orElse(impactPosition);
        AABB area = new AABB(center, center).inflate(radius);
        List<Entity> resolved = new ArrayList<>(level.getEntities(source, area, entity -> entity.isAlive() && entity != source));

        if (resolved.isEmpty()) {
            resolved.add(primaryTarget());
        }

        return List.copyOf(resolved);
    }

    private static double readRadius(SkillTargetSelector selector, SkillExecutionContext context) {
        double resolved = selector.radius().resolve(context.preparedUse().useContext());
        if (!Double.isFinite(resolved) || resolved <= 0.0) {
            return 3.0;
        }
        return resolved;
    }

    private boolean matches(List<SkillPredicate> predicates) {
        for (SkillPredicate predicate : predicates) {
            if (!predicate.matches(this)) {
                return false;
            }
        }
        return true;
    }
}
