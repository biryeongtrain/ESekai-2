package kim.biryeong.esekai2.impl.skill.execution;

import kim.biryeong.esekai2.api.damage.breakdown.DamageType;
import kim.biryeong.esekai2.api.damage.critical.HitKind;
import kim.biryeong.esekai2.api.skill.definition.SkillAttached;
import kim.biryeong.esekai2.api.skill.definition.SkillConfig;
import kim.biryeong.esekai2.api.skill.definition.SkillDefinition;
import kim.biryeong.esekai2.api.skill.definition.graph.SkillAction;
import kim.biryeong.esekai2.api.skill.support.SkillActionFieldOverride;
import kim.biryeong.esekai2.api.skill.support.SkillActionFieldPathType;
import kim.biryeong.esekai2.api.skill.definition.graph.SkillRule;
import kim.biryeong.esekai2.api.skill.execution.SkillUseContext;
import kim.biryeong.esekai2.api.skill.support.SkillActionOverride;
import kim.biryeong.esekai2.api.skill.support.SkillSupportDefinition;
import kim.biryeong.esekai2.api.skill.support.SkillSupportEffect;
import kim.biryeong.esekai2.api.skill.support.SkillSupportRuleAppend;
import kim.biryeong.esekai2.api.skill.support.SkillSupportRuleTargetType;
import kim.biryeong.esekai2.api.skill.tag.SkillTag;
import kim.biryeong.esekai2.api.skill.tag.SkillTags;
import kim.biryeong.esekai2.api.skill.value.SkillValueExpression;
import kim.biryeong.esekai2.api.stat.modifier.ConditionalStatModifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Applies linked support definitions to a base skill definition before runtime preparation.
 */
public final class SkillSupportMerger {
    private SkillSupportMerger() {
    }

    public static SupportMergeResult merge(SkillDefinition skill, SkillUseContext context) {
        Objects.requireNonNull(skill, "skill");
        Objects.requireNonNull(context, "context");
        if (context.linkedSupports().isEmpty()) {
            return new SupportMergeResult(skill, List.of(), List.of());
        }

        Set<SkillTag> mergedTags = new LinkedHashSet<>(skill.config().tags());
        SkillAttached mergedAttached = skill.attached();
        List<ConditionalStatModifier> additionalConditionalModifiers = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        for (SkillSupportDefinition support : context.linkedSupports()) {
            for (SkillSupportEffect effect : support.effects()) {
                if (!effect.matches(Set.copyOf(mergedTags))) {
                    continue;
                }

                mergedTags.addAll(effect.addedTags());
                additionalConditionalModifiers.addAll(effect.addedConditionalStatModifiers());
                ApplyEffectResult applyResult = applyEffect(mergedAttached, effect);
                mergedAttached = applyResult.attached();
                warnings.addAll(applyResult.warnings());
            }
        }

        SkillConfig mergedConfig = new SkillConfig(
                skill.config().castingWeapon(),
                skill.config().resourceCost(),
                skill.config().castTimeTicks(),
                skill.config().cooldownTicks(),
                skill.config().style(),
                skill.config().castType(),
                skill.config().swingArm(),
                skill.config().applyCastSpeedToCooldown(),
                skill.config().timesToCast(),
                skill.config().charges(),
                skill.config().chargeRegen(),
                SkillTags.copyOf(Set.copyOf(mergedTags))
        );

        SkillDefinition mergedSkill = new SkillDefinition(
                skill.identifier(),
                mergedConfig,
                mergedAttached,
                skill.manualTip(),
                skill.disabledDims(),
                skill.effectTip()
        );
        return new SupportMergeResult(mergedSkill, List.copyOf(additionalConditionalModifiers), List.copyOf(warnings));
    }

