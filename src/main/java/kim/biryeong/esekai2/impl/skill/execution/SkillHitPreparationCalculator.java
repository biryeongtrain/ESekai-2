package kim.biryeong.esekai2.impl.skill.execution;

import kim.biryeong.esekai2.api.damage.breakdown.DamageBreakdown;
import kim.biryeong.esekai2.api.damage.breakdown.DamageType;
import kim.biryeong.esekai2.api.damage.calculation.DamageOverTimeCalculation;
import kim.biryeong.esekai2.api.damage.calculation.HitDamageCalculation;
import kim.biryeong.esekai2.api.damage.critical.HitContext;
import kim.biryeong.esekai2.api.damage.critical.HitKind;
import kim.biryeong.esekai2.api.ailment.AilmentType;
import kim.biryeong.esekai2.api.skill.calculation.SkillCalculationDefinition;
import kim.biryeong.esekai2.api.skill.definition.SkillDefinition;
import kim.biryeong.esekai2.api.skill.definition.graph.SkillAction;
import kim.biryeong.esekai2.api.skill.definition.graph.SkillActionType;
import kim.biryeong.esekai2.api.skill.definition.graph.SkillCondition;
import kim.biryeong.esekai2.api.skill.definition.graph.SkillConditionType;
import kim.biryeong.esekai2.api.skill.definition.graph.SkillPredicate;
import kim.biryeong.esekai2.api.skill.definition.graph.SkillRule;
import kim.biryeong.esekai2.api.skill.execution.PreparedApplyBuffAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedApplyAilmentAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedApplyDotAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedDamageAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedProjectileAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedSandstormParticleAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedSkillExecutionRoute;
import kim.biryeong.esekai2.api.skill.execution.PreparedSkillAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedSkillEntityComponent;
import kim.biryeong.esekai2.api.skill.execution.PreparedSkillUse;
import kim.biryeong.esekai2.api.skill.execution.PreparedSoundAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedSummonAtSightAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedSummonBlockAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedTickAction;
import kim.biryeong.esekai2.api.skill.execution.SkillExecutionEvent;
import kim.biryeong.esekai2.api.skill.execution.SkillUseContext;
import kim.biryeong.esekai2.api.skill.value.SkillValueExpression;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Builds a prepared skill use from a data-driven action graph.
 */
public final class SkillHitPreparationCalculator {
    private SkillHitPreparationCalculator() {
    }

    public static PreparedSkillUse prepareUse(SkillDefinition skill, SkillUseContext context) {
        Objects.requireNonNull(skill, "skill");
        Objects.requireNonNull(context, "context");

        SkillSupportMerger.SupportMergeResult mergeResult = SkillSupportMerger.merge(skill, context);
        SkillDefinition mergedSkill = mergeResult.skill();
        SkillUseContext mergedContext = context.withAdditionalConditionalModifiers(mergeResult.additionalConditionalModifiers());

        double resourceCost = SkillRuntimeResolver.resolveResourceCost(mergedSkill, mergedContext);
        int useTimeTicks = SkillRuntimeResolver.resolveUseTimeTicks(mergedSkill, mergedContext);
        int cooldownTicks = SkillRuntimeResolver.resolveCooldownTicks(mergedSkill, mergedContext);
        List<String> warnings = new ArrayList<>(mergeResult.warnings());

        List<PreparedSkillExecutionRoute> onCastRoutes = parseRoutesFromRules(
                mergedSkill.attached().onCast(),
                SkillExecutionEvent.ON_CAST,
                mergedContext,
                warnings
        );
        ParsedComponents parsedComponents = parseComponents(mergedSkill.attached().entityComponents(), mergedContext, warnings);

        return new PreparedSkillUse(
                mergedSkill,
                mergedContext,
                resourceCost,
                useTimeTicks,
                cooldownTicks,
                onCastRoutes,
                parsedComponents.onSpellCastRoutes(),
                parsedComponents.components(),
                warnings
        );
    }

