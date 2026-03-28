package kim.biryeong.esekai2.impl.stat.holder;

import kim.biryeong.esekai2.api.stat.definition.StatDefinition;
import kim.biryeong.esekai2.api.stat.holder.StatHolder;
import kim.biryeong.esekai2.api.stat.modifier.StatModifier;
import kim.biryeong.esekai2.api.stat.value.StatInstance;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class SimpleStatHolder implements StatHolder {
    private final Registry<StatDefinition> registry;
    private final Map<ResourceKey<StatDefinition>, StatInstance> instances = new LinkedHashMap<>();

    public SimpleStatHolder(Registry<StatDefinition> registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
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
        instances.put(stat, stat(stat).withBaseValue(baseValue));
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
        List<StatModifier> currentModifiers = current.modifiers();
        int matchIndex = currentModifiers.indexOf(modifier);

        if (matchIndex < 0) {
            return false;
        }

        List<StatModifier> updatedModifiers = new ArrayList<>(currentModifiers);
        updatedModifiers.remove(matchIndex);
        instances.put(
                modifier.stat(),
                new StatInstance(current.stat(), current.definition(), current.baseValue(), updatedModifiers)
        );
        return true;
    }

    @Override
    public Map<ResourceKey<StatDefinition>, StatInstance> snapshot() {
        return Map.copyOf(instances);
    }

    private StatInstance createInstance(ResourceKey<StatDefinition> stat) {
        StatDefinition definition = registry.getOptional(stat)
                .orElseThrow(() -> new IllegalArgumentException("Unknown stat definition: " + stat));
        return StatInstance.fromDefinition(stat, definition);
    }
}