    private static ApplyEffectResult applyEffect(SkillAttached attached, SkillSupportEffect effect) {
        List<SkillRule> mergedOnCast = mergeRules(attached.onCast(), effect);
        Map<String, List<SkillRule>> mergedComponents = new LinkedHashMap<>();
        for (Map.Entry<String, List<SkillRule>> entry : attached.entityComponents().entrySet()) {
            mergedComponents.put(entry.getKey(), mergeRules(entry.getValue(), effect));
        }

        List<String> warnings = new ArrayList<>();
        for (SkillSupportRuleAppend appendedRule : effect.appendedRules()) {
            if (appendedRule.target().type() == SkillSupportRuleTargetType.ON_CAST) {
                mergedOnCast = appendRules(mergedOnCast, appendedRule.rules());
                continue;
            }

            String componentId = appendedRule.target().componentId();
            if (!mergedComponents.containsKey(componentId)) {
                warnings.add("Support appended rules target unknown entity component: " + componentId);
                continue;
            }

            mergedComponents.put(componentId, appendRules(mergedComponents.get(componentId), appendedRule.rules()));
        }

        return new ApplyEffectResult(new SkillAttached(mergedOnCast, Map.copyOf(mergedComponents)), warnings);
    }

    private static List<SkillRule> mergeRules(List<SkillRule> rules, SkillSupportEffect effect) {
        List<SkillRule> merged = new ArrayList<>(rules.size());
        for (SkillRule rule : rules) {
            merged.add(mergeRule(rule, effect));
        }
        return List.copyOf(merged);
    }

    private static List<SkillRule> appendRules(List<SkillRule> rules, List<SkillRule> appendedRules) {
        List<SkillRule> merged = new ArrayList<>(rules.size() + appendedRules.size());
        merged.addAll(rules);
        merged.addAll(appendedRules);
        return List.copyOf(merged);
    }

    private static SkillRule mergeRule(SkillRule rule, SkillSupportEffect effect) {
        boolean matchedOverride = false;
        List<SkillAction> mergedActions = new ArrayList<>();
        for (SkillAction action : rule.acts()) {
            SkillAction mergedAction = action;
            for (SkillActionOverride override : effect.actionParameterOverrides()) {
                if (override.matches(mergedAction)) {
                    mergedAction = applyOverride(mergedAction, override);
                    matchedOverride = true;
                }
            }
            mergedActions.add(mergedAction);
        }

        if (!effect.appendedActions().isEmpty()
                && (effect.actionParameterOverrides().isEmpty() || matchedOverride)) {
            mergedActions.addAll(effect.appendedActions());
        }

        return new SkillRule(rule.targets(), List.copyOf(mergedActions), rule.ifs(), rule.enPreds());
    }

