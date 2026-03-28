package kim.biryeong.esekai2.impl.item.affix;

import kim.biryeong.esekai2.api.item.affix.RolledAffix;
import kim.biryeong.esekai2.api.stat.modifier.StatModifier;

import java.util.List;
import java.util.Objects;

/**
 * Internal helper that projects rolled affix snapshots into stat modifier lists.
 */
public final class AffixModifierResolver {
    private AffixModifierResolver() {
    }

    public static List<StatModifier> toModifiers(RolledAffix rolledAffix) {
        Objects.requireNonNull(rolledAffix, "rolledAffix");
        return rolledAffix.modifiers();
    }
}
