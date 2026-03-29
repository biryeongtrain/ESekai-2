package kim.biryeong.esekai2.api.skill.value;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kim.biryeong.esekai2.api.skill.execution.SkillUseContext;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Datapack-backed reusable numeric expression surface.
 *
 * @param value reusable expression payload
 */
public record SkillValueDefinition(
        SkillValueExpression value
) {
    private static final Codec<SkillValueDefinition> BASE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SkillValueExpression.CODEC.fieldOf("value").forGetter(SkillValueDefinition::value)
    ).apply(instance, SkillValueDefinition::new));

    /**
     * Validated codec for `esekai2:skill_value`.
     */
    public static final Codec<SkillValueDefinition> CODEC = BASE_CODEC.validate(SkillValueDefinition::validate);

    public SkillValueDefinition {
        Objects.requireNonNull(value, "value");
    }

    /**
     * Resolves the value definition for the provided runtime context.
     *
     * @param context runtime skill use context
     * @return resolved numeric value
     */
    public double resolve(SkillUseContext context) {
        Objects.requireNonNull(context, "context");
        return resolve(context, new HashSet<>());
    }

    /**
     * Resolves the value definition for the provided runtime context and recursion guard.
     *
     * @param context runtime skill use context
     * @param visited value identifiers already visited during recursive resolution
     * @return resolved numeric value
     */
    public double resolve(SkillUseContext context, Set<net.minecraft.resources.Identifier> visited) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(visited, "visited");
        return value.resolve(context, visited);
    }

    private static DataResult<SkillValueDefinition> validate(SkillValueDefinition definition) {
        return DataResult.success(definition);
    }
}
