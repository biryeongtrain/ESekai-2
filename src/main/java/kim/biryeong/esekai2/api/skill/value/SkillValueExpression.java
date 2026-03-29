package kim.biryeong.esekai2.api.skill.value;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kim.biryeong.esekai2.api.skill.execution.SkillUseContext;
import net.minecraft.resources.Identifier;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Typed numeric expression used by skill graph payloads.
 *
 * <p>The current MVP supports inline numeric constants, datapack-backed reusable references, and
 * stat reads from the attacking entity. Higher-order expression composition can be layered on top
 * of this surface later without falling back to string maps.</p>
 */
public interface SkillValueExpression {
    /**
     * Validated codec used for graph payloads and the skill value registry.
     */
    Codec<SkillValueExpression> CODEC = Codec.either(Codec.DOUBLE, Serialized.CODEC).xmap(
            SkillValueExpression::decode,
            SkillValueExpression::encode
    ).validate(SkillValueExpression::validate);

    /**
     * Returns the expression discriminant.
     *
     * @return typed value expression discriminator
     */
    SkillValueExpressionType type();

    /**
     * Returns whether this expression is an inline constant.
     *
     * @return {@code true} when the expression is constant
     */
    default boolean isConstant() {
        return type() == SkillValueExpressionType.CONSTANT;
    }

    /**
     * Returns whether this expression is a datapack-backed reference.
     *
     * @return {@code true} when the expression is a reference
     */
    default boolean isReference() {
        return type() == SkillValueExpressionType.REFERENCE;
    }

    /**
     * Returns whether this expression reads a stat value.
     *
     * @return {@code true} when the expression is stat-backed
     */
    default boolean isStat() {
        return type() == SkillValueExpressionType.STAT;
    }

    /**
     * Returns the inline constant value for constant expressions.
     *
     * @return resolved inline constant value
     * @throws IllegalStateException when the expression is not constant
     */
    default double constant() {
        if (this instanceof SkillLiteralValueExpression literal) {
            return literal.value();
        }
        throw new IllegalStateException("constant() is only valid for constant value expressions");
    }

    /**
     * Returns the referenced reusable value identifier for reference expressions.
     *
     * @return referenced value identifier as a string
     * @throws IllegalStateException when the expression is not a reference
     */
    default String referenceId() {
        if (this instanceof SkillValueReferenceExpression reference) {
            return reference.valueId().toString();
        }
        throw new IllegalStateException("referenceId() is only valid for reference value expressions");
    }

    /**
     * Returns the stat identifier for stat-backed expressions.
     *
     * @return referenced stat identifier
     * @throws IllegalStateException when the expression is not stat-backed
     */
    default Identifier statId() {
        if (this instanceof SkillStatValueExpression stat) {
            return stat.stat();
        }
        throw new IllegalStateException("statId() is only valid for stat value expressions");
    }

    /**
     * Resolves this expression against the provided runtime context.
     *
     * @param context runtime skill use context
     * @return resolved numeric value
     */
    default double resolve(SkillUseContext context) {
        return resolve(context, new HashSet<>());
    }

    /**
     * Resolves this expression against the provided runtime context using the supplied recursion guard.
     *
     * @param context runtime skill use context
     * @param visited reusable value identifiers already visited during recursive resolution
     * @return resolved numeric value
     */
    double resolve(SkillUseContext context, Set<Identifier> visited);

    /**
     * Creates an inline constant expression.
     *
     * @param constant inline numeric value
     * @return constant expression
     */
    static SkillValueExpression constant(double constant) {
        return new SkillLiteralValueExpression(constant);
    }

    /**
     * Creates a datapack-backed value reference expression.
     *
     * @param referenceId reusable value identifier
     * @return reference expression
     */
    static SkillValueExpression reference(Identifier referenceId) {
        return new SkillValueReferenceExpression(referenceId);
    }

    /**
     * Creates a stat-backed value expression.
     *
     * @param statId stat identifier to resolve from the attacker stat holder
     * @return stat expression
     */
    static SkillValueExpression stat(Identifier statId) {
        return new SkillStatValueExpression(statId);
    }

    private static SkillValueExpression decode(Either<Double, Serialized> encoded) {
        return encoded.map(SkillValueExpression::constant, SkillValueExpression::decodeSerialized);
    }

    private static Either<Double, Serialized> encode(SkillValueExpression expression) {
        if (expression instanceof SkillLiteralValueExpression literal) {
            return Either.left(literal.value());
        }
        return Either.right(Serialized.from(expression));
    }

    private static SkillValueExpression decodeSerialized(Serialized serialized) {
        return switch (serialized.type()) {
            case CONSTANT -> new SkillLiteralValueExpression(serialized.constant());
            case REFERENCE -> new SkillValueReferenceExpression(serialized.referenceId().orElseThrow());
            case STAT -> new SkillStatValueExpression(serialized.stat().orElseThrow());
        };
    }

    private static DataResult<SkillValueExpression> validate(SkillValueExpression expression) {
        Objects.requireNonNull(expression, "expression");
        return switch (expression.type()) {
            case CONSTANT -> {
                SkillLiteralValueExpression literal = (SkillLiteralValueExpression) expression;
                if (!Double.isFinite(literal.value())) {
                    yield DataResult.error(() -> "constant skill value expressions require a finite constant");
                }
                yield DataResult.success(expression);
            }
            case REFERENCE -> {
                SkillValueReferenceExpression reference = (SkillValueReferenceExpression) expression;
                if (reference.valueId() == null) {
                    yield DataResult.error(() -> "reference skill value expressions require reference_id");
                }
                yield DataResult.success(expression);
            }
            case STAT -> {
                SkillStatValueExpression stat = (SkillStatValueExpression) expression;
                if (stat.stat() == null) {
                    yield DataResult.error(() -> "stat skill value expressions require stat");
                }
                yield DataResult.success(expression);
            }
        };
    }

    /**
     * Serialized codec payload used by the public codec.
     */
    record Serialized(
            SkillValueExpressionType type,
            double constant,
            Optional<Identifier> referenceId,
            Optional<Identifier> stat
    ) {
        private static final Codec<Serialized> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                SkillValueExpressionType.CODEC.fieldOf("type").forGetter(Serialized::type),
                Codec.DOUBLE.optionalFieldOf("constant", 0.0).forGetter(Serialized::constant),
                Identifier.CODEC.optionalFieldOf("reference_id").forGetter(Serialized::referenceId),
                Identifier.CODEC.optionalFieldOf("stat").forGetter(Serialized::stat)
        ).apply(instance, Serialized::new));

        private static Serialized from(SkillValueExpression expression) {
            return switch (expression.type()) {
                case CONSTANT -> new Serialized(SkillValueExpressionType.CONSTANT, ((SkillLiteralValueExpression) expression).value(), Optional.empty(), Optional.empty());
                case REFERENCE -> new Serialized(SkillValueExpressionType.REFERENCE, 0.0,
                        Optional.of(((SkillValueReferenceExpression) expression).valueId()), Optional.empty());
                case STAT -> new Serialized(SkillValueExpressionType.STAT, 0.0, Optional.empty(),
                        Optional.of(((SkillStatValueExpression) expression).stat()));
            };
        }
    }
}
