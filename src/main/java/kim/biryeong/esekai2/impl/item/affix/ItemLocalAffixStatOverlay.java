package kim.biryeong.esekai2.impl.item.affix;

import kim.biryeong.esekai2.api.item.affix.AffixDefinition;
import kim.biryeong.esekai2.api.item.affix.AffixScope;
import kim.biryeong.esekai2.api.item.affix.ItemAffixes;
import kim.biryeong.esekai2.api.stat.definition.StatDefinition;
import kim.biryeong.esekai2.api.stat.holder.StatHolder;
import kim.biryeong.esekai2.api.stat.modifier.StatModifier;
import kim.biryeong.esekai2.api.stat.value.StatInstance;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Builds one ephemeral attacker stat overlay from owner-item local affixes.
 */
public final class ItemLocalAffixStatOverlay {
    private ItemLocalAffixStatOverlay() {
    }

    public static StatHolder apply(
            StatHolder base,
            ItemStack ownerStack,
            Registry<AffixDefinition> affixRegistry
    ) {
        Objects.requireNonNull(base, "base");
        Objects.requireNonNull(ownerStack, "ownerStack");
        Objects.requireNonNull(affixRegistry, "affixRegistry");

        List<StatModifier> localModifiers = ItemAffixes.get(ownerStack).modifiersForScope(affixRegistry, AffixScope.LOCAL);
        if (localModifiers.isEmpty()) {
            return base;
        }

        return new LocalAffixOverlayStatHolder(base, localModifiers);
    }

    private static final class LocalAffixOverlayStatHolder implements StatHolder {
        private final StatHolder base;
        private final List<StatModifier> localModifiers;
        private final Map<ResourceKey<StatDefinition>, StatInstance> instances = new LinkedHashMap<>();

        private LocalAffixOverlayStatHolder(StatHolder base, List<StatModifier> localModifiers) {
            this.base = Objects.requireNonNull(base, "base");
            this.localModifiers = List.copyOf(localModifiers);
        }

        @Override
        public StatInstance stat(ResourceKey<StatDefinition> stat) {
            Objects.requireNonNull(stat, "stat");
            return instances.computeIfAbsent(stat, this::createInstance);
        }

        @Override
        public double resolvedValue(ResourceKey<StatDefinition> stat) {
            return stat(stat).resolvedValue();
        }

        @Override
        public void setBaseValue(ResourceKey<StatDefinition> stat, double baseValue) {
            StatInstance current = stat(stat);
            instances.put(stat, current.withBaseValue(baseValue));
        }

        @Override
        public void addModifier(StatModifier modifier) {
            Objects.requireNonNull(modifier, "modifier");
            instances.put(modifier.stat(), stat(modifier.stat()).withModifier(modifier));
        }

        @Override
        public boolean removeModifier(StatModifier modifier) {
            Objects.requireNonNull(modifier, "modifier");
            StatInstance current = stat(modifier.stat());
            List<StatModifier> updated = new ArrayList<>(current.modifiers());
            int matchIndex = updated.indexOf(modifier);
            if (matchIndex < 0) {
                return false;
            }
            updated.remove(matchIndex);
            instances.put(modifier.stat(), new StatInstance(current.stat(), current.definition(), current.baseValue(), updated));
            return true;
        }

        @Override
        public Map<ResourceKey<StatDefinition>, StatInstance> snapshot() {
            Map<ResourceKey<StatDefinition>, StatInstance> snapshot = new LinkedHashMap<>(base.snapshot());
            snapshot.putAll(instances);
            return Map.copyOf(snapshot);
        }

        private StatInstance createInstance(ResourceKey<StatDefinition> stat) {
            StatInstance baseInstance = base.stat(stat);
            List<StatModifier> mergedModifiers = new ArrayList<>(baseInstance.modifiers());
            for (StatModifier modifier : localModifiers) {
                if (modifier.stat().equals(stat)) {
                    mergedModifiers.add(modifier);
                }
            }
            return new StatInstance(stat, baseInstance.definition(), baseInstance.baseValue(), mergedModifiers);
        }
    }
}
