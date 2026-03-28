package kim.biryeong.esekai2.api.monster.affix;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kim.biryeong.esekai2.api.stat.modifier.StatModifier;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Objects;

/**
 * Snapshot of one monster affix after its ranged modifier definitions have been rolled into concrete stat modifiers.
 *
 * @param affixId stable identifier of the affix definition that produced this rolled snapshot
 * @param kind rolled monster affix kind
 * @param modifiers rolled concrete stat modifiers carried by this affix
 */
public record RolledMonsterAffix(
        Identifier affixId,
        MonsterAffixKind kind,
        List<StatModifier> modifiers
) {
    private static final Codec<RolledMonsterAffix> BASE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("affix_id").forGetter(RolledMonsterAffix::affixId),
            MonsterAffixKind.CODEC.fieldOf("kind").forGetter(RolledMonsterAffix::kind),
            Codec.list(StatModifier.CODEC).fieldOf("modifiers").forGetter(RolledMonsterAffix::modifiers)
    ).apply(instance, RolledMonsterAffix::new));

    /**
     * Validated codec used to decode rolled monster affixes from fixtures and persistence.
     */
    public static final Codec<RolledMonsterAffix> CODEC = BASE_CODEC.validate(RolledMonsterAffix::validate);

    public RolledMonsterAffix {
        Objects.requireNonNull(affixId, "affixId");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(modifiers, "modifiers");

        modifiers = List.copyOf(modifiers);
        for (StatModifier modifier : modifiers) {
            Objects.requireNonNull(modifier, "modifier entry");
        }
    }

    private static DataResult<RolledMonsterAffix> validate(RolledMonsterAffix rolledAffix) {
        for (StatModifier modifier : rolledAffix.modifiers()) {
            if (!rolledAffix.affixId().equals(modifier.sourceId())) {
                return DataResult.error(() -> "every modifier source_id must match affix_id");
            }
        }

        return DataResult.success(rolledAffix);
    }
}