    private static List<PreparedSkillExecutionRoute> parseRoutesFromRules(
            List<SkillRule> rules,
            SkillExecutionEvent event,
            SkillUseContext context,
            List<String> warnings
    ) {
        List<PreparedSkillExecutionRoute> routes = new ArrayList<>();
        for (SkillRule rule : rules) {
            List<PreparedSkillAction> actions = parseActionList(rule.acts(), context, warnings);
            if (actions.isEmpty()) {
                continue;
            }
            routes.add(new PreparedSkillExecutionRoute(event, rule.targets(), actions, rule.enPreds(), 0));
        }
        return List.copyOf(routes);
    }

    private static ParsedComponents parseComponents(
            Map<String, List<SkillRule>> entityComponents,
            SkillUseContext context,
            List<String> warnings
    ) {
        Map<String, PreparedSkillEntityComponent> parsed = new LinkedHashMap<>();
        List<PreparedSkillExecutionRoute> spellCastRoutes = new ArrayList<>();
        for (Map.Entry<String, List<SkillRule>> componentEntry : entityComponents.entrySet()) {
            String componentId = componentEntry.getKey();
            List<PreparedSkillExecutionRoute> onSpellCastRoutes = new ArrayList<>();
            List<PreparedSkillExecutionRoute> onHitRoutes = new ArrayList<>();
            List<PreparedSkillExecutionRoute> onExpireRoutes = new ArrayList<>();
            List<PreparedSkillExecutionRoute> tickRoutes = new ArrayList<>();

        for (SkillRule rule : componentEntry.getValue()) {
            List<SkillExecutionEvent> events = resolveRuleEvents(rule);
            List<PreparedSkillAction> actions = parseActionList(rule.acts(), context, warnings);
            int tickInterval = resolveTickInterval(rule.ifs(), context);

                if (actions.isEmpty()) {
                    continue;
                }

                for (SkillExecutionEvent event : events) {
                    PreparedSkillExecutionRoute route = new PreparedSkillExecutionRoute(
                            event,
                            rule.targets(),
                            actions,
                            rule.enPreds(),
                            event == SkillExecutionEvent.ON_TICK_CONDITION ? Math.max(1, tickInterval) : 0
                    );
                    switch (event) {
                        case ON_SPELL_CAST -> {
                            onSpellCastRoutes.add(route);
                            spellCastRoutes.add(route);
                        }
                        case ON_HIT -> onHitRoutes.add(route);
                        case ON_ENTITY_EXPIRE -> onExpireRoutes.add(route);
                        case ON_TICK_CONDITION -> tickRoutes.add(route);
                        case ON_CAST -> {
                        }
                    }
                }
            }

            parsed.put(
                    componentId,
                    new PreparedSkillEntityComponent(componentId, onSpellCastRoutes, onHitRoutes, onExpireRoutes, tickRoutes)
            );
        }

        return new ParsedComponents(Map.copyOf(parsed), List.copyOf(spellCastRoutes));
    }

    private static List<SkillExecutionEvent> resolveRuleEvents(SkillRule rule) {
        List<SkillExecutionEvent> events = new ArrayList<>();
        if (rule.ifs().isEmpty()) {
            events.add(SkillExecutionEvent.ON_HIT);
            return events;
        }

        for (SkillCondition condition : rule.ifs()) {
            if (condition.type() == SkillConditionType.ON_SPELL_CAST) {
                events.add(SkillExecutionEvent.ON_SPELL_CAST);
            } else
            if (condition.type() == SkillConditionType.ON_HIT) {
                events.add(SkillExecutionEvent.ON_HIT);
            } else if (condition.type() == SkillConditionType.ON_ENTITY_EXPIRE) {
                events.add(SkillExecutionEvent.ON_ENTITY_EXPIRE);
            } else if (condition.type() == SkillConditionType.X_TICKS_CONDITION) {
                events.add(SkillExecutionEvent.ON_TICK_CONDITION);
            }
        }

        if (events.isEmpty()) {
            events.add(SkillExecutionEvent.ON_HIT);
        }

        return List.copyOf(events);
    }

    private static int resolveTickInterval(List<SkillCondition> conditions) {
        return resolveTickInterval(conditions, null);
    }

