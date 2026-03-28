package kim.biryeong.esekai2.api.item.affix;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kim.biryeong.esekai2.api.item.type.ItemFamily;
import kim.biryeong.esekai2.api.stat.modifier.StatModifier;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Objects;

/**
 * Snapshot of one affix after its ranged modifier definitions have been rolled into concrete stat modifiers.
 *
 * @param affixId stable identifier of the affix definition that produced this rolled snapshot
 * @param itemFamily item family the rolled affix was generated for
 * @param modifiers rolled concrete stat modifiers carried by this affix
 */
public record RolledAffix(
        Identifier affixId,
        ItemFamily itemFamily,
        List<StatModifier> modifiers
) {
    private static final Codec<RolledAffix> BASE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("affix_id").forGetter(RolledAffix::affixId),
            ItemFamily.CODEC.fieldOf("item_family").forGetter(RolledAffix::itemFamily),
            Codec.list(StatModifier.CODEC).fieldOf("modifiers").forGetter(RolledAffix::modifiers)
    ).apply(instance, RolledAffix::new));

    /**
     * Validated codec used to decode rolled affixes from fixtures and future persistence layers.
     */
    public static final Codec<RolledAffix> CODEC = BASE_CODEC.validate(RolledAffix::validate);

    public RolledAffix {
        Objects.requireNonNull(affixId, "affixId");
        Objects.requireNonNull(itemFamily, "itemFamily");
        Objects.requireNonNull(modifiers, "modifiers");

        modifiers = List.copyOf(modifiers);
        for (StatModifier modifier : modifiers) {
            Objects.requireNonNull(modifier, "modifier entry");
        }
    }

    private static DataResult<RolledAffix> validate(RolledAffix rolledAffix) {
        if (rolledAffix.modifiers().isEmpty()) {
            return DataResult.error(() -> "modifiers must not be empty");
        }

        for (StatModifier modifier : rolledAffix.modifiers()) {
            if (!rolledAffix.affixId().equals(modifier.sourceId())) {
                return DataResult.error(() -> "every modifier source_id must match affix_id");
            }
        }

        return DataResult.success(rolledAffix);
    }
}
