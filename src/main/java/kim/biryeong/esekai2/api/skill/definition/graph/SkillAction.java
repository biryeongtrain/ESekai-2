package kim.biryeong.esekai2.api.skill.definition.graph;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapDecoder;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kim.biryeong.esekai2.api.ailment.AilmentType;
import kim.biryeong.esekai2.api.damage.breakdown.DamageBreakdown;
import kim.biryeong.esekai2.api.damage.breakdown.DamageType;
import kim.biryeong.esekai2.api.damage.critical.HitKind;
import kim.biryeong.esekai2.api.skill.effect.SkillEffectPurgeMode;
import kim.biryeong.esekai2.api.skill.effect.SkillAilmentRefreshPolicy;
import kim.biryeong.esekai2.api.skill.effect.MobEffectRefreshPolicy;
import kim.biryeong.esekai2.api.skill.value.SkillValueExpression;
import net.minecraft.resources.Identifier;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Typed action node carried by a skill rule.
 *
 * @param type action discriminant
 * @param componentId component id used by projectile and summon actions
 * @param entityId entity id used by projectile and summon actions
 * @param blockId block id used by summon-block actions
 * @param soundId sound id used by sound actions
 * @param particleId particle id used by sandstorm particle actions
 * @param calculationId datapack-backed damage calculation reference
 * @param resource resource id used by resource_delta actions
 * @param effectId mob effect id used by MobEffect actions
 * @param effectIds mob effect id list used by multi-cleanse remove_effect actions
 * @param purge broad purge selector used by remove_effect actions
 * @param dotId stable identifier used by damage-over-time actions
 * @param ailmentId stable ailment identifier used by ailment application actions
 * @param hitKind hit kind override used by damage actions
 * @param baseDamage inline typed base damage overrides used by damage actions
 * @param baseCriticalStrikeChance inline critical strike chance override used by damage actions
 * @param baseCriticalStrikeMultiplier inline critical strike multiplier override used by damage actions
 * @param amount scalar payload used by heal and resource_delta actions
 * @param volume sound volume payload
 * @param pitch sound pitch payload
 * @param lifeTicks projectile or summon lifetime payload
 * @param durationTicks buff or dot duration payload
 * @param amplifier buff amplifier payload
 * @param chance ailment chance percentage payload
 * @param potencyMultiplier ailment potency multiplier percentage payload
 * @param tickIntervalTicks damage-over-time tick interval payload
 * @param gravity gravity toggle for projectile and summon actions
 * @param ambient mob effect ambient toggle
 * @param showParticles mob effect particle visibility toggle
 * @param showIcon mob effect icon visibility toggle
 * @param refreshPolicy mob effect refresh or stacking policy
 * @param ailmentRefreshPolicy ailment refresh or replacement policy
 * @param anchor particle anchor string
 * @param offsetX particle offset x payload
 * @param offsetY particle offset y payload
 * @param offsetZ particle offset z payload
 * @param enPreds action-local predicates evaluated before hook execution
 */
