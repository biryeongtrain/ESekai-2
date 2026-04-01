package kim.biryeong.esekai2.impl.player.stat;

import kim.biryeong.esekai2.api.item.affix.AffixDefinition;
import kim.biryeong.esekai2.api.item.affix.AffixScope;
import kim.biryeong.esekai2.api.item.affix.ItemAffixes;
import kim.biryeong.esekai2.api.item.affix.ItemAffixState;
import kim.biryeong.esekai2.api.item.socket.SocketedEquipmentSlot;
import kim.biryeong.esekai2.api.level.LevelProgressionDefinition;
import kim.biryeong.esekai2.api.player.level.PlayerLevelState;
import kim.biryeong.esekai2.api.stat.definition.StatDefinition;
import kim.biryeong.esekai2.api.stat.holder.StatHolder;
import kim.biryeong.esekai2.api.stat.holder.StatHolders;
import kim.biryeong.esekai2.api.stat.modifier.StatModifier;
import kim.biryeong.esekai2.api.stat.value.StatInstance;
import kim.biryeong.esekai2.impl.item.affix.AffixRegistryAccess;
import kim.biryeong.esekai2.impl.player.level.PlayerLevelService;
import kim.biryeong.esekai2.impl.stat.registry.StatRegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Stable per-player stat holder facade that lazily rebuilds source-backed combat stats.
 */
final class PlayerCombatStatHolder implements StatHolder {
    private final MinecraftServer server;
    private final UUID playerId;
    private final Map<ResourceKey<StatDefinition>, Double> baseOverrides = new LinkedHashMap<>();
    private final List<StatModifier> modifierOverrides = new ArrayList<>();
    private StatHolder delegate;
    private boolean dirty = true;
    private int cachedLevel = Integer.MIN_VALUE;
    private EquipmentFingerprint cachedEquipment = EquipmentFingerprint.EMPTY;

    PlayerCombatStatHolder(MinecraftServer server, UUID playerId) {
        this.server = Objects.requireNonNull(server, "server");
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.delegate = StatHolders.create(StatRegistryAccess.statRegistry(server));
    }

    void markDirty() {
        dirty = true;
    }

    @Override
    public StatInstance stat(ResourceKey<StatDefinition> stat) {
        Objects.requireNonNull(stat, "stat");
        ensureFresh();
        return delegate.stat(stat);
    }

    @Override
    public double resolvedValue(ResourceKey<StatDefinition> stat) {
        Objects.requireNonNull(stat, "stat");
        ensureFresh();
        return delegate.resolvedValue(stat);
    }

    @Override
    public void setBaseValue(ResourceKey<StatDefinition> stat, double baseValue) {
        Objects.requireNonNull(stat, "stat");
        baseOverrides.put(stat, baseValue);
        delegate.setBaseValue(stat, baseValue);
    }

    @Override
    public void addModifier(StatModifier modifier) {
        Objects.requireNonNull(modifier, "modifier");
        modifierOverrides.add(modifier);
        delegate.addModifier(modifier);
    }

    @Override
    public boolean removeModifier(StatModifier modifier) {
        Objects.requireNonNull(modifier, "modifier");
        int matchIndex = modifierOverrides.indexOf(modifier);
        if (matchIndex < 0) {
            return false;
        }
        modifierOverrides.remove(matchIndex);
        return delegate.removeModifier(modifier);
    }

    @Override
    public Map<ResourceKey<StatDefinition>, StatInstance> snapshot() {
        ensureFresh();
        return delegate.snapshot();
    }

    private void ensureFresh() {
        PlayerLevelState levelState = PlayerLevelService.get(server, playerId);
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        EquipmentFingerprint equipmentFingerprint = EquipmentFingerprint.capture(player);
        if (!dirty
                && cachedLevel == levelState.level()
                && cachedEquipment.equals(equipmentFingerprint)) {
            return;
        }

        StatHolder rebuilt = StatHolders.create(StatRegistryAccess.statRegistry(server));
        applyProgressionModifiers(rebuilt, levelState.level());
        if (player != null) {
            applyEquipmentModifiers(rebuilt, player);
        }
        reapplyOverrides(rebuilt);

        delegate = rebuilt;
        cachedLevel = levelState.level();
        cachedEquipment = equipmentFingerprint;
        dirty = false;
    }

    private void applyProgressionModifiers(StatHolder rebuilt, int currentLevel) {
        LevelProgressionDefinition progression = PlayerLevelService.progression(server);
        for (int level = 1; level <= currentLevel; level++) {
            for (StatModifier modifier : progression.grantedModifiers(level)) {
                rebuilt.addModifier(modifier);
            }
        }
    }

    private void applyEquipmentModifiers(StatHolder rebuilt, ServerPlayer player) {
        Registry<AffixDefinition> affixRegistry = AffixRegistryAccess.affixRegistry(server);
        for (SocketedEquipmentSlot slot : SocketedEquipmentSlot.values()) {
            ItemStack stack = player.getItemBySlot(slot.equipmentSlot());
            ItemAffixState state = ItemAffixes.get(stack);
            for (StatModifier modifier : state.modifiersForScope(affixRegistry, AffixScope.GLOBAL)) {
                rebuilt.addModifier(modifier);
            }
        }
    }

    private void reapplyOverrides(StatHolder rebuilt) {
        for (Map.Entry<ResourceKey<StatDefinition>, Double> entry : baseOverrides.entrySet()) {
            rebuilt.setBaseValue(entry.getKey(), entry.getValue());
        }
        for (StatModifier modifier : modifierOverrides) {
            rebuilt.addModifier(modifier);
        }
    }

    private record EquipmentFingerprint(List<SlotEntry> slots) {
        private static final EquipmentFingerprint EMPTY = new EquipmentFingerprint(List.of());

        private EquipmentFingerprint {
            Objects.requireNonNull(slots, "slots");
            slots = List.copyOf(slots);
        }

        static EquipmentFingerprint capture(ServerPlayer player) {
            if (player == null) {
                return EMPTY;
            }

            List<SlotEntry> slots = new ArrayList<>(SocketedEquipmentSlot.values().length);
            for (SocketedEquipmentSlot slot : SocketedEquipmentSlot.values()) {
                ItemStack stack = player.getItemBySlot(slot.equipmentSlot());
                Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
                slots.add(new SlotEntry(slot, itemId, ItemAffixes.get(stack)));
            }
            return new EquipmentFingerprint(slots);
        }
    }

    private record SlotEntry(
            SocketedEquipmentSlot slot,
            Identifier itemId,
            ItemAffixState affixState
    ) {
        private SlotEntry {
            Objects.requireNonNull(slot, "slot");
            Objects.requireNonNull(itemId, "itemId");
            Objects.requireNonNull(affixState, "affixState");
        }
    }
}
