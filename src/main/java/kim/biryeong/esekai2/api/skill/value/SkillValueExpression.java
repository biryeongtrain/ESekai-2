package kim.biryeong.esekai2.api.skill.value;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kim.biryeong.esekai2.api.player.resource.PlayerResourceIds;
import kim.biryeong.esekai2.api.skill.definition.graph.SkillPredicateSubject;
import kim.biryeong.esekai2.api.skill.execution.SkillExecutionContext;
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
 * stat reads from the attacking entity, and resource-backed reads that target the existing skill
 * subject axis. Higher-order expression composition can be layered on top of this surface later
 * without falling back to string maps.</p>
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
     * Returns whether this expression reads the current amount of a named resource.
     *
     * @return {@code true} when the expression is current-resource-backed
     */
    default boolean isCurrentResource() {
        return type() == SkillValueExpressionType.RESOURCE_CURRENT;
    }

    /**
     * Returns whether this expression reads the maximum amount of a named resource.
     *
     * @return {@code true} when the expression is max-resource-backed
     */
    default boolean isMaxResource() {
        return type() == SkillValueExpressionType.RESOURCE_MAX;
    }

    /**
     * Returns whether this expression reads the prepared resource cost.
     *
     * @return {@code true} when the expression is prepared-resource-cost-backed
     */
    default boolean isResourceCost() {
        return type() == SkillValueExpressionType.RESOURCE_COST;
    }

    /**
     * Returns whether this expression reads the prepared use time in ticks.
     *
     * @return {@code true} when the expression is prepared-use-time-backed
     */
    default boolean isUseTimeTicks() {
        return type() == SkillValueExpressionType.USE_TIME_TICKS;
    }

    /**
     * Returns whether this expression reads the prepared cooldown in ticks.
     *
     * @return {@code true} when the expression is prepared-cooldown-backed
     */
    default boolean isCooldownTicks() {
        return type() == SkillValueExpressionType.COOLDOWN_TICKS;
    }

    /**
     * Returns whether this expression reads the prepared maximum charges.
     *
     * @return {@code true} when the expression is prepared-max-charges-backed
     */
    default boolean isMaxCharges() {
        return type() == SkillValueExpressionType.MAX_CHARGES;
    }

    /**
     * Returns whether this expression reads the prepared total burst casts.
     *
     * @return {@code true} when the expression is prepared-times-to-cast-backed
     */
    default boolean isTimesToCast() {
        return type() == SkillValueExpressionType.TIMES_TO_CAST;
    }

    /**
     * Returns whether this expression reads the current skill cooldown state.
     *
     * @return {@code true} when the expression is cooldown-backed
     */
    default boolean isCooldownRemaining() {
        return type() == SkillValueExpressionType.COOLDOWN_REMAINING;
    }

    /**
     * Returns whether this expression reads the current available charges for the skill.
     *
     * @return {@code true} when the expression is charge-backed
     */
    default boolean isChargesAvailable() {
        return type() == SkillValueExpressionType.CHARGES_AVAILABLE;
    }

    /**
     * Returns whether this expression reads the current remaining burst follow-up casts.
     *
     * @return {@code true} when the expression is burst-backed
     */
    default boolean isBurstRemaining() {
        return type() == SkillValueExpressionType.BURST_REMAINING;
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
     * Returns the named resource identifier for resource-backed expressions.
     *
     * @return referenced resource identifier
     * @throws IllegalStateException when the expression is not resource-backed
     */
    default String resourceId() {
        if (this instanceof SkillCurrentResourceValueExpression resource) {
            return resource.resource();
        }
        if (this instanceof SkillMaxResourceValueExpression resource) {
            return resource.resource();
        }
        throw new IllegalStateException("resourceId() is only valid for resource-backed value expressions");
    }

    /**
     * Returns the subject selector used by resource-backed expressions.
     *
     * @return logical subject inspected by the resource read
     * @throws IllegalStateException when the expression is not resource-backed
     */
    default SkillPredicateSubject resourceSubject() {
        if (this instanceof SkillCurrentResourceValueExpression resource) {
            return resource.subject();
        }
        if (this instanceof SkillMaxResourceValueExpression resource) {
            return resource.subject();
        }
        throw new IllegalStateException("resourceSubject() is only valid for resource-backed value expressions");
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
     * Resolves this expression against the provided runtime execution context.
     *
     * <p>Most value expressions continue to resolve from the prepared {@link SkillUseContext}.
     * Runtime-state-backed expressions may override this to inspect live player state.</p>
     *
     * @param context runtime execution context
     * @return resolved numeric value
     */
    default double resolve(SkillExecutionContext context) {
        Objects.requireNonNull(context, "context");
        return resolve(context.preparedUse().useContext());
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

    /**
     * Creates a current-resource expression.
     *
     * @param resourceId named resource identifier to inspect
     * @param subject logical subject whose current amount should be read
     * @return current-resource expression
     */
    static SkillValueExpression currentResource(String resourceId, SkillPredicateSubject subject) {
        return new SkillCurrentResourceValueExpression(resourceId, subject);
    }

    /**
     * Creates a max-resource expression.
     *
     * @param resourceId named resource identifier to inspect
     * @param subject logical subject whose maximum amount should be read
     * @return max-resource expression
     */
    static SkillValueExpression maxResource(String resourceId, SkillPredicateSubject subject) {
        return new SkillMaxResourceValueExpression(resourceId, subject);
    }

    /**
     * Creates a prepared resource-cost expression for the current prepared skill.
     *
     * @return prepared resource-cost expression
     */
    static SkillValueExpression resourceCost() {
        return new SkillResourceCostValueExpression();
    }

    /**
     * Creates a prepared use-time expression for the current prepared skill.
     *
     * @return prepared use-time expression
     */
    static SkillValueExpression useTimeTicks() {
        return new SkillUseTimeTicksValueExpression();
    }

    /**
     * Creates a prepared cooldown expression for the current prepared skill.
     *
     * @return prepared cooldown expression
     */
    static SkillValueExpression cooldownTicks() {
        return new SkillCooldownTicksValueExpression();
    }

    /**
     * Creates a prepared max-charges expression for the current prepared skill.
     *
     * @return prepared max-charges expression
     */
    static SkillValueExpression maxCharges() {
        return new SkillMaxChargesValueExpression();
    }

    /**
     * Creates a prepared times-to-cast expression for the current prepared skill.
     *
     * @return prepared times-to-cast expression
     */
    static SkillValueExpression timesToCast() {
        return new SkillTimesToCastValueExpression();
    }

    /**
     * Creates a cooldown-remaining expression for the current prepared skill.
     *
     * @return cooldown-remaining expression
     */
    static SkillValueExpression cooldownRemaining() {
        return new SkillCooldownRemainingValueExpression();
    }

    /**
     * Creates an available-charges expression for the current prepared skill.
     *
     * @return available-charges expression
     */
    static SkillValueExpression chargesAvailable() {
        return new SkillChargesAvailableValueExpression();
    }

    /**
     * Creates a burst-remaining expression for the current prepared skill.
     *
     * @return burst-remaining expression
     */
    static SkillValueExpression burstRemaining() {
        return new SkillBurstRemainingValueExpression();
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
            case RESOURCE_CURRENT -> new SkillCurrentResourceValueExpression(
                    serialized.resource().orElseThrow(),
                    serialized.subject().orElseThrow()
            );
            case RESOURCE_MAX -> new SkillMaxResourceValueExpression(
                    serialized.resource().orElseThrow(),
                    serialized.subject().orElseThrow()
            );
            case RESOURCE_COST -> new SkillResourceCostValueExpression();
            case USE_TIME_TICKS -> new SkillUseTimeTicksValueExpression();
            case COOLDOWN_TICKS -> new SkillCooldownTicksValueExpression();
            case MAX_CHARGES -> new SkillMaxChargesValueExpression();
            case TIMES_TO_CAST -> new SkillTimesToCastValueExpression();
            case COOLDOWN_REMAINING -> new SkillCooldownRemainingValueExpression();
            case CHARGES_AVAILABLE -> new SkillChargesAvailableValueExpression();
            case BURST_REMAINING -> new SkillBurstRemainingValueExpression();
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
            case RESOURCE_CURRENT -> validateResourceExpression((SkillCurrentResourceValueExpression) expression);
            case RESOURCE_MAX -> validateResourceExpression((SkillMaxResourceValueExpression) expression);
            case RESOURCE_COST,
                    USE_TIME_TICKS,
                    COOLDOWN_TICKS,
                    MAX_CHARGES,
                    TIMES_TO_CAST,
                    COOLDOWN_REMAINING,
                    CHARGES_AVAILABLE,
                    BURST_REMAINING -> DataResult.success(expression);
        };
    }

    private static DataResult<SkillValueExpression> validateResourceExpression(SkillValueExpression expression) {
        String resourceId = expression.resourceId();
        if (!PlayerResourceIds.isUsable(resourceId)) {
            return DataResult.error(() -> "resource-backed skill value expressions require non-blank resource");
        }
        if (expression.resourceSubject() == null) {
            return DataResult.error(() -> "resource-backed skill value expressions require subject");
        }
        return DataResult.success(expression);
    }

    /**
     * Serialized codec payload used by the public codec.
     */
    record Serialized(
            SkillValueExpressionType type,
            double constant,
            Optional<Identifier> referenceId,
            Optional<Identifier> stat,
            Optional<String> resource,
            Optional<SkillPredicateSubject> subject
    ) {
        private static final Codec<Serialized> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                SkillValueExpressionType.CODEC.fieldOf("type").forGetter(Serialized::type),
                Codec.DOUBLE.optionalFieldOf("constant", 0.0).forGetter(Serialized::constant),
                Identifier.CODEC.optionalFieldOf("reference_id").forGetter(Serialized::referenceId),
                Identifier.CODEC.optionalFieldOf("stat").forGetter(Serialized::stat),
                Codec.STRING.optionalFieldOf("resource").forGetter(Serialized::resource),
                SkillPredicateSubject.CODEC.optionalFieldOf("subject").forGetter(Serialized::subject)
        ).apply(instance, Serialized::new));

        private static Serialized from(SkillValueExpression expression) {
            return switch (expression.type()) {
                case CONSTANT -> new Serialized(
                        SkillValueExpressionType.CONSTANT,
                        ((SkillLiteralValueExpression) expression).value(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty()
                );
                case REFERENCE -> new Serialized(SkillValueExpressionType.REFERENCE, 0.0,
                        Optional.of(((SkillValueReferenceExpression) expression).valueId()), Optional.empty(), Optional.empty(), Optional.empty());
                case STAT -> new Serialized(SkillValueExpressionType.STAT, 0.0, Optional.empty(),
                        Optional.of(((SkillStatValueExpression) expression).stat()), Optional.empty(), Optional.empty());
                case RESOURCE_CURRENT -> new Serialized(
                        SkillValueExpressionType.RESOURCE_CURRENT,
                        0.0,
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of(((SkillCurrentResourceValueExpression) expression).resource()),
                        Optional.of(((SkillCurrentResourceValueExpression) expression).subject())
                );
                case RESOURCE_MAX -> new Serialized(
                        SkillValueExpressionType.RESOURCE_MAX,
                        0.0,
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of(((SkillMaxResourceValueExpression) expression).resource()),
                        Optional.of(((SkillMaxResourceValueExpression) expression).subject())
                );
                case RESOURCE_COST -> new Serialized(
                        SkillValueExpressionType.RESOURCE_COST,
                        0.0,
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty()
                );
                case USE_TIME_TICKS -> new Serialized(
                        SkillValueExpressionType.USE_TIME_TICKS,
                        0.0,
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty()
                );
                case COOLDOWN_TICKS -> new Serialized(
                        SkillValueExpressionType.COOLDOWN_TICKS,
                        0.0,
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty()
                );
                case MAX_CHARGES -> new Serialized(
                        SkillValueExpressionType.MAX_CHARGES,
                        0.0,
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty()
                );
                case TIMES_TO_CAST -> new Serialized(
                        SkillValueExpressionType.TIMES_TO_CAST,
                        0.0,
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty()
                );
                case COOLDOWN_REMAINING -> new Serialized(
                        SkillValueExpressionType.COOLDOWN_REMAINING,
                        0.0,
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty()
                );
                case CHARGES_AVAILABLE -> new Serialized(
                        SkillValueExpressionType.CHARGES_AVAILABLE,
                        0.0,
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty()
                );
                case BURST_REMAINING -> new Serialized(
                        SkillValueExpressionType.BURST_REMAINING,
                        0.0,
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty()
                );
            };
        }
    }
}
