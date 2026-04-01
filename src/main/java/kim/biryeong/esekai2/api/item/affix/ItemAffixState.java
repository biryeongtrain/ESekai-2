package kim.biryeong.esekai2.api.item.affix;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kim.biryeong.esekai2.api.stat.modifier.StatModifier;
import net.minecraft.core.Registry;

import java.util.List;
import java.util.Objects;

/**
 * Runtime affix state persisted on an item stack.
 *
 * @param rolledAffixes concrete rolled affix snapshots stored on the item
 */
public record ItemAffixState(
        List<RolledAffix> rolledAffixes
) {
    /**
     * Stable empty state used when an item stack has no affix component.
     */
    public static final ItemAffixState EMPTY = new ItemAffixState(List.of());

    /**
     * Codec used by affix item persistence.
     */
    public static final Codec<ItemAffixState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.list(RolledAffix.CODEC).optionalFieldOf("rolled_affixes", List.of()).forGetter(ItemAffixState::rolledAffixes)
    ).apply(instance, ItemAffixState::new));

    public ItemAffixState {
        Objects.requireNonNull(rolledAffixes, "rolledAffixes");
        rolledAffixes = List.copyOf(rolledAffixes);
        for (RolledAffix rolledAffix : rolledAffixes) {
            Objects.requireNonNull(rolledAffix, "rolledAffixes entry");
        }
    }

    /**
     * Returns only affixes whose definition resolves to the requested scope.
     *
     * @param affixRegistry affix registry used to resolve scope metadata
     * @param scope scope to filter for
     * @return immutable rolled affix list in the requested scope
     */
    public List<RolledAffix> affixesForScope(Registry<AffixDefinition> affixRegistry, AffixScope scope) {
        Objects.requireNonNull(affixRegistry, "affixRegistry");
        Objects.requireNonNull(scope, "scope");
        return rolledAffixes.stream()
                .filter(rolledAffix -> affixRegistry.getOptional(rolledAffix.affixId())
                        .map(AffixDefinition::scope)
                        .filter(scope::equals)
                        .isPresent())
                .toList();
    }

    /**
     * Returns the concrete stat modifiers carried by affixes in the requested scope.
     *
     * @param affixRegistry affix registry used to resolve scope metadata
     * @param scope scope to filter for
     * @return immutable concrete modifier list
     */
    public List<StatModifier> modifiersForScope(Registry<AffixDefinition> affixRegistry, AffixScope scope) {
        return affixesForScope(affixRegistry, scope).stream()
                .flatMap(rolledAffix -> rolledAffix.modifiers().stream())
                .toList();
    }
}