    private static SkillAction applyOverride(SkillAction action, SkillActionOverride override) {
        String componentId = action.componentId();
        String entityId = action.entityId();
        String blockId = action.blockId();
        String soundId = action.soundId();
        String particleId = action.particleId();
        String calculationId = action.calculationId();
        HitKind hitKind = action.hitKind();
        Map<DamageType, SkillValueExpression> baseDamage = new LinkedHashMap<>(action.baseDamage());
        SkillValueExpression baseCriticalStrikeChance = action.baseCriticalStrikeChance();
        SkillValueExpression baseCriticalStrikeMultiplier = action.baseCriticalStrikeMultiplier();
        SkillValueExpression volume = action.volume();
        SkillValueExpression pitch = action.pitch();
        SkillValueExpression lifeTicks = action.lifeTicks();
        boolean gravity = action.gravity();
        String anchor = action.anchor();
        SkillValueExpression offsetX = action.offsetX();
        SkillValueExpression offsetY = action.offsetY();
        SkillValueExpression offsetZ = action.offsetZ();

        for (SkillActionFieldOverride fieldOverride : override.fieldOverrides()) {
            if (fieldOverride.path().type() == SkillActionFieldPathType.CALCULATION_ID) {
                calculationId = fieldOverride.value();
                continue;
            }

            String key = fieldOverride.path().parameterKey();
            if (key.equals("component_id") || key.equals("entity_name")) {
                componentId = fieldOverride.value();
            } else if (key.equals("entity_id") || key.equals("proj_en")) {
                entityId = fieldOverride.value();
            } else if (key.equals("block_id") || key.equals("block")) {
                blockId = fieldOverride.value();
            } else if (key.equals("sound") || key.equals("sound_id")) {
                soundId = fieldOverride.value();
            } else if (key.equals("particle_id")) {
                particleId = fieldOverride.value();
            } else if (key.equals("hit_kind")) {
                hitKind = parseHitKind(fieldOverride.value());
            } else if (key.startsWith("base_damage_")) {
                DamageType type = parseDamageType(key.substring("base_damage_".length()));
                if (type != null) {
                    baseDamage.put(type, SkillValueExpression.constant(parseDouble(fieldOverride.value(), 0.0)));
                }
            } else if (key.equals("base_critical_strike_chance")) {
                baseCriticalStrikeChance = SkillValueExpression.constant(parseDouble(fieldOverride.value(), 0.0));
            } else if (key.equals("base_critical_strike_multiplier")) {
                baseCriticalStrikeMultiplier = SkillValueExpression.constant(parseDouble(fieldOverride.value(), 100.0));
            } else if (key.equals("volume")) {
                volume = SkillValueExpression.constant(parseDouble(fieldOverride.value(), 1.0));
            } else if (key.equals("pitch")) {
                pitch = SkillValueExpression.constant(parseDouble(fieldOverride.value(), 1.0));
            } else if (key.equals("life_ticks")) {
                lifeTicks = SkillValueExpression.constant(parseDouble(fieldOverride.value(), 0.0));
            } else if (key.equals("gravity")) {
                gravity = Boolean.parseBoolean(fieldOverride.value());
            } else if (key.equals("anchor")) {
                anchor = fieldOverride.value();
            } else if (key.equals("offset_x")) {
                offsetX = SkillValueExpression.constant(parseDouble(fieldOverride.value(), 0.0));
            } else if (key.equals("offset_y")) {
                offsetY = SkillValueExpression.constant(parseDouble(fieldOverride.value(), 0.0));
            } else if (key.equals("offset_z")) {
                offsetZ = SkillValueExpression.constant(parseDouble(fieldOverride.value(), 0.0));
            }
        }

        return new SkillAction(
                action.type(),
                componentId,
                entityId,
                blockId,
                soundId,
                particleId,
                calculationId,
                hitKind,
                Map.copyOf(baseDamage),
                baseCriticalStrikeChance,
                baseCriticalStrikeMultiplier,
                volume,
                pitch,
                lifeTicks,
                gravity,
                anchor,
                offsetX,
                offsetY,
                offsetZ,
                action.enPreds()
        );
    }

    private static double parseDouble(String raw, double fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            double parsed = Double.parseDouble(raw);
            return Double.isFinite(parsed) ? parsed : fallback;
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static HitKind parseHitKind(String raw) {
        if (raw == null || raw.isBlank()) {
            return HitKind.ATTACK;
        }
        for (HitKind kind : HitKind.values()) {
            if (kind.serializedName().equals(raw)) {
                return kind;
            }
        }
        return HitKind.ATTACK;
    }

    private static DamageType parseDamageType(String rawType) {
        if (rawType == null || rawType.isBlank()) {
            return null;
        }

        for (DamageType damageType : DamageType.values()) {
            if (damageType.id().equals(rawType)) {
                return damageType;
            }
        }
        return null;
    }

    public record SupportMergeResult(
            SkillDefinition skill,
            List<ConditionalStatModifier> additionalConditionalModifiers,
            List<String> warnings
    ) {
        public SupportMergeResult {
            Objects.requireNonNull(skill, "skill");
            Objects.requireNonNull(additionalConditionalModifiers, "additionalConditionalModifiers");
            Objects.requireNonNull(warnings, "warnings");
            additionalConditionalModifiers = List.copyOf(additionalConditionalModifiers);
            warnings = List.copyOf(warnings);
        }
    }

    private record ApplyEffectResult(
            SkillAttached attached,
            List<String> warnings
    ) {
        private ApplyEffectResult {
            Objects.requireNonNull(attached, "attached");
            Objects.requireNonNull(warnings, "warnings");
            warnings = List.copyOf(warnings);
        }
    }
}
