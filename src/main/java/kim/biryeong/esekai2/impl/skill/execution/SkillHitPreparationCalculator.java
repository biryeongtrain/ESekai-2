package kim.biryeong.esekai2.impl.skill.execution;

import kim.biryeong.esekai2.api.damage.breakdown.DamageBreakdown;
import kim.biryeong.esekai2.api.damage.breakdown.DamageType;
import kim.biryeong.esekai2.api.damage.calculation.HitDamageCalculation;
import kim.biryeong.esekai2.api.damage.critical.HitContext;
import kim.biryeong.esekai2.api.damage.critical.HitKind;
import kim.biryeong.esekai2.api.skill.definition.SkillDefinition;
import kim.biryeong.esekai2.api.skill.definition.graph.SkillAction;
import kim.biryeong.esekai2.api.skill.definition.graph.SkillActionType;
import kim.biryeong.esekai2.api.skill.definition.graph.SkillCondition;
import kim.biryeong.esekai2.api.skill.definition.graph.SkillConditionType;
import kim.biryeong.esekai2.api.skill.definition.graph.SkillRule;
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

        double resourceCost = SkillRuntimeResolver.resolveResourceCost(skill, context);
        int useTimeTicks = SkillRuntimeResolver.resolveUseTimeTicks(skill, context);
        int cooldownTicks = SkillRuntimeResolver.resolveCooldownTicks(skill, context);
        List<String> warnings = new ArrayList<>();

        List<PreparedSkillExecutionRoute> onCastRoutes = parseRoutesFromRules(
                skill.attached().onCast(),
                SkillExecutionEvent.ON_CAST,
                context,
                warnings
        );
        Map<String, PreparedSkillEntityComponent> components = parseComponents(skill.attached().entityComponents(), context, warnings);

        return new PreparedSkillUse(
                skill,
                resourceCost,
                useTimeTicks,
                cooldownTicks,
                onCastRoutes,
                components,
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
            routes.add(new PreparedSkillExecutionRoute(event, rule.targets(), actions, 0));
        }
        return List.copyOf(routes);
    }

    private static Map<String, PreparedSkillEntityComponent> parseComponents(
            Map<String, List<SkillRule>> entityComponents,
            SkillUseContext context,
            List<String> warnings
    ) {
        Map<String, PreparedSkillEntityComponent> parsed = new LinkedHashMap<>();
        for (Map.Entry<String, List<SkillRule>> componentEntry : entityComponents.entrySet()) {
            String componentId = componentEntry.getKey();
            List<PreparedSkillExecutionRoute> onHitRoutes = new ArrayList<>();
            List<PreparedSkillExecutionRoute> onExpireRoutes = new ArrayList<>();
            List<PreparedSkillExecutionRoute> tickRoutes = new ArrayList<>();

            for (SkillRule rule : componentEntry.getValue()) {
                List<SkillExecutionEvent> events = resolveRuleEvents(rule);
                List<PreparedSkillAction> actions = parseActionList(rule.acts(), context, warnings);
                int tickInterval = resolveTickInterval(rule.ifs());

                if (actions.isEmpty()) {
                    continue;
                }

                for (SkillExecutionEvent event : events) {
                    PreparedSkillExecutionRoute route = new PreparedSkillExecutionRoute(
                            event,
                            rule.targets(),
                            actions,
                            event == SkillExecutionEvent.ON_TICK_CONDITION ? Math.max(1, tickInterval) : 0
                    );
                    switch (event) {
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
                    new PreparedSkillEntityComponent(componentId, onHitRoutes, onExpireRoutes, tickRoutes)
            );
        }

        return Map.copyOf(parsed);
    }

    private static List<SkillExecutionEvent> resolveRuleEvents(SkillRule rule) {
        List<SkillExecutionEvent> events = new ArrayList<>();
        if (rule.ifs().isEmpty()) {
            events.add(SkillExecutionEvent.ON_HIT);
            return events;
        }

        for (SkillCondition condition : rule.ifs()) {
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
        for (SkillCondition condition : conditions) {
            if (condition.type() != SkillConditionType.X_TICKS_CONDITION) {
                continue;
            }

            String ticksRaw = readString(condition.parameters().get("x_ticks"));
            if (ticksRaw == null) {
                ticksRaw = readString(condition.parameters().get("ticks"));
            }
            if (ticksRaw == null) {
                ticksRaw = readString(condition.parameters().get("tick_rate"));
            }
            int ticks = readInt(ticksRaw, -1);
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
        Map<String, String> parameters = action.parameters();

        return switch (type) {
            case SOUND -> parseSound(parameters, warnings);
            case DAMAGE -> parseDamage(parameters, context, warnings);
            case PROJECTILE -> parseProjectile(parameters, warnings);
            case SUMMON_AT_SIGHT -> parseSummonAtSight(parameters, warnings);
            case SUMMON_BLOCK -> parseSummonBlock(parameters, warnings);
            case SANDSTORM_PARTICLE -> parseSandstormParticle(parameters, warnings);
        };
    }

    private static PreparedSoundAction parseSound(Map<String, String> parameters, List<String> warnings) {
        Identifier soundId = parseIdentifier(parameters, "sound", "id", "identifier");
        if (soundId == null) {
            warnings.add("sound action requires a valid identifier");
            return null;
        }
        float volume = readFloat(parameters.get("volume"), 1.0f);
        float pitch = readFloat(parameters.get("pitch"), 1.0f);
        return new PreparedSoundAction(soundId, volume, pitch);
    }

    private static PreparedDamageAction parseDamage(
            Map<String, String> parameters,
            SkillUseContext context,
            List<String> warnings
    ) {
        HitKind hitKind = parseHitKind(readString(parameters.get("hit_kind")));
        DamageBreakdown baseDamage = parseBaseDamage(parameters, warnings);
        double baseCriticalStrikeChance = readPercent(parameters.get("base_critical_strike_chance"));
        double baseCriticalStrikeMultiplier = readMultiplier(parameters.get("base_critical_strike_multiplier"));
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
        ));
    }

    private static PreparedProjectileAction parseProjectile(Map<String, String> parameters, List<String> warnings) {
        String componentId = readString(parameters.get("entity_name"));
        Identifier projectileEntityId = parseIdentifier(parameters, "proj_en", "projectile", "id", "identifier", "path");
        if (componentId == null || projectileEntityId == null) {
            warnings.add("projectile action requires entity_name and proj_en");
            return null;
        }
        int lifeTicks = readInt(parameters.get("life_ticks"), 0);
        boolean gravity = Boolean.parseBoolean(parameters.getOrDefault("gravity", "false"));
        return new PreparedProjectileAction(componentId, projectileEntityId.toString(), lifeTicks, gravity);
    }

    private static PreparedSummonAtSightAction parseSummonAtSight(Map<String, String> parameters, List<String> warnings) {
        String componentId = readString(parameters.get("entity_name"));
        Identifier summonEntityId = parseIdentifier(parameters, "proj_en", "summon_at_sight", "summon", "id", "identifier");
        if (componentId == null || summonEntityId == null) {
            warnings.add("summon_at_sight action requires entity_name and proj_en");
            return null;
        }
        int lifeTicks = readInt(parameters.get("life_ticks"), 0);
        boolean gravity = Boolean.parseBoolean(parameters.getOrDefault("gravity", "false"));
        return new PreparedSummonAtSightAction(componentId, summonEntityId.toString(), lifeTicks, gravity);
    }

    private static PreparedSummonBlockAction parseSummonBlock(Map<String, String> parameters, List<String> warnings) {
        String componentId = readString(parameters.get("entity_name"));
        String blockId = readString(parameters.get("block"));
        if (componentId == null || blockId == null) {
            warnings.add("summon_block action requires entity_name and block");
            return null;
        }
        int lifeTicks = readInt(parameters.get("life_ticks"), 0);
        return new PreparedSummonBlockAction(componentId, blockId, lifeTicks);
    }

    private static PreparedSandstormParticleAction parseSandstormParticle(
            Map<String, String> parameters,
            List<String> warnings
    ) {
        Identifier id = parseIdentifier(parameters, "sandstorm_particle", "particle_id", "id", "identifier");
        if (id == null) {
            warnings.add("sandstorm_particle action requires particle_id");
            return null;
        }
        String anchor = readString(parameters.get("anchor"));
        if (anchor == null) {
            anchor = "self";
        }
        double offsetX = readDouble(parameters.get("offset_x"), 0.0);
        double offsetY = readDouble(parameters.get("offset_y"), 0.0);
        double offsetZ = readDouble(parameters.get("offset_z"), 0.0);
        return new PreparedSandstormParticleAction(id, anchor, offsetX, offsetY, offsetZ);
    }

    private static HitKind parseHitKind(String kindRaw) {
        if (kindRaw == null) {
            return HitKind.ATTACK;
        }
        for (HitKind kind : HitKind.values()) {
            if (kind.serializedName().equals(kindRaw)) {
                return kind;
            }
        }
        return HitKind.ATTACK;
    }

    private static DamageBreakdown parseBaseDamage(Map<String, String> parameters, List<String> warnings) {
        DamageBreakdown base = DamageBreakdown.empty();

        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("base_damage_")) {
                DamageType type = parseDamageType(key.substring("base_damage_".length()), warnings);
                if (type != null) {
                    base = base.with(type, readDouble(entry.getValue(), 0.0));
                }
            }
        }

        return base;
    }

    private static DamageType parseDamageType(String rawType, List<String> warnings) {
        if (rawType == null || rawType.isBlank()) {
            return null;
        }

        for (DamageType damageType : DamageType.values()) {
            if (damageType.id().equals(rawType)) {
                return damageType;
            }
        }
        if (warnings != null) {
            warnings.add("Unknown damage type in skill action payload: " + rawType);
        }
        return null;
    }

    private static double readDouble(String raw, double fallback) {
        double parsed = readDoubleOrNaN(raw);
        return Double.isFinite(parsed) ? parsed : fallback;
    }

    private static double readPercent(String raw) {
        double value = readDoubleOrNaN(raw);
        if (!Double.isFinite(value)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(100.0, value));
    }

    private static double readMultiplier(String raw) {
        double value = readDoubleOrNaN(raw);
        if (!Double.isFinite(value) || value < 100.0) {
            return 100.0;
        }
        return value;
    }

    private static double readDoubleOrNaN(String raw) {
        if (raw == null || raw.isBlank()) {
            return Double.NaN;
        }
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException exception) {
            return Double.NaN;
        }
    }

    private static float readFloat(String raw, float fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Float.parseFloat(raw);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static int readInt(String raw, int fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static String readString(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw;
    }

    private static Identifier parseIdentifier(Map<String, String> parameters, String...keys) {
        for (String key : keys) {
            String raw = readString(parameters.get(key));
            if (raw == null) {
                continue;
            }
            Identifier parsed = Identifier.tryParse(raw);
            if (parsed != null) {
                return parsed;
            }
        }
        return null;
    }
}