    private static int resolveTickInterval(List<SkillCondition> conditions, SkillUseContext context) {
        for (SkillCondition condition : conditions) {
            if (condition.type() != SkillConditionType.X_TICKS_CONDITION) {
                continue;
            }

            if (context == null) {
                continue;
            }

            int ticks = (int) Math.round(condition.interval().resolve(context));
            if (ticks > 0) {
                return ticks;
            }
        }
        return 1;
    }

    private static List<PreparedSkillAction> parseActionList(
            List<SkillAction> acts,
            SkillUseContext context,
            List<String> warnings
    ) {
        List<PreparedSkillAction> prepared = new ArrayList<>();
        for (SkillAction action : acts) {
            PreparedSkillAction parsed = parseAction(action, context, warnings);
            if (parsed != null) {
                prepared.add(parsed);
            }
        }
        return prepared;
    }

    private static PreparedSkillAction parseAction(SkillAction action, SkillUseContext context, List<String> warnings) {
        SkillActionType type = action.type();

        return switch (type) {
            case SOUND -> parseSound(action, context, warnings);
            case DAMAGE -> parseDamage(action, context, warnings);
            case APPLY_BUFF -> parseApplyBuff(action, context, warnings);
            case APPLY_AILMENT -> parseApplyAilment(action, context, warnings);
            case APPLY_DOT -> parseApplyDot(action, context, warnings);
            case PROJECTILE -> parseProjectile(action, context, warnings);
            case SUMMON_AT_SIGHT -> parseSummonAtSight(action, context, warnings);
            case SUMMON_BLOCK -> parseSummonBlock(action, context, warnings);
            case SANDSTORM_PARTICLE -> parseSandstormParticle(action, context, warnings);
        };
    }

    private static PreparedSoundAction parseSound(SkillAction action, SkillUseContext context, List<String> warnings) {
        Identifier soundId = parseIdentifier(action.soundId());
        if (soundId == null) {
            warnings.add("sound action requires a valid identifier");
            return null;
        }
        float volume = (float) resolveValue(action.volume(), context, 1.0);
        float pitch = (float) resolveValue(action.pitch(), context, 1.0);
        return new PreparedSoundAction(soundId, volume, pitch, action.enPreds());
    }

    private static PreparedDamageAction parseDamage(
            SkillAction action,
            SkillUseContext context,
            List<String> warnings
    ) {
        String calculationId = action.calculationId();
        SkillCalculationDefinition calculationDefinition = resolveCalculationDefinition(calculationId, context, warnings);
        HitKind hitKind = action.hitKind();
        if (calculationDefinition != null && hitKind == HitKind.ATTACK) {
            hitKind = calculationDefinition.hitKind();
        }

        DamageBreakdown baseDamage = calculationDefinition != null
                ? calculationDefinition.resolveBaseDamage(context)
                : DamageBreakdown.empty();
        baseDamage = mergeInlineBaseDamage(baseDamage, action.baseDamage(), context);

        double baseCriticalStrikeChance = calculationDefinition != null
                ? calculationDefinition.resolveBaseCriticalStrikeChance(context)
                : 0.0;
        if (calculationDefinition == null || shouldOverride(action.baseCriticalStrikeChance(), 0.0)) {
            baseCriticalStrikeChance = clampPercent(action.baseCriticalStrikeChance().resolve(context));
        }

        double baseCriticalStrikeMultiplier = calculationDefinition != null
                ? calculationDefinition.resolveBaseCriticalStrikeMultiplier(context)
                : 100.0;
        if (calculationDefinition == null || shouldOverride(action.baseCriticalStrikeMultiplier(), 100.0)) {
            baseCriticalStrikeMultiplier = clampMultiplier(action.baseCriticalStrikeMultiplier().resolve(context));
        }

        return new PreparedDamageAction(new HitDamageCalculation(
                baseDamage,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                context.attackerStats(),
                new HitContext(
                        hitKind,
                        context.hitRoll(),
                        context.criticalStrikeRoll(),
                        baseCriticalStrikeChance,
                        baseCriticalStrikeMultiplier
                ),
                context.defenderStats()
                ), calculationId, action.enPreds());
    }

