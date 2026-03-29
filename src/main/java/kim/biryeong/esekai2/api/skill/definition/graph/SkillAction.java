package kim.biryeong.esekai2.api.skill.definition.graph;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kim.biryeong.esekai2.api.damage.breakdown.DamageBreakdown;
import kim.biryeong.esekai2.api.damage.breakdown.DamageType;
import kim.biryeong.esekai2.api.damage.critical.HitKind;
import kim.biryeong.esekai2.api.skill.value.SkillValueExpression;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
 * @param hitKind hit kind override used by damage actions
 * @param baseDamage inline typed base damage overrides used by damage actions
 * @param baseCriticalStrikeChance inline critical strike chance override used by damage actions
 * @param baseCriticalStrikeMultiplier inline critical strike multiplier override used by damage actions
 * @param volume sound volume payload
 * @param pitch sound pitch payload
 * @param lifeTicks projectile or summon lifetime payload
 * @param gravity gravity toggle for projectile and summon actions
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

    private static final MapCodec<IdentityPayload> IDENTITY_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            SkillActionType.CODEC.fieldOf("type").forGetter(IdentityPayload::type),
            Codec.STRING.optionalFieldOf("component_id", "").forGetter(IdentityPayload::componentId),
            Codec.STRING.optionalFieldOf("entity_id", "").forGetter(IdentityPayload::entityId),
            Codec.STRING.optionalFieldOf("block_id", "").forGetter(IdentityPayload::blockId),
            Codec.STRING.optionalFieldOf("sound_id", "").forGetter(IdentityPayload::soundId),
            Codec.STRING.optionalFieldOf("particle_id", "").forGetter(IdentityPayload::particleId),
            Codec.STRING.optionalFieldOf("calculation_id", "").forGetter(IdentityPayload::calculationId)
    ).apply(instance, IdentityPayload::new));

    private static final MapCodec<DamagePayload> DAMAGE_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            HitKind.CODEC.optionalFieldOf("hit_kind", HitKind.ATTACK).forGetter(DamagePayload::hitKind),
            BASE_DAMAGE_CODEC.optionalFieldOf("base_damage", Map.of()).forGetter(DamagePayload::baseDamage),
            SkillValueExpression.CODEC.optionalFieldOf("base_critical_strike_chance", SkillValueExpression.constant(0.0))
                    .forGetter(DamagePayload::baseCriticalStrikeChance),
            SkillValueExpression.CODEC.optionalFieldOf("base_critical_strike_multiplier", SkillValueExpression.constant(100.0))
                    .forGetter(DamagePayload::baseCriticalStrikeMultiplier)
    ).apply(instance, DamagePayload::new));

    private static final MapCodec<RuntimePayload> RUNTIME_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            SkillValueExpression.CODEC.optionalFieldOf("volume", SkillValueExpression.constant(1.0)).forGetter(RuntimePayload::volume),
            SkillValueExpression.CODEC.optionalFieldOf("pitch", SkillValueExpression.constant(1.0)).forGetter(RuntimePayload::pitch),
            SkillValueExpression.CODEC.optionalFieldOf("life_ticks", SkillValueExpression.constant(0.0)).forGetter(RuntimePayload::lifeTicks),
            Codec.BOOL.optionalFieldOf("gravity", false).forGetter(RuntimePayload::gravity),
            Codec.STRING.optionalFieldOf("anchor", "self").forGetter(RuntimePayload::anchor),
            SkillValueExpression.CODEC.optionalFieldOf("offset_x", SkillValueExpression.constant(0.0)).forGetter(RuntimePayload::offsetX),
            SkillValueExpression.CODEC.optionalFieldOf("offset_y", SkillValueExpression.constant(0.0)).forGetter(RuntimePayload::offsetY),
            SkillValueExpression.CODEC.optionalFieldOf("offset_z", SkillValueExpression.constant(0.0)).forGetter(RuntimePayload::offsetZ),
            SkillPredicate.CODEC.listOf().optionalFieldOf("en_preds", List.of()).forGetter(RuntimePayload::enPreds)
    ).apply(instance, RuntimePayload::new));

    private static final Codec<SkillAction> BASE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            IDENTITY_CODEC.forGetter(SkillAction::identityPayload),
            DAMAGE_CODEC.forGetter(SkillAction::damagePayload),
            RUNTIME_CODEC.forGetter(SkillAction::runtimePayload),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("map", Map.of())
                    .forGetter(SkillAction::legacyParameters)
    ).apply(instance, SkillAction::fromCodec));

    /**
     * Validated codec used to decode typed action blocks.
     */
    public static final Codec<SkillAction> CODEC = BASE_CODEC.validate(SkillAction::validate);

    private IdentityPayload identityPayload() {
        return new IdentityPayload(type, componentId, entityId, blockId, soundId, particleId, calculationId);
    }

    private DamagePayload damagePayload() {
        return new DamagePayload(hitKind, baseDamage, baseCriticalStrikeChance, baseCriticalStrikeMultiplier);
    }

    private RuntimePayload runtimePayload() {
        return new RuntimePayload(volume, pitch, lifeTicks, gravity, anchor, offsetX, offsetY, offsetZ, enPreds);
    }

    private static SkillAction fromCodec(
            IdentityPayload identity,
            DamagePayload damage,
            RuntimePayload runtime,
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

        boolean gravity = runtime.gravity();
        String legacyGravity = readString(legacyParameters, "gravity");
        if (!legacyGravity.isBlank()) {
            gravity = parseBoolean(legacyGravity);
        }

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
                hitKind,
                baseDamage,
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
                runtime.enPreds()
        );
    }

    public SkillAction {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(componentId, "componentId");
        Objects.requireNonNull(entityId, "entityId");
        Objects.requireNonNull(blockId, "blockId");
        Objects.requireNonNull(soundId, "soundId");
        Objects.requireNonNull(particleId, "particleId");
        Objects.requireNonNull(calculationId, "calculationId");
        Objects.requireNonNull(hitKind, "hitKind");
        Objects.requireNonNull(baseDamage, "baseDamage");
        Objects.requireNonNull(baseCriticalStrikeChance, "baseCriticalStrikeChance");
        Objects.requireNonNull(baseCriticalStrikeMultiplier, "baseCriticalStrikeMultiplier");
        Objects.requireNonNull(volume, "volume");
        Objects.requireNonNull(pitch, "pitch");
        Objects.requireNonNull(lifeTicks, "lifeTicks");
        Objects.requireNonNull(anchor, "anchor");
        Objects.requireNonNull(offsetX, "offsetX");
        Objects.requireNonNull(offsetY, "offsetY");
        Objects.requireNonNull(offsetZ, "offsetZ");
        Objects.requireNonNull(enPreds, "enPreds");
        baseDamage = Map.copyOf(baseDamage);
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
                parseHitKind(readString(parameters, "hit_kind")),
                parseBaseDamage(parameters),
                parseExpression(parameters, "base_critical_strike_chance", 0.0),
                parseExpression(parameters, "base_critical_strike_multiplier", 100.0),
                parseExpression(parameters, "volume", 1.0),
                parseExpression(parameters, "pitch", 1.0),
                parseExpression(parameters, "life_ticks", 0.0),
                parseBoolean(parameters.get("gravity")),
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
                HitKind.ATTACK,
                Map.of(),
                SkillValueExpression.constant(0.0),
                SkillValueExpression.constant(100.0),
                SkillValueExpression.constant(1.0),
                SkillValueExpression.constant(1.0),
                SkillValueExpression.constant(0.0),
                false,
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
                HitKind.ATTACK,
                Map.of(),
                SkillValueExpression.constant(0.0),
                SkillValueExpression.constant(100.0),
                SkillValueExpression.constant(1.0),
                SkillValueExpression.constant(1.0),
                SkillValueExpression.constant(0.0),
                false,
                "self",
                SkillValueExpression.constant(0.0),
                SkillValueExpression.constant(0.0),
                SkillValueExpression.constant(0.0),
                List.of()
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
        parameters.put("hit_kind", hitKind.serializedName());
        parameters.put("gravity", Boolean.toString(gravity));
        parameters.put("anchor", anchor);
        parameters.put("volume", serializeExpression(volume));
        parameters.put("pitch", serializeExpression(pitch));
        parameters.put("life_ticks", serializeExpression(lifeTicks));
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

        if (action.type() == SkillActionType.SOUND && Identifier.tryParse(action.soundId()) == null) {
            return DataResult.error(() -> "sound action requires a valid sound_id");
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

    private record IdentityPayload(
            SkillActionType type,
            String componentId,
            String entityId,
            String blockId,
            String soundId,
            String particleId,
            String calculationId
    ) {
    }

    private record DamagePayload(
            HitKind hitKind,
            Map<DamageType, SkillValueExpression> baseDamage,
            SkillValueExpression baseCriticalStrikeChance,
            SkillValueExpression baseCriticalStrikeMultiplier
    ) {
    }

    private record RuntimePayload(
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
    }
}
