package kim.biryeong.esekai2.api.skill.tag;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Shared helpers and codec for immutable skill tag sets.
 */
public final class SkillTags {
    /**
     * Codec used by datapacks and fixtures that carry a unique set of skill tags.
     */
    public static final Codec<Set<SkillTag>> CODEC = Codec.list(SkillTag.CODEC).comapFlatMap(SkillTags::decode, SkillTags::encode);

    private SkillTags() {
    }

    /**
     * Returns an immutable copy of the provided skill tag set.
     *
     * @param tags tags to normalize into an immutable enum-backed copy
     * @return immutable copy of the provided tags
     */
    public static Set<SkillTag> copyOf(Set<SkillTag> tags) {
        Objects.requireNonNull(tags, "tags");
        if (tags.isEmpty()) {
            return Set.of();
        }

        EnumSet<SkillTag> copy = EnumSet.noneOf(SkillTag.class);
        for (SkillTag tag : tags) {
            copy.add(Objects.requireNonNull(tag, "tag"));
        }

        return Set.copyOf(copy);
    }

    /**
     * Returns whether the provided tag set contains every required tag.
     *
     * @param tags skill tags to inspect
     * @param required required tags that must all be present
     * @return {@code true} when every required tag is present
     */
    public static boolean containsAll(Set<SkillTag> tags, Set<SkillTag> required) {
        Objects.requireNonNull(tags, "tags");
        Objects.requireNonNull(required, "required");
        return tags.containsAll(required);
    }

    /**
     * Returns whether the two provided skill tag sets share at least one tag.
     *
     * @param left first tag set
     * @param right second tag set
     * @return {@code true} when the two sets intersect
     */
    public static boolean intersects(Set<SkillTag> left, Set<SkillTag> right) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");

        for (SkillTag tag : right) {
            if (left.contains(tag)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns whether the two provided skill tag sets share at least one tag.
     *
     * @param left first tag set
     * @param right second tag set
     * @return {@code true} when the two sets intersect
     */
    public static boolean containsAny(Set<SkillTag> left, Set<SkillTag> right) {
        return intersects(left, right);
    }

    private static DataResult<Set<SkillTag>> decode(List<SkillTag> tags) {
        EnumSet<SkillTag> deduplicated = EnumSet.noneOf(SkillTag.class);
        for (SkillTag tag : tags) {
            deduplicated.add(Objects.requireNonNull(tag, "tag"));
        }

        if (deduplicated.size() != tags.size()) {
            return DataResult.error(() -> "skill tag lists must not contain duplicates");
        }

        return DataResult.success(Set.copyOf(deduplicated));
    }

    private static List<SkillTag> encode(Set<SkillTag> tags) {
        return copyOf(tags).stream()
                .sorted(Comparator.comparingInt(Enum::ordinal))
                .toList();
    }
}
