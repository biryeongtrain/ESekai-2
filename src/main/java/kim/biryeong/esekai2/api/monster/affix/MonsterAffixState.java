package kim.biryeong.esekai2.api.monster.affix;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kim.biryeong.esekai2.api.monster.level.MonsterLevelContext;

import java.util.List;
import java.util.Objects;

/**
 * Persistent runtime snapshot attached to live monster entities after affixes are initialized.
 *
 * @param levelContext level and rarity context used when the monster was initialized
 * @param rolledAffixes rolled monster affixes attached to the entity
 */
public record MonsterAffixState(
        MonsterLevelContext levelContext,
        List<RolledMonsterAffix> rolledAffixes
) {
    /**
     * Codec used by persistent entity attachments and test fixtures.
     */
    public static final Codec<MonsterAffixState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            MonsterLevelContext.CODEC.fieldOf("level_context").forGetter(MonsterAffixState::levelContext),
            Codec.list(RolledMonsterAffix.CODEC).optionalFieldOf("rolled_affixes", List.of()).forGetter(MonsterAffixState::rolledAffixes)
    ).apply(instance, MonsterAffixState::new));

    public MonsterAffixState {
        Objects.requireNonNull(levelContext, "levelContext");
        Objects.requireNonNull(rolledAffixes, "rolledAffixes");

        rolledAffixes = List.copyOf(rolledAffixes);
        for (RolledMonsterAffix rolledAffix : rolledAffixes) {
            Objects.requireNonNull(rolledAffix, "rolledAffixes entry");
        }
    }
}
