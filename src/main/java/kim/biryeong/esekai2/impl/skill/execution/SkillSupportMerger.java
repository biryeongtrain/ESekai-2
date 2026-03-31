package kim.biryeong.esekai2.impl.skill.execution;

import kim.biryeong.esekai2.api.damage.breakdown.DamageType;
import kim.biryeong.esekai2.api.damage.critical.HitKind;
import kim.biryeong.esekai2.api.ailment.AilmentType;
import kim.biryeong.esekai2.api.skill.definition.SkillAttached;
import kim.biryeong.esekai2.api.skill.definition.SkillConfig;
import kim.biryeong.esekai2.api.skill.definition.SkillDefinition;
import kim.biryeong.esekai2.api.skill.definition.graph.SkillAction;
import kim.biryeong.esekai2.api.skill.definition.graph.SkillActionType;
import kim.biryeong.esekai2.api.skill.effect.SkillEffectPurgeMode;
import kim.biryeong.esekai2.api.skill.effect.SkillAilmentRefreshPolicy;
import kim.biryeong.esekai2.api.skill.support.SkillActionFieldOverride;
import kim.biryeong.esekai2.api.skill.support.SkillActionFieldPathType;
import kim.biryeong.esekai2.api.skill.definition.graph.SkillRule;
import kim.biryeong.esekai2.api.skill.effect.MobEffectRefreshPolicy;
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
import java.util.Optional;
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
        String resource = action.resource();
        String effectId = action.effectId();
        List<String> effectIds = action.effectIds();
        Optional<SkillEffectPurgeMode> purge = action.purge();
        String dotId = action.dotId();
        String ailmentId = action.ailmentId();
        HitKind hitKind = action.hitKind();
        Map<DamageType, SkillValueExpression> baseDamage = new LinkedHashMap<>(action.baseDamage());
        SkillValueExpression baseCriticalStrikeChance = action.baseCriticalStrikeChance();
        SkillValueExpression baseCriticalStrikeMultiplier = action.baseCriticalStrikeMultiplier();
        SkillValueExpression amount = action.amount();
        SkillValueExpression volume = action.volume();
        SkillValueExpression pitch = action.pitch();
        SkillValueExpression lifeTicks = action.lifeTicks();
        SkillValueExpression durationTicks = action.durationTicks();
        SkillValueExpression amplifier = action.amplifier();
        SkillValueExpression chance = action.chance();
        SkillValueExpression potencyMultiplier = action.potencyMultiplier();
        SkillValueExpression tickIntervalTicks = action.tickIntervalTicks();
        boolean gravity = action.gravity();
        boolean ambient = action.ambient();
        boolean showParticles = action.showParticles();
        boolean showIcon = action.showIcon();
        MobEffectRefreshPolicy refreshPolicy = action.refreshPolicy();
        SkillAilmentRefreshPolicy ailmentRefreshPolicy = action.ailmentRefreshPolicy();
        String anchor = action.anchor();
        SkillValueExpression offsetX = action.offsetX();
        SkillValueExpression offsetY = action.offsetY();
        SkillValueExpression offsetZ = action.offsetZ();

        for (SkillActionFieldOverride fieldOverride : override.fieldOverrides()) {
            if (fieldOverride.path().type() == SkillActionFieldPathType.CALCULATION_ID) {
                calculationId = fieldOverride.value().first();
                continue;
            }

            String key = fieldOverride.path().parameterKey();
            String stringValue = fieldOverride.value().first();
            if (key.equals("component_id") || key.equals("entity_name")) {
                componentId = stringValue;
            } else if (key.equals("entity_id") || key.equals("proj_en")) {
                entityId = stringValue;
            } else if (key.equals("block_id") || key.equals("block")) {
                blockId = stringValue;
            } else if (key.equals("sound") || key.equals("sound_id")) {
                soundId = stringValue;
            } else if (key.equals("particle_id")) {
                particleId = stringValue;
            } else if (key.equals("resource")) {
                resource = stringValue;
            } else if (key.equals("effect_id")) {
                effectId = stringValue;
                if (action.type() == SkillActionType.REMOVE_EFFECT) {
                    effectIds = stringValue.isBlank() ? List.of() : List.of(stringValue);
                }
            } else if (key.equals("effect_ids")) {
                effectIds = List.copyOf(fieldOverride.value().values());
                if (action.type() == SkillActionType.REMOVE_EFFECT) {
                    effectId = effectIds.isEmpty() ? "" : effectIds.getFirst();
                }
            } else if (key.equals("purge")) {
                Optional<SkillEffectPurgeMode> parsedPurge = parsePurgeMode(stringValue);
                if (parsedPurge.isPresent() || stringValue.isBlank()) {
                    purge = parsedPurge;
                }
            } else if (key.equals("dot_id")) {
                dotId = stringValue;
            } else if (key.equals("ailment_id")) {
                ailmentId = stringValue;
            } else if (key.equals("hit_kind")) {
                hitKind = parseHitKind(stringValue);
            } else if (key.startsWith("base_damage_")) {
                DamageType type = parseDamageType(key.substring("base_damage_".length()));
                if (type != null) {
                    baseDamage.put(type, SkillValueExpression.constant(parseDouble(stringValue, 0.0)));
                }
            } else if (key.equals("base_critical_strike_chance")) {
                baseCriticalStrikeChance = SkillValueExpression.constant(parseDouble(stringValue, 0.0));
            } else if (key.equals("base_critical_strike_multiplier")) {
                baseCriticalStrikeMultiplier = SkillValueExpression.constant(parseDouble(stringValue, 100.0));
            } else if (key.equals("amount")) {
                amount = SkillValueExpression.constant(parseDouble(stringValue, 0.0));
            } else if (key.equals("volume")) {
                volume = SkillValueExpression.constant(parseDouble(stringValue, 1.0));
            } else if (key.equals("pitch")) {
                pitch = SkillValueExpression.constant(parseDouble(stringValue, 1.0));
            } else if (key.equals("life_ticks")) {
                lifeTicks = SkillValueExpression.constant(parseDouble(stringValue, 0.0));
            } else if (key.equals("duration_ticks")) {
                durationTicks = SkillValueExpression.constant(parseDouble(stringValue, 0.0));
            } else if (key.equals("amplifier")) {
                amplifier = SkillValueExpression.constant(parseDouble(stringValue, 0.0));
            } else if (key.equals("chance")) {
                chance = SkillValueExpression.constant(parseDouble(stringValue, 100.0));
            } else if (key.equals("potency_multiplier")) {
                potencyMultiplier = SkillValueExpression.constant(parseDouble(stringValue, 100.0));
            } else if (key.equals("tick_interval")) {
                tickIntervalTicks = SkillValueExpression.constant(parseDouble(stringValue, 1.0));
            } else if (key.equals("gravity")) {
                gravity = Boolean.parseBoolean(stringValue);
            } else if (key.equals("ambient")) {
                ambient = Boolean.parseBoolean(stringValue);
            } else if (key.equals("show_particles")) {
                showParticles = Boolean.parseBoolean(stringValue);
            } else if (key.equals("show_icon")) {
                showIcon = Boolean.parseBoolean(stringValue);
            } else if (key.equals("refresh_policy")) {
                if (action.type() == SkillActionType.APPLY_AILMENT) {
                    SkillAilmentRefreshPolicy parsedAilmentRefreshPolicy = parseAilmentRefreshPolicy(stringValue, ailmentId);
                    if (parsedAilmentRefreshPolicy != null) {
                        ailmentRefreshPolicy = parsedAilmentRefreshPolicy;
                    }
                } else {
                    refreshPolicy = parseRefreshPolicy(stringValue);
                }
            } else if (key.equals("anchor")) {
                anchor = stringValue;
            } else if (key.equals("offset_x")) {
                offsetX = SkillValueExpression.constant(parseDouble(stringValue, 0.0));
            } else if (key.equals("offset_y")) {
                offsetY = SkillValueExpression.constant(parseDouble(stringValue, 0.0));
            } else if (key.equals("offset_z")) {
                offsetZ = SkillValueExpression.constant(parseDouble(stringValue, 0.0));
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
                resource,
                effectId,
                effectIds,
                purge,
                dotId,
                ailmentId,
                hitKind,
                Map.copyOf(baseDamage),
                baseCriticalStrikeChance,
                baseCriticalStrikeMultiplier,
                amount,
                volume,
                pitch,
                lifeTicks,
                durationTicks,
                amplifier,
                chance,
                potencyMultiplier,
                tickIntervalTicks,
                gravity,
                ambient,
                showParticles,
                showIcon,
                refreshPolicy,
                ailmentRefreshPolicy,
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

    private static MobEffectRefreshPolicy parseRefreshPolicy(String raw) {
        if (raw == null || raw.isBlank()) {
            return MobEffectRefreshPolicy.OVERWRITE;
        }

        for (MobEffectRefreshPolicy policy : MobEffectRefreshPolicy.values()) {
            if (policy.serializedName().equals(raw)) {
                return policy;
            }
        }
        return MobEffectRefreshPolicy.OVERWRITE;
    }

    private static SkillAilmentRefreshPolicy parseAilmentRefreshPolicy(String raw, String ailmentId) {
        SkillAilmentRefreshPolicy parsed = SkillAilmentRefreshPolicy.fromSerializedName(raw);
        if (parsed != null) {
            return parsed;
        }
        if (raw != null && !raw.isBlank()) {
            return null;
        }

        if (ailmentId == null || ailmentId.isBlank()) {
            return SkillAilmentRefreshPolicy.STRONGER_ONLY;
        }
        for (AilmentType ailmentType : AilmentType.values()) {
            if (ailmentType.serializedName().equals(ailmentId)) {
                return SkillAilmentRefreshPolicy.defaultFor(ailmentType);
            }
        }
        return SkillAilmentRefreshPolicy.STRONGER_ONLY;
    }

    private static Optional<SkillEffectPurgeMode> parsePurgeMode(String raw) {
        return Optional.ofNullable(SkillEffectPurgeMode.fromSerializedName(raw));
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