    private static PreparedApplyBuffAction parseApplyBuff(
            SkillAction action,
            SkillUseContext context,
            List<String> warnings
    ) {
        Identifier effectId = parseIdentifier(action.effectId());
        if (effectId == null) {
            warnings.add("apply_buff action requires a valid effect_id");
            return null;
        }

        int durationTicks = resolveInt(action.durationTicks(), context, 0);
        if (durationTicks <= 0) {
            warnings.add("apply_buff action requires duration_ticks > 0");
            return null;
        }

        int amplifier = Math.max(0, resolveInt(action.amplifier(), context, 0));
        return new PreparedApplyBuffAction(
                effectId,
                durationTicks,
                amplifier,
                action.ambient(),
                action.showParticles(),
                action.showIcon(),
                action.enPreds()
        );
    }

    private static PreparedApplyDotAction parseApplyDot(
            SkillAction action,
            SkillUseContext context,
            List<String> warnings
    ) {
        String dotId = readString(action.dotId());
        if (dotId == null) {
            warnings.add("apply_dot action requires dot_id");
            return null;
        }

        int durationTicks = resolveInt(action.durationTicks(), context, 0);
        if (durationTicks <= 0) {
            warnings.add("apply_dot action requires duration_ticks > 0");
            return null;
        }

        int tickIntervalTicks = resolveInt(action.tickIntervalTicks(), context, 1);
        if (tickIntervalTicks <= 0) {
            warnings.add("apply_dot action requires tick_interval > 0");
            return null;
        }

        DamageBreakdown baseDamage = mergeInlineBaseDamage(DamageBreakdown.empty(), action.baseDamage(), context);
        if (baseDamage.isEmpty()) {
            warnings.add("apply_dot action requires base_damage");
            return null;
        }

        return new PreparedApplyDotAction(
                dotId,
                new DamageOverTimeCalculation(
                        baseDamage,
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        context.defenderStats()
                ),
                durationTicks,
                tickIntervalTicks,
                action.enPreds()
        );
    }

    private static PreparedApplyAilmentAction parseApplyAilment(
            SkillAction action,
            SkillUseContext context,
            List<String> warnings
    ) {
        String rawAilmentId = readString(action.ailmentId());
        if (rawAilmentId == null) {
            warnings.add("apply_ailment action requires ailment_id");
            return null;
        }

        AilmentType ailmentType = parseAilmentType(rawAilmentId);
        if (ailmentType == null) {
            warnings.add("apply_ailment action requires a valid ailment_id");
            return null;
        }

        int durationTicks = resolveInt(action.durationTicks(), context, 0);
        if (durationTicks <= 0) {
            warnings.add("apply_ailment action requires duration_ticks > 0");
            return null;
        }

        double chancePercent = resolveValue(action.chance(), context, 100.0);
        if (!Double.isFinite(chancePercent) || chancePercent <= 0.0) {
            warnings.add("apply_ailment action requires chance > 0");
            return null;
        }

        double potencyMultiplierPercent = resolveValue(action.potencyMultiplier(), context, 100.0);
        if (!Double.isFinite(potencyMultiplierPercent) || potencyMultiplierPercent < 0.0) {
            warnings.add("apply_ailment action requires potency_multiplier >= 0");
            return null;
        }

        return new PreparedApplyAilmentAction(
                ailmentType,
                Math.min(100.0, chancePercent),
                durationTicks,
                potencyMultiplierPercent,
                action.enPreds()
        );
    }

    private static SkillCalculationDefinition resolveCalculationDefinition(
            String calculationId,
            SkillUseContext context,
            List<String> warnings
    ) {
        if (calculationId == null || calculationId.isBlank()) {
            return null;
        }

        Identifier id = Identifier.tryParse(calculationId);
        if (id == null) {
            warnings.add("damage action calculation_id must be a valid identifier: " + calculationId);
            return null;
        }

        return context.calculationLookup().resolve(id).orElseGet(() -> {
            warnings.add("Unknown skill calculation: " + calculationId);
            return null;
        });
    }

    private static PreparedProjectileAction parseProjectile(SkillAction action, SkillUseContext context, List<String> warnings) {
        String componentId = readString(action.componentId());
        Identifier projectileEntityId = parseIdentifier(action.entityId());
        if (componentId == null || projectileEntityId == null) {
            warnings.add("projectile action requires entity_name and proj_en");
            return null;
        }
        int lifeTicks = resolveInt(action.lifeTicks(), context, 0);
        boolean gravity = action.gravity();
        return new PreparedProjectileAction(componentId, projectileEntityId.toString(), lifeTicks, gravity, action.enPreds());
    }