public record SkillAction(
        SkillActionType type,
        String componentId,
        String entityId,
        String blockId,
        String soundId,
        String particleId,
        String calculationId,
        String resource,
        String effectId,
        List<String> effectIds,
        Optional<SkillEffectPurgeMode> purge,
        String dotId,
        String ailmentId,
        HitKind hitKind,
        Map<DamageType, SkillValueExpression> baseDamage,
        SkillValueExpression baseCriticalStrikeChance,
        SkillValueExpression baseCriticalStrikeMultiplier,
        SkillValueExpression amount,
        SkillValueExpression volume,
        SkillValueExpression pitch,
        SkillValueExpression lifeTicks,
        SkillValueExpression durationTicks,
        SkillValueExpression amplifier,
        SkillValueExpression chance,
        SkillValueExpression potencyMultiplier,
        SkillValueExpression tickIntervalTicks,
        boolean gravity,
        boolean ambient,
        boolean showParticles,
        boolean showIcon,
        MobEffectRefreshPolicy refreshPolicy,
        SkillAilmentRefreshPolicy ailmentRefreshPolicy,
        String anchor,
        SkillValueExpression offsetX,
        SkillValueExpression offsetY,
        SkillValueExpression offsetZ,
        List<SkillPredicate> enPreds
) {
    private static final Codec<Map<DamageType, SkillValueExpression>> BASE_DAMAGE_CODEC = Codec.unboundedMap(
            DamageType.CODEC,
            SkillValueExpression.CODEC
    ).xmap(
            map -> {
                Map<DamageType, SkillValueExpression> copy = new EnumMap<>(DamageType.class);
                copy.putAll(map);
                return Map.copyOf(copy);
            },
            map -> map
    );

    private static final Codec<String> REFRESH_POLICY_NAME_CODEC = Codec.STRING.comapFlatMap(
            SkillAction::validateRefreshPolicyName,
            value -> value
    );

    private static final MapCodec<IdentityPayload> IDENTITY_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            SkillActionType.CODEC.fieldOf("type").forGetter(IdentityPayload::type),
            Codec.STRING.optionalFieldOf("component_id", "").forGetter(IdentityPayload::componentId),
            Codec.STRING.optionalFieldOf("entity_id", "").forGetter(IdentityPayload::entityId),
            Codec.STRING.optionalFieldOf("block_id", "").forGetter(IdentityPayload::blockId),
            Codec.STRING.optionalFieldOf("sound_id", "").forGetter(IdentityPayload::soundId),
            Codec.STRING.optionalFieldOf("particle_id", "").forGetter(IdentityPayload::particleId),
            Codec.STRING.optionalFieldOf("calculation_id", "").forGetter(IdentityPayload::calculationId),
            Codec.STRING.optionalFieldOf("resource", "").forGetter(IdentityPayload::resource),
            Codec.STRING.optionalFieldOf("effect_id", "").forGetter(IdentityPayload::effectId),
            Codec.STRING.listOf().optionalFieldOf("effect_ids", List.of()).forGetter(IdentityPayload::effectIds),
            SkillEffectPurgeMode.CODEC.optionalFieldOf("purge").forGetter(IdentityPayload::purge),
            Codec.STRING.optionalFieldOf("dot_id", "").forGetter(IdentityPayload::dotId),
            Codec.STRING.optionalFieldOf("ailment_id", "").forGetter(IdentityPayload::ailmentId),
            REFRESH_POLICY_NAME_CODEC.optionalFieldOf("refresh_policy", "").forGetter(IdentityPayload::refreshPolicy)
    ).apply(instance, IdentityPayload::new));

    private static final MapCodec<DamagePayload> DAMAGE_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            HitKind.CODEC.optionalFieldOf("hit_kind", HitKind.ATTACK).forGetter(DamagePayload::hitKind),
            BASE_DAMAGE_CODEC.optionalFieldOf("base_damage", Map.of()).forGetter(DamagePayload::baseDamage),
            SkillValueExpression.CODEC.optionalFieldOf("base_critical_strike_chance", SkillValueExpression.constant(0.0))
                    .forGetter(DamagePayload::baseCriticalStrikeChance),
            SkillValueExpression.CODEC.optionalFieldOf("base_critical_strike_multiplier", SkillValueExpression.constant(100.0))
                    .forGetter(DamagePayload::baseCriticalStrikeMultiplier),
            SkillValueExpression.CODEC.optionalFieldOf("amount", SkillValueExpression.constant(0.0))
                    .forGetter(DamagePayload::amount)
    ).apply(instance, DamagePayload::new));

    private static final MapCodec<RuntimePayload> RUNTIME_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            SkillValueExpression.CODEC.optionalFieldOf("volume", SkillValueExpression.constant(1.0)).forGetter(RuntimePayload::volume),
            SkillValueExpression.CODEC.optionalFieldOf("pitch", SkillValueExpression.constant(1.0)).forGetter(RuntimePayload::pitch),
            SkillValueExpression.CODEC.optionalFieldOf("life_ticks", SkillValueExpression.constant(0.0)).forGetter(RuntimePayload::lifeTicks),
            SkillValueExpression.CODEC.optionalFieldOf("duration_ticks", SkillValueExpression.constant(0.0)).forGetter(RuntimePayload::durationTicks),
            SkillValueExpression.CODEC.optionalFieldOf("amplifier", SkillValueExpression.constant(0.0)).forGetter(RuntimePayload::amplifier),
            SkillValueExpression.CODEC.optionalFieldOf("chance", SkillValueExpression.constant(100.0)).forGetter(RuntimePayload::chance),
            SkillValueExpression.CODEC.optionalFieldOf("potency_multiplier", SkillValueExpression.constant(100.0)).forGetter(RuntimePayload::potencyMultiplier),
            SkillValueExpression.CODEC.optionalFieldOf("tick_interval", SkillValueExpression.constant(1.0)).forGetter(RuntimePayload::tickIntervalTicks),
            Codec.BOOL.optionalFieldOf("gravity", false).forGetter(RuntimePayload::gravity),
            Codec.BOOL.optionalFieldOf("ambient", false).forGetter(RuntimePayload::ambient),
            Codec.BOOL.optionalFieldOf("show_particles", true).forGetter(RuntimePayload::showParticles),
            Codec.BOOL.optionalFieldOf("show_icon", true).forGetter(RuntimePayload::showIcon),
            Codec.STRING.optionalFieldOf("anchor", "self").forGetter(RuntimePayload::anchor),
            SkillValueExpression.CODEC.optionalFieldOf("offset_x", SkillValueExpression.constant(0.0)).forGetter(RuntimePayload::offsetX),
            SkillValueExpression.CODEC.optionalFieldOf("offset_y", SkillValueExpression.constant(0.0)).forGetter(RuntimePayload::offsetY),
            SkillValueExpression.CODEC.optionalFieldOf("offset_z", SkillValueExpression.constant(0.0)).forGetter(RuntimePayload::offsetZ)
    ).apply(instance, RuntimePayload::new));

    private static final Codec<SkillAction> BASE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            IDENTITY_CODEC.forGetter(SkillAction::identityPayload),
            DAMAGE_CODEC.forGetter(SkillAction::damagePayload),
            RUNTIME_CODEC.forGetter(SkillAction::runtimePayload),
            SkillPredicate.CODEC.listOf().optionalFieldOf("en_preds", List.of()).forGetter(SkillAction::enPreds),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("map", Map.of())
                    .forGetter(SkillAction::legacyParameters)
    ).apply(instance, SkillAction::fromCodec));

    private static final MapDecoder<SkillAction> LEGACY_MAP_DECODER = new MapDecoder.Implementation<>() {
        @Override
        public <T> DataResult<SkillAction> decode(DynamicOps<T> ops, MapLike<T> input) {
            DataResult<SkillActionType> typeResult = readRequiredField(ops, input, "type", SkillActionType.CODEC);
            return typeResult.map(type -> decodeLegacyAction(ops, input, type));
        }

        @Override
        public <T> Stream<T> keys(DynamicOps<T> ops) {
            return Stream.empty();
        }
    };

    private static final Codec<SkillAction> LEGACY_CODEC = Codec.of(
            Encoder.error("Legacy SkillAction codec is decode-only"),
            LEGACY_MAP_DECODER.decoder(),
            "SkillActionLegacyCodec"
    );

    /**
     * Validated codec used to decode typed action blocks.
     */
    public static final Codec<SkillAction> CODEC = Codec.withAlternative(BASE_CODEC, LEGACY_CODEC).validate(SkillAction::validate);

    private IdentityPayload identityPayload() {
        return new IdentityPayload(
                type,
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
                serializedRefreshPolicy(type, refreshPolicy, ailmentRefreshPolicy)
        );
    }

    private DamagePayload damagePayload() {
        return new DamagePayload(hitKind, baseDamage, baseCriticalStrikeChance, baseCriticalStrikeMultiplier, amount);
    }

    private RuntimePayload runtimePayload() {
        return new RuntimePayload(
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
                anchor,
                offsetX,
                offsetY,
                offsetZ
        );
    }

    private static SkillAction fromCodec(
            IdentityPayload identity,
            DamagePayload damage,
            RuntimePayload runtime,
            List<SkillPredicate> enPreds,
            Map<String, String> legacyParameters
    ) {
        String componentId = identity.componentId();
        if (componentId.isBlank()) {
            componentId = readString(legacyParameters, "component_id", "entity_name");
        }

        String entityId = identity.entityId();
        if (entityId.isBlank()) {
            entityId = readString(legacyParameters, "entity_id", "proj_en");
        }

        String blockId = identity.blockId();
        if (blockId.isBlank()) {
            blockId = readString(legacyParameters, "block_id", "block");
        }

        String soundId = identity.soundId();
        if (soundId.isBlank()) {
            soundId = readString(legacyParameters, "sound_id", "sound");
        }

        String particleId = identity.particleId();
        if (particleId.isBlank()) {
            particleId = readString(legacyParameters, "particle_id");
        }

        String calculationId = identity.calculationId();
        if (calculationId.isBlank()) {
            calculationId = readString(legacyParameters, "calculation_id");
        }

        String resource = identity.resource();
        if (resource.isBlank()) {
            resource = readString(legacyParameters, "resource");
        }

        String effectId = identity.effectId();
        if (effectId.isBlank()) {
            effectId = readString(legacyParameters, "effect_id");
        }
        List<String> effectIds = identity.effectIds();
        if (effectIds.isEmpty()) {
            effectIds = parseEffectIds(legacyParameters.get("effect_ids"));
        }
        Optional<SkillEffectPurgeMode> purge = identity.purge();
        if (purge.isEmpty()) {
            purge = parsePurgeMode(legacyParameters.get("purge"));
        }

        String dotId = identity.dotId();
        if (dotId.isBlank()) {
            dotId = readString(legacyParameters, "dot_id");
        }

        String ailmentId = identity.ailmentId();
        if (ailmentId.isBlank()) {
            ailmentId = readString(legacyParameters, "ailment_id");
        }

        HitKind hitKind = damage.hitKind();
        String legacyHitKind = readString(legacyParameters, "hit_kind");
        if (!legacyHitKind.isBlank()) {
            hitKind = parseHitKind(legacyHitKind);
        }

        Map<DamageType, SkillValueExpression> baseDamage = damage.baseDamage();
        Map<DamageType, SkillValueExpression> legacyBaseDamage = parseBaseDamage(legacyParameters);
        if (!legacyBaseDamage.isEmpty()) {
            Map<DamageType, SkillValueExpression> merged = new EnumMap<>(DamageType.class);
            merged.putAll(baseDamage);
            merged.putAll(legacyBaseDamage);
            baseDamage = Map.copyOf(merged);
        }

        SkillValueExpression baseCriticalStrikeChance = damage.baseCriticalStrikeChance();
        if (isDefaultExpression(baseCriticalStrikeChance, 0.0)) {
            baseCriticalStrikeChance = parseExpression(legacyParameters, "base_critical_strike_chance", 0.0);
        }

        SkillValueExpression baseCriticalStrikeMultiplier = damage.baseCriticalStrikeMultiplier();
        if (isDefaultExpression(baseCriticalStrikeMultiplier, 100.0)) {
            baseCriticalStrikeMultiplier = parseExpression(legacyParameters, "base_critical_strike_multiplier", 100.0);
        }

        SkillValueExpression amount = damage.amount();
        if (isDefaultExpression(amount, 0.0)) {
            amount = parseExpression(legacyParameters, "amount", 0.0);
        }

        SkillValueExpression volume = runtime.volume();
        if (isDefaultExpression(volume, 1.0)) {
            volume = parseExpression(legacyParameters, "volume", 1.0);
        }

        SkillValueExpression pitch = runtime.pitch();
        if (isDefaultExpression(pitch, 1.0)) {
            pitch = parseExpression(legacyParameters, "pitch", 1.0);
        }

        SkillValueExpression lifeTicks = runtime.lifeTicks();
        if (isDefaultExpression(lifeTicks, 0.0)) {
            lifeTicks = parseExpression(legacyParameters, "life_ticks", 0.0);
        }

        SkillValueExpression durationTicks = runtime.durationTicks();
        if (isDefaultExpression(durationTicks, 0.0)) {
            durationTicks = parseExpression(legacyParameters, "duration_ticks", 0.0);
        }

        SkillValueExpression amplifier = runtime.amplifier();
        if (isDefaultExpression(amplifier, 0.0)) {
            amplifier = parseExpression(legacyParameters, "amplifier", 0.0);
        }

        SkillValueExpression chance = runtime.chance();
        if (isDefaultExpression(chance, 100.0)) {
            chance = parseExpression(legacyParameters, "chance", 100.0);
        }

        SkillValueExpression potencyMultiplier = runtime.potencyMultiplier();
        if (isDefaultExpression(potencyMultiplier, 100.0)) {
            potencyMultiplier = parseExpression(legacyParameters, "potency_multiplier", 100.0);
        }

        SkillValueExpression tickIntervalTicks = runtime.tickIntervalTicks();
        if (isDefaultExpression(tickIntervalTicks, 1.0)) {
            tickIntervalTicks = parseExpression(legacyParameters, "tick_interval", 1.0);
        }

        boolean gravity = runtime.gravity();
        String legacyGravity = readString(legacyParameters, "gravity");
        if (!legacyGravity.isBlank()) {
            gravity = parseBoolean(legacyGravity);
        }

        boolean ambient = runtime.ambient();
        String legacyAmbient = readString(legacyParameters, "ambient");
        if (!legacyAmbient.isBlank()) {
            ambient = parseBoolean(legacyAmbient);
        }

        boolean showParticles = runtime.showParticles();
        String legacyShowParticles = readString(legacyParameters, "show_particles");
        if (!legacyShowParticles.isBlank()) {
            showParticles = parseBoolean(legacyShowParticles);
        }

        boolean showIcon = runtime.showIcon();
        String legacyShowIcon = readString(legacyParameters, "show_icon");
        if (!legacyShowIcon.isBlank()) {
            showIcon = parseBoolean(legacyShowIcon);
        }

        String rawRefreshPolicy = identity.refreshPolicy();
        String legacyRefreshPolicy = readString(legacyParameters, "refresh_policy");
        if (!legacyRefreshPolicy.isBlank()) {
            rawRefreshPolicy = legacyRefreshPolicy;
        }
        AilmentType ailmentType = parseAilmentType(ailmentId);
        MobEffectRefreshPolicy refreshPolicy = parseMobEffectRefreshPolicy(rawRefreshPolicy);
        SkillAilmentRefreshPolicy ailmentRefreshPolicy = parseAilmentRefreshPolicy(rawRefreshPolicy, ailmentType);

        String anchor = runtime.anchor();
        if (anchor.isBlank() || "self".equals(anchor)) {
            String legacyAnchor = readString(legacyParameters, "anchor", "self");
            if (!legacyAnchor.isBlank()) {
                anchor = legacyAnchor;
            }
        }

        SkillValueExpression offsetX = runtime.offsetX();
        if (isDefaultExpression(offsetX, 0.0)) {
            offsetX = parseExpression(legacyParameters, "offset_x", 0.0);
        }

        SkillValueExpression offsetY = runtime.offsetY();
        if (isDefaultExpression(offsetY, 0.0)) {
            offsetY = parseExpression(legacyParameters, "offset_y", 0.0);
        }

        SkillValueExpression offsetZ = runtime.offsetZ();
        if (isDefaultExpression(offsetZ, 0.0)) {
            offsetZ = parseExpression(legacyParameters, "offset_z", 0.0);
        }

        return new SkillAction(
                identity.type(),
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
                baseDamage,
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
                enPreds
        );
    }

    private static <T, A> DataResult<A> readRequiredField(DynamicOps<T> ops, MapLike<T> input, String key, Codec<A> codec) {
        T value = input.get(key);
        if (value == null) {
            return DataResult.error(() -> "No key " + key + " in " + input);
        }
        return codec.parse(ops, value);
    }

    private static <T, A> Optional<A> readOptionalField(DynamicOps<T> ops, MapLike<T> input, String key, Codec<A> codec) {
        T value = input.get(key);
        if (value == null) {
            return Optional.empty();
        }
        return codec.parse(ops, value).result();
    }

    private static <T> SkillAction decodeLegacyAction(DynamicOps<T> ops, MapLike<T> input, SkillActionType type) {
        String componentId = readOptionalField(ops, input, "component_id", Codec.STRING)
                .orElseGet(() -> readOptionalField(ops, input, "entity_name", Codec.STRING).orElse(""));
        String entityId = readOptionalField(ops, input, "entity_id", Codec.STRING)
                .orElseGet(() -> readOptionalField(ops, input, "proj_en", Codec.STRING).orElse(""));
        String blockId = readOptionalField(ops, input, "block_id", Codec.STRING)
                .orElseGet(() -> readOptionalField(ops, input, "block", Codec.STRING).orElse(""));
        String soundId = readOptionalField(ops, input, "sound_id", Codec.STRING)
                .orElseGet(() -> readOptionalField(ops, input, "sound", Codec.STRING).orElse(""));
        String particleId = readOptionalField(ops, input, "particle_id", Codec.STRING).orElse("");
        String calculationId = readOptionalField(ops, input, "calculation_id", Codec.STRING).orElse("");
        String resource = readOptionalField(ops, input, "resource", Codec.STRING).orElse("");
        String effectId = readOptionalField(ops, input, "effect_id", Codec.STRING).orElse("");
        List<String> effectIds = readOptionalField(ops, input, "effect_ids", Codec.STRING.listOf()).orElse(List.of());
        Optional<SkillEffectPurgeMode> purge = readOptionalField(ops, input, "purge", SkillEffectPurgeMode.CODEC);
        String dotId = readOptionalField(ops, input, "dot_id", Codec.STRING).orElse("");
        String ailmentId = readOptionalField(ops, input, "ailment_id", Codec.STRING).orElse("");
        HitKind hitKind = readOptionalField(ops, input, "hit_kind", HitKind.CODEC).orElse(HitKind.ATTACK);
        Map<DamageType, SkillValueExpression> baseDamage = readLegacyBaseDamage(ops, input);
        SkillValueExpression baseCriticalStrikeChance = readOptionalField(ops, input, "base_critical_strike_chance", SkillValueExpression.CODEC)
                .orElse(SkillValueExpression.constant(0.0));
        SkillValueExpression baseCriticalStrikeMultiplier = readOptionalField(ops, input, "base_critical_strike_multiplier", SkillValueExpression.CODEC)
                .orElse(SkillValueExpression.constant(100.0));
        SkillValueExpression amount = readOptionalField(ops, input, "amount", SkillValueExpression.CODEC)
                .orElse(SkillValueExpression.constant(0.0));
        SkillValueExpression volume = readOptionalField(ops, input, "volume", SkillValueExpression.CODEC)
                .orElse(SkillValueExpression.constant(1.0));
        SkillValueExpression pitch = readOptionalField(ops, input, "pitch", SkillValueExpression.CODEC)
                .orElse(SkillValueExpression.constant(1.0));
        SkillValueExpression lifeTicks = readOptionalField(ops, input, "life_ticks", SkillValueExpression.CODEC)
                .orElse(SkillValueExpression.constant(0.0));
        SkillValueExpression durationTicks = readOptionalField(ops, input, "duration_ticks", SkillValueExpression.CODEC)
                .orElse(SkillValueExpression.constant(0.0));
        SkillValueExpression amplifier = readOptionalField(ops, input, "amplifier", SkillValueExpression.CODEC)
                .orElse(SkillValueExpression.constant(0.0));
        SkillValueExpression chance = readOptionalField(ops, input, "chance", SkillValueExpression.CODEC)
                .orElse(SkillValueExpression.constant(100.0));
        SkillValueExpression potencyMultiplier = readOptionalField(ops, input, "potency_multiplier", SkillValueExpression.CODEC)
                .orElse(SkillValueExpression.constant(100.0));
        SkillValueExpression tickIntervalTicks = readOptionalField(ops, input, "tick_interval", SkillValueExpression.CODEC)
                .orElse(SkillValueExpression.constant(1.0));
        boolean gravity = readOptionalField(ops, input, "gravity", Codec.BOOL).orElse(false);
        boolean ambient = readOptionalField(ops, input, "ambient", Codec.BOOL).orElse(false);
        boolean showParticles = readOptionalField(ops, input, "show_particles", Codec.BOOL).orElse(true);
        boolean showIcon = readOptionalField(ops, input, "show_icon", Codec.BOOL).orElse(true);
        String rawRefreshPolicy = readOptionalField(ops, input, "refresh_policy", REFRESH_POLICY_NAME_CODEC)
                .orElse("");
        AilmentType ailmentType = parseAilmentType(ailmentId);
        MobEffectRefreshPolicy refreshPolicy = parseMobEffectRefreshPolicy(rawRefreshPolicy);
        SkillAilmentRefreshPolicy ailmentRefreshPolicy = parseAilmentRefreshPolicy(rawRefreshPolicy, ailmentType);
        String anchor = readOptionalField(ops, input, "anchor", Codec.STRING).orElse("self");
        SkillValueExpression offsetX = readOptionalField(ops, input, "offset_x", SkillValueExpression.CODEC)
                .orElse(SkillValueExpression.constant(0.0));
        SkillValueExpression offsetY = readOptionalField(ops, input, "offset_y", SkillValueExpression.CODEC)
                .orElse(SkillValueExpression.constant(0.0));
        SkillValueExpression offsetZ = readOptionalField(ops, input, "offset_z", SkillValueExpression.CODEC)
                .orElse(SkillValueExpression.constant(0.0));
        List<SkillPredicate> enPreds = readOptionalField(ops, input, "en_preds", SkillPredicate.CODEC.listOf())
                .orElse(List.of());

        return new SkillAction(
                type,
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
                baseDamage,
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
                enPreds
        );
    }

    private static <T> Map<DamageType, SkillValueExpression> readLegacyBaseDamage(DynamicOps<T> ops, MapLike<T> input) {
        Map<DamageType, SkillValueExpression> parsed = new EnumMap<>(DamageType.class);
        input.entries().forEach(entry -> {
            String key = ops.getStringValue(entry.getFirst()).result().orElse("");
            if (!key.startsWith("base_damage_")) {
                return;
            }
            DamageType damageType = parseDamageType(key.substring("base_damage_".length()));
            if (damageType == null) {
                return;
            }
            SkillValueExpression expression = SkillValueExpression.CODEC.parse(ops, entry.getSecond())
                    .result()
                    .orElse(SkillValueExpression.constant(0.0));
            parsed.put(damageType, expression);
        });
        return Map.copyOf(parsed);
    }

    public SkillAction {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(componentId, "componentId");
        Objects.requireNonNull(entityId, "entityId");
        Objects.requireNonNull(blockId, "blockId");
        Objects.requireNonNull(soundId, "soundId");
        Objects.requireNonNull(particleId, "particleId");
        Objects.requireNonNull(calculationId, "calculationId");
        Objects.requireNonNull(resource, "resource");
        Objects.requireNonNull(effectId, "effectId");
        Objects.requireNonNull(effectIds, "effectIds");
        Objects.requireNonNull(purge, "purge");
        Objects.requireNonNull(dotId, "dotId");
        Objects.requireNonNull(ailmentId, "ailmentId");
        Objects.requireNonNull(hitKind, "hitKind");
        Objects.requireNonNull(baseDamage, "baseDamage");
        Objects.requireNonNull(baseCriticalStrikeChance, "baseCriticalStrikeChance");
        Objects.requireNonNull(baseCriticalStrikeMultiplier, "baseCriticalStrikeMultiplier");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(volume, "volume");
        Objects.requireNonNull(pitch, "pitch");
        Objects.requireNonNull(lifeTicks, "lifeTicks");
        Objects.requireNonNull(durationTicks, "durationTicks");
        Objects.requireNonNull(amplifier, "amplifier");
        Objects.requireNonNull(chance, "chance");
        Objects.requireNonNull(potencyMultiplier, "potencyMultiplier");
        Objects.requireNonNull(tickIntervalTicks, "tickIntervalTicks");
        Objects.requireNonNull(refreshPolicy, "refreshPolicy");
        Objects.requireNonNull(ailmentRefreshPolicy, "ailmentRefreshPolicy");
        Objects.requireNonNull(anchor, "anchor");
        Objects.requireNonNull(offsetX, "offsetX");
        Objects.requireNonNull(offsetY, "offsetY");
        Objects.requireNonNull(offsetZ, "offsetZ");
        Objects.requireNonNull(enPreds, "enPreds");
        baseDamage = Map.copyOf(baseDamage);
        effectIds = effectIds.stream()
                .filter(entry -> entry != null && !entry.isBlank())
                .distinct()
                .toList();
        if (!effectIds.isEmpty()) {
            effectId = effectIds.getFirst();
        } else if (!effectId.isBlank()) {
            effectIds = List.of(effectId);
        }
        enPreds = List.copyOf(enPreds);
    }

    /**
     * Compatibility constructor that accepts the legacy flat parameter map shape.
     *
     * @param type action discriminant
     * @param parameters legacy flat parameter map
     */
    public SkillAction(SkillActionType type, Map<String, String> parameters) {
        this(type, parameters, "");
    }

    /**
     * Compatibility constructor that accepts the legacy flat parameter map shape.
     *
     * @param type action discriminant
     * @param parameters legacy flat parameter map
     * @param calculationId optional calculation id override
     */
    public SkillAction(SkillActionType type, Map<String, String> parameters, String calculationId) {
        this(
                type,
                readString(parameters, "component_id", "entity_name"),
                readString(parameters, "entity_id", "proj_en"),
                readString(parameters, "block_id", "block"),
                readString(parameters, "sound_id", "sound"),
                readString(parameters, "particle_id"),
                calculationId == null || calculationId.isBlank() ? readString(parameters, "calculation_id") : calculationId,
                readString(parameters, "resource"),
                readString(parameters, "effect_id"),
                parseEffectIds(parameters.get("effect_ids")),
                parsePurgeMode(parameters.get("purge")),
                readString(parameters, "dot_id"),
                readString(parameters, "ailment_id"),
                parseHitKind(readString(parameters, "hit_kind")),
                parseBaseDamage(parameters),
                parseExpression(parameters, "base_critical_strike_chance", 0.0),
                parseExpression(parameters, "base_critical_strike_multiplier", 100.0),
                parseExpression(parameters, "amount", 0.0),
                parseExpression(parameters, "volume", 1.0),
                parseExpression(parameters, "pitch", 1.0),
                parseExpression(parameters, "life_ticks", 0.0),
                parseExpression(parameters, "duration_ticks", 0.0),
                parseExpression(parameters, "amplifier", 0.0),
                parseExpression(parameters, "chance", 100.0),
                parseExpression(parameters, "potency_multiplier", 100.0),
                parseExpression(parameters, "tick_interval", 1.0),
                parseBoolean(parameters.get("gravity")),
                parseBoolean(parameters.get("ambient")),
                !parameters.containsKey("show_particles") || parseBoolean(parameters.get("show_particles")),
                !parameters.containsKey("show_icon") || parseBoolean(parameters.get("show_icon")),
                parseMobEffectRefreshPolicy(readString(parameters, "refresh_policy", MobEffectRefreshPolicy.OVERWRITE.serializedName())),
                parseAilmentRefreshPolicy(readString(parameters, "refresh_policy"), parseAilmentType(readString(parameters, "ailment_id"))),
                readString(parameters, "anchor", "self"),
                parseExpression(parameters, "offset_x", 0.0),
                parseExpression(parameters, "offset_y", 0.0),
                parseExpression(parameters, "offset_z", 0.0),
                List.of()
        );
    }

    /**
     * Creates a sound action with default pitch and volume.
     *
     * @param soundId sound identifier
     * @return typed sound action
     */
    public static SkillAction sound(Identifier soundId) {
        Objects.requireNonNull(soundId, "soundId");
        return new SkillAction(
                SkillActionType.SOUND,
                "",
                "",
                "",
                soundId.toString(),
                "",
                "",
                "",
                "",
                List.of(),
                Optional.empty(),
                "",
                "",
                HitKind.ATTACK,
                Map.of(),
                SkillValueExpression.constant(0.0),
                SkillValueExpression.constant(100.0),
                SkillValueExpression.constant(0.0),
                SkillValueExpression.constant(1.0),
                SkillValueExpression.constant(1.0),
                SkillValueExpression.constant(0.0),
                SkillValueExpression.constant(0.0),
                SkillValueExpression.constant(0.0),
                SkillValueExpression.constant(100.0),
                SkillValueExpression.constant(100.0),
                SkillValueExpression.constant(1.0),
                false,
                false,
                true,
                true,
                MobEffectRefreshPolicy.OVERWRITE,
                SkillAilmentRefreshPolicy.OVERWRITE,
                "self",
                SkillValueExpression.constant(0.0),
                SkillValueExpression.constant(0.0),
                SkillValueExpression.constant(0.0),
                List.of()
        );
    }

    /**
     * Creates a damage action with an optional calculation reference.
     *
     * @param calculationId reusable calculation id
     * @return typed damage action
     */
    public static SkillAction damage(Identifier calculationId) {
        Objects.requireNonNull(calculationId, "calculationId");
        return new SkillAction(
                SkillActionType.DAMAGE,
                "",
                "",
                "",
                "",
                "",
                calculationId.toString(),
                "",
                "",
                List.of(),
                Optional.empty(),
                "",
                "",
                HitKind.ATTACK,
                Map.of(),
                SkillValueExpression.constant(0.0),
                SkillValueExpression.constant(100.0),
                SkillValueExpression.constant(0.0),
                SkillValueExpression.constant(1.0),
                SkillValueExpression.constant(1.0),
                SkillValueExpression.constant(0.0),
                SkillValueExpression.constant(0.0),
                SkillValueExpression.constant(0.0),
                SkillValueExpression.constant(100.0),
                SkillValueExpression.constant(100.0),
                SkillValueExpression.constant(1.0),
                false,
                false,
                true,
                true,
                MobEffectRefreshPolicy.OVERWRITE,
                SkillAilmentRefreshPolicy.OVERWRITE,
                "self",
                SkillValueExpression.constant(0.0),
                SkillValueExpression.constant(0.0),
                SkillValueExpression.constant(0.0),
                List.of()
        );
    }

    /**
     * Compatibility constructor preserved for existing typed graph call sites that do not use buff/dot fields.
     */
    public SkillAction(
            SkillActionType type,
            String componentId,
            String entityId,
            String blockId,
            String soundId,
            String particleId,
            String calculationId,
            HitKind hitKind,
            Map<DamageType, SkillValueExpression> baseDamage,
            SkillValueExpression baseCriticalStrikeChance,
            SkillValueExpression baseCriticalStrikeMultiplier,
            SkillValueExpression volume,
            SkillValueExpression pitch,
            SkillValueExpression lifeTicks,
            boolean gravity,
            String anchor,
            SkillValueExpression offsetX,
            SkillValueExpression offsetY,
            SkillValueExpression offsetZ,
            List<SkillPredicate> enPreds
    ) {
        this(
                type,
                componentId,
                entityId,
                blockId,
                soundId,
                particleId,
                calculationId,
                "",
                "",
                List.of(),
                Optional.empty(),
                "",
                "",
                hitKind,
                baseDamage,
                baseCriticalStrikeChance,
                baseCriticalStrikeMultiplier,
                SkillValueExpression.constant(0.0),
                volume,
                pitch,
                lifeTicks,
                SkillValueExpression.constant(0.0),
                SkillValueExpression.constant(0.0),
                SkillValueExpression.constant(100.0),
                SkillValueExpression.constant(100.0),
                SkillValueExpression.constant(1.0),
                gravity,
                false,
                true,
                true,
                MobEffectRefreshPolicy.OVERWRITE,
                SkillAilmentRefreshPolicy.OVERWRITE,
                anchor,
                offsetX,
                offsetY,
                offsetZ,
                enPreds
        );
    }

    /**
     * Returns a legacy parameter-map view of this typed action.
     *
     * @return compatibility parameter map for existing runtime code
     */
    public Map<String, String> parameters() {
        Map<String, String> parameters = new LinkedHashMap<>();

        if (!componentId.isBlank()) {
            parameters.put("component_id", componentId);
            parameters.put("entity_name", componentId);
        }
        if (!entityId.isBlank()) {
            parameters.put("entity_id", entityId);
            parameters.put("proj_en", entityId);
        }
        if (!blockId.isBlank()) {
            parameters.put("block", blockId);
            parameters.put("block_id", blockId);
        }
        if (!soundId.isBlank()) {
            parameters.put("sound", soundId);
            parameters.put("sound_id", soundId);
        }
        if (!particleId.isBlank()) {
            parameters.put("particle_id", particleId);
        }
        if (!resource.isBlank()) {
            parameters.put("resource", resource);
        }
        if (!effectId.isBlank()) {
            parameters.put("effect_id", effectId);
        }
        if (!effectIds.isEmpty()) {
            parameters.put("effect_ids", String.join(",", effectIds));
        }
        purge.ifPresent(mode -> parameters.put("purge", mode.serializedName()));
        if (!dotId.isBlank()) {
            parameters.put("dot_id", dotId);
        }
        if (!ailmentId.isBlank()) {
            parameters.put("ailment_id", ailmentId);
        }
        parameters.put("hit_kind", hitKind.serializedName());
        parameters.put("gravity", Boolean.toString(gravity));
        parameters.put("ambient", Boolean.toString(ambient));
        parameters.put("show_particles", Boolean.toString(showParticles));
        parameters.put("show_icon", Boolean.toString(showIcon));
        parameters.put("refresh_policy", serializedRefreshPolicy(type, refreshPolicy, ailmentRefreshPolicy));
        parameters.put("anchor", anchor);
        parameters.put("amount", serializeExpression(amount));
        parameters.put("volume", serializeExpression(volume));
        parameters.put("pitch", serializeExpression(pitch));
        parameters.put("life_ticks", serializeExpression(lifeTicks));
        parameters.put("duration_ticks", serializeExpression(durationTicks));
        parameters.put("amplifier", serializeExpression(amplifier));
        parameters.put("chance", serializeExpression(chance));
        parameters.put("potency_multiplier", serializeExpression(potencyMultiplier));
        parameters.put("tick_interval", serializeExpression(tickIntervalTicks));
        parameters.put("offset_x", serializeExpression(offsetX));
        parameters.put("offset_y", serializeExpression(offsetY));
        parameters.put("offset_z", serializeExpression(offsetZ));

        for (Map.Entry<DamageType, SkillValueExpression> entry : baseDamage.entrySet()) {
            parameters.put("base_damage_" + entry.getKey().id(), serializeExpression(entry.getValue()));
        }
        parameters.put("base_critical_strike_chance", serializeExpression(baseCriticalStrikeChance));
        parameters.put("base_critical_strike_multiplier", serializeExpression(baseCriticalStrikeMultiplier));
        return Map.copyOf(parameters);
    }

    private Map<String, String> legacyParameters() {
        return Map.of();
    }

    private static DataResult<SkillAction> validate(SkillAction action) {
        if (!action.calculationId().isBlank() && Identifier.tryParse(action.calculationId()) == null) {
            return DataResult.error(() -> "calculation_id must be a valid identifier: " + action.calculationId());
        }

        if (!action.effectId().isBlank() && Identifier.tryParse(action.effectId()) == null) {
            return DataResult.error(() -> "effect_id must be a valid identifier: " + action.effectId());
        }
        for (String effectId : action.effectIds()) {
            if (Identifier.tryParse(effectId) == null) {
                return DataResult.error(() -> "effect_ids entries must be valid identifiers: " + effectId);
            }
        }

        if (action.type() == SkillActionType.SOUND && Identifier.tryParse(action.soundId()) == null) {
            return DataResult.error(() -> "sound action requires a valid sound_id");
        }

        if (action.type() == SkillActionType.HEAL && isDefaultExpression(action.amount(), 0.0)) {
            return DataResult.error(() -> "heal action requires amount");
        }

        if (action.type() == SkillActionType.RESOURCE_DELTA) {
            if (!"mana".equals(action.resource())) {
                return DataResult.error(() -> "resource_delta action currently supports only resource: mana");
            }
            if (isDefaultExpression(action.amount(), 0.0)) {
                return DataResult.error(() -> "resource_delta action requires amount");
            }
        }

        if ((action.type() == SkillActionType.APPLY_EFFECT
                || action.type() == SkillActionType.APPLY_BUFF
                || action.type() == SkillActionType.REMOVE_EFFECT)
                && action.type() != SkillActionType.REMOVE_EFFECT
                && Identifier.tryParse(action.effectId()) == null) {
            return DataResult.error(() -> action.type().serializedName() + " action requires a valid effect_id");
        }

        if (action.purge().isPresent() && action.type() != SkillActionType.REMOVE_EFFECT) {
            return DataResult.error(() -> "purge is only supported by remove_effect actions");
        }

        if (action.type() == SkillActionType.REMOVE_EFFECT
                && action.removeEffectIds().isEmpty()
                && action.purge().isEmpty()) {
            return DataResult.error(() -> "remove_effect action requires effect_id, effect_ids, or purge");
        }

        if (action.type() == SkillActionType.APPLY_AILMENT && parseAilmentType(action.ailmentId()) == null) {
            return DataResult.error(() -> "apply_ailment action requires a valid ailment_id");
        }

        if ((action.type() == SkillActionType.APPLY_EFFECT || action.type() == SkillActionType.APPLY_BUFF)
                && action.refreshPolicy() == null) {
            return DataResult.error(() -> action.type().serializedName() + " action requires a valid refresh_policy");
        }

        if (action.type() == SkillActionType.APPLY_AILMENT && action.ailmentRefreshPolicy() == null) {
            return DataResult.error(() -> "apply_ailment action requires a valid refresh_policy");
        }
        if (action.type() == SkillActionType.APPLY_AILMENT && action.refreshPolicy() == MobEffectRefreshPolicy.ADD_DURATION) {
            return DataResult.error(() -> "apply_ailment refresh_policy does not support add_duration");
        }

        if (action.type() == SkillActionType.APPLY_DOT && action.dotId().isBlank()) {
            return DataResult.error(() -> "apply_dot action requires a non-blank dot_id");
        }

        if (action.type() == SkillActionType.SANDSTORM_PARTICLE && Identifier.tryParse(action.particleId()) == null) {
            return DataResult.error(() -> "sandstorm_particle action requires a valid particle_id");
        }

        if ((action.type() == SkillActionType.PROJECTILE || action.type() == SkillActionType.SUMMON_AT_SIGHT)
                && (action.componentId().isBlank() || Identifier.tryParse(action.entityId()) == null)) {
            return DataResult.error(() -> action.type().serializedName() + " action requires component_id and entity_id");
        }

        if (action.type() == SkillActionType.SUMMON_BLOCK) {
            if (action.componentId().isBlank() || Identifier.tryParse(action.blockId()) == null) {
                return DataResult.error(() -> "summon_block action requires component_id and block_id");
            }
        }

        return DataResult.success(action);
    }

    /**
     * Returns the inline typed base damage payload as a breakdown snapshot.
     *
     * @return base damage snapshot using only constant values
     */
    public DamageBreakdown constantBaseDamage() {
        DamageBreakdown breakdown = DamageBreakdown.empty();
        for (Map.Entry<DamageType, SkillValueExpression> entry : baseDamage.entrySet()) {
            if (!entry.getValue().isConstant()) {
                continue;
            }
            breakdown = breakdown.with(entry.getKey(), entry.getValue().constant());
        }
        return breakdown;
    }

    private static String readString(Map<String, String> parameters, String... keys) {
        for (String key : keys) {
            String value = parameters.get(key);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private static String readString(Map<String, String> parameters, String key, String fallback) {
        String value = parameters.get(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }

    private static List<String> parseEffectIds(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(entry -> !entry.isBlank())
                .toList();
    }

    private static Optional<SkillEffectPurgeMode> parsePurgeMode(String raw) {
        return Optional.ofNullable(SkillEffectPurgeMode.fromSerializedName(raw));
    }

    private static boolean parseBoolean(String raw) {
        return raw != null && Boolean.parseBoolean(raw);
    }

    private static SkillValueExpression parseExpression(Map<String, String> parameters, String key, double fallback) {
        String raw = parameters.get(key);
        if (raw == null || raw.isBlank()) {
            return SkillValueExpression.constant(fallback);
        }
        try {
            return SkillValueExpression.constant(Double.parseDouble(raw));
        } catch (NumberFormatException exception) {
            Identifier parsed = Identifier.tryParse(raw);
            if (parsed != null) {
                return SkillValueExpression.reference(parsed);
            }
            return SkillValueExpression.constant(fallback);
        }
    }

    private static Map<DamageType, SkillValueExpression> parseBaseDamage(Map<String, String> parameters) {
        Map<DamageType, SkillValueExpression> parsed = new EnumMap<>(DamageType.class);
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            String key = entry.getKey();
            if (!key.startsWith("base_damage_")) {
                continue;
            }
            DamageType damageType = parseDamageType(key.substring("base_damage_".length()));
            if (damageType == null) {
                continue;
            }
            parsed.put(damageType, parseExpression(parameters, key, 0.0));
        }
        return Map.copyOf(parsed);
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

    private static DataResult<String> validateRefreshPolicyName(String raw) {
        if (raw == null || raw.isBlank()) {
            return DataResult.success("");
        }
        if (MobEffectRefreshPolicy.OVERWRITE.serializedName().equals(raw)
                || MobEffectRefreshPolicy.LONGER_ONLY.serializedName().equals(raw)
                || MobEffectRefreshPolicy.ADD_DURATION.serializedName().equals(raw)
                || SkillAilmentRefreshPolicy.STRONGER_ONLY.serializedName().equals(raw)
                || SkillAilmentRefreshPolicy.LONGER_ONLY.serializedName().equals(raw)) {
            return DataResult.success(raw);
        }
        return DataResult.error(() -> "Unknown refresh_policy: " + raw);
    }

    private static MobEffectRefreshPolicy parseMobEffectRefreshPolicy(String raw) {
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

    private static SkillAilmentRefreshPolicy parseAilmentRefreshPolicy(String raw, AilmentType ailmentType) {
        SkillAilmentRefreshPolicy parsed = SkillAilmentRefreshPolicy.fromSerializedName(raw);
        if (parsed != null) {
            return parsed;
        }
        if (ailmentType != null) {
            return SkillAilmentRefreshPolicy.defaultFor(ailmentType);
        }
        return SkillAilmentRefreshPolicy.OVERWRITE;
    }

    private static String serializedRefreshPolicy(
            SkillActionType type,
            MobEffectRefreshPolicy refreshPolicy,
            SkillAilmentRefreshPolicy ailmentRefreshPolicy
    ) {
        if (type == SkillActionType.APPLY_AILMENT) {
            return ailmentRefreshPolicy.serializedName();
        }
        return refreshPolicy.serializedName();
    }

    private static String serializeExpression(SkillValueExpression expression) {
        if (expression.isConstant()) {
            return Double.toString(expression.constant());
        }
        if (expression.isStat()) {
            return expression.statId().toString();
        }
        return expression.referenceId();
    }

    private static boolean isDefaultExpression(SkillValueExpression expression, double defaultValue) {
        return expression.isConstant() && Double.compare(expression.constant(), defaultValue) == 0;
    }

    /**
     * Returns the resolved remove-effect target list using `effect_ids` first and `effect_id` as a shorthand fallback.
     *
     * @return ordered list of configured remove targets
     */
    public List<String> removeEffectIds() {
        if (!effectIds.isEmpty()) {
            return effectIds;
        }
        if (!effectId.isBlank()) {
            return List.of(effectId);
        }
        return List.of();
    }

    private record IdentityPayload(
            SkillActionType type,
            String componentId,
            String entityId,
            String blockId,
            String soundId,
            String particleId,
            String calculationId,
            String resource,
            String effectId,
            List<String> effectIds,
            Optional<SkillEffectPurgeMode> purge,
            String dotId,
            String ailmentId,
            String refreshPolicy
    ) {
    }

    private record DamagePayload(
            HitKind hitKind,
            Map<DamageType, SkillValueExpression> baseDamage,
            SkillValueExpression baseCriticalStrikeChance,
            SkillValueExpression baseCriticalStrikeMultiplier,
            SkillValueExpression amount
    ) {
    }

    private record RuntimePayload(
            SkillValueExpression volume,
            SkillValueExpression pitch,
            SkillValueExpression lifeTicks,
            SkillValueExpression durationTicks,
            SkillValueExpression amplifier,
            SkillValueExpression chance,
            SkillValueExpression potencyMultiplier,
            SkillValueExpression tickIntervalTicks,
            boolean gravity,
            boolean ambient,
            boolean showParticles,
            boolean showIcon,
            String anchor,
            SkillValueExpression offsetX,
            SkillValueExpression offsetY,
            SkillValueExpression offsetZ
    ) {
    }
}
