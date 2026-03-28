package kim.biryeong.esekai2.api.skill.definition.graph;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.Objects;

/**
 * Action node carried by a skill rule.
 *
 * <p>Action payload is represented as a flat string map to keep the schema extensible while runtime
 * action handlers are implemented in phase 3+. {@link SkillActionType#SANDSTORM_PARTICLE} requires
 * the {@code particle_id} parameter by contract.</p>
 *
 * @param type action discriminant
 * @param parameters action-specific parameters
 */
public record SkillAction(
        SkillActionType type,
        Map<String, String> parameters
) {
    private static final Codec<SkillAction> BASE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SkillActionType.CODEC.fieldOf("type").forGetter(SkillAction::type),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("map", Map.of())
                    .forGetter(SkillAction::parameters)
    ).apply(instance, SkillAction::new));

    /**
     * Validated codec used to decode action blocks.
     */
    public static final Codec<SkillAction> CODEC = BASE_CODEC.validate(SkillAction::validate);

    public SkillAction {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(parameters, "parameters");
        parameters = Map.copyOf(parameters);
    }

    private static DataResult<SkillAction> validate(SkillAction action) {
        if (action.type() == SkillActionType.SANDSTORM_PARTICLE) {
            String rawParticleId = action.parameters().get("particle_id");
            if (rawParticleId == null || rawParticleId.isBlank()) {
                return DataResult.error(() -> "sandstorm_particle action requires particle_id");
            }
            if (Identifier.tryParse(rawParticleId) == null) {
                return DataResult.error(() -> "particle_id must be a valid identifier: " + rawParticleId);
            }
        }

        if (action.type() == SkillActionType.SOUND && action.parameters().get("sound") == null) {
            return DataResult.error(() -> "sound action requires a sound parameter");
        }

        return DataResult.success(action);
    }
}