    private static PreparedSummonAtSightAction parseSummonAtSight(SkillAction action, SkillUseContext context, List<String> warnings) {
        String componentId = readString(action.componentId());
        Identifier summonEntityId = parseIdentifier(action.entityId());
        if (componentId == null || summonEntityId == null) {
            warnings.add("summon_at_sight action requires entity_name and proj_en");
            return null;
        }
        int lifeTicks = resolveInt(action.lifeTicks(), context, 0);
        boolean gravity = action.gravity();
        return new PreparedSummonAtSightAction(componentId, summonEntityId.toString(), lifeTicks, gravity, action.enPreds());
    }

    private static PreparedSummonBlockAction parseSummonBlock(SkillAction action, SkillUseContext context, List<String> warnings) {
        String componentId = readString(action.componentId());
        String blockId = readString(action.blockId());
        if (componentId == null || blockId == null) {
            warnings.add("summon_block action requires entity_name and block");
            return null;
        }
        int lifeTicks = resolveInt(action.lifeTicks(), context, 0);
        return new PreparedSummonBlockAction(componentId, blockId, lifeTicks, action.enPreds());
    }

    private static PreparedSandstormParticleAction parseSandstormParticle(
            SkillAction action,
            SkillUseContext context,
            List<String> warnings
    ) {
        Identifier id = parseIdentifier(action.particleId());
        if (id == null) {
            warnings.add("sandstorm_particle action requires particle_id");
            return null;
        }
        String anchor = readString(action.anchor());
        if (anchor == null) {
            anchor = "self";
        }
        double offsetX = resolveValue(action.offsetX(), context, 0.0);
        double offsetY = resolveValue(action.offsetY(), context, 0.0);
        double offsetZ = resolveValue(action.offsetZ(), context, 0.0);
        return new PreparedSandstormParticleAction(id, anchor, offsetX, offsetY, offsetZ, action.enPreds());
    }

    private static DamageBreakdown mergeInlineBaseDamage(
            DamageBreakdown base,
            Map<DamageType, SkillValueExpression> parameters,
            SkillUseContext context
    ) {
        DamageBreakdown merged = base == null ? DamageBreakdown.empty() : base;

        for (Map.Entry<DamageType, SkillValueExpression> entry : parameters.entrySet()) {
            double resolved = entry.getValue().resolve(context);
            if (Double.isFinite(resolved)) {
                merged = merged.with(entry.getKey(), resolved);
            }
        }

        return merged;
    }

    private static String readString(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw;
    }

    private static Identifier parseIdentifier(String raw) {
        String value = readString(raw);
        if (value == null) {
            return null;
        }
        return Identifier.tryParse(value);
    }

    private static AilmentType parseAilmentType(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        for (AilmentType type : AilmentType.values()) {
            if (type.serializedName().equals(raw)) {
                return type;
            }
        }
        return null;
    }

    private static double resolveValue(SkillValueExpression expression, SkillUseContext context, double fallback) {
        double resolved = expression.resolve(context);
        return Double.isFinite(resolved) ? resolved : fallback;
    }

    private static int resolveInt(SkillValueExpression expression, SkillUseContext context, int fallback) {
        double resolved = expression.resolve(context);
        if (!Double.isFinite(resolved)) {
            return fallback;
        }
        return (int) Math.round(resolved);
    }

    private static double clampPercent(double value) {
        if (!Double.isFinite(value)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(100.0, value));
    }

    private static double clampMultiplier(double value) {
        if (!Double.isFinite(value) || value < 100.0) {
            return 100.0;
        }
        return value;
    }

    private static boolean shouldOverride(SkillValueExpression expression, double defaultConstant) {
        return !expression.isConstant() || expression.constant() != defaultConstant;
    }

    private record ParsedComponents(
            Map<String, PreparedSkillEntityComponent> components,
            List<PreparedSkillExecutionRoute> onSpellCastRoutes
    ) {
    }
}
