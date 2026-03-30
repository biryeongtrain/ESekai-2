package kim.biryeong.esekai2.api.skill.definition.graph;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

/**
 * Subject selector used by runtime skill predicates that inspect one entity.
 */
public enum SkillPredicateSubject {
    PRIMARY_TARGET("primary_target"),
    SELF("self"),
    TARGET("target");

    public static final Codec<SkillPredicateSubject> CODEC = Codec.STRING.comapFlatMap(
            SkillPredicateSubject::bySerializedName,
            SkillPredicateSubject::serializedName
    );

    private final String serializedName;

    SkillPredicateSubject(String serializedName) {
        this.serializedName = serializedName;
    }

    /**
     * Returns the stable datapack-facing subject name.
     *
     * @return subject discriminator
     */
    public String serializedName() {
        return serializedName;
    }

    private static DataResult<SkillPredicateSubject> bySerializedName(String serializedName) {
        for (SkillPredicateSubject subject : values()) {
            if (subject.serializedName.equals(serializedName)) {
                return DataResult.success(subject);
            }
        }

        return DataResult.error(() -> "Unknown predicate subject: " + serializedName);
    }
}
