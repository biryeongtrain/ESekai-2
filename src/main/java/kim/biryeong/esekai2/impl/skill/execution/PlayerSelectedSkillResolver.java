package kim.biryeong.esekai2.impl.skill.execution;

import kim.biryeong.esekai2.api.item.socket.SocketedSkillLoadResult;
import kim.biryeong.esekai2.api.item.socket.SocketedSkills;
import kim.biryeong.esekai2.api.player.skill.SelectedActiveSkillRef;
import kim.biryeong.esekai2.api.skill.definition.SkillDefinition;
import kim.biryeong.esekai2.api.skill.definition.SkillRegistries;
import kim.biryeong.esekai2.api.skill.support.SkillSupportDefinition;
import net.minecraft.core.Registry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Resolves one player's selected active skill from equipped socketed items.
 */
public final class PlayerSelectedSkillResolver {
    private PlayerSelectedSkillResolver() {
    }

    public static ResolutionResult resolve(ServerPlayer player, SelectedActiveSkillRef selection) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(selection, "selection");

        MinecraftServer server = Objects.requireNonNull(player.level().getServer(), "player is not attached to a running server");
        Registry<SkillDefinition> skillRegistry = server.registryAccess().lookupOrThrow(SkillRegistries.SKILL);
        Registry<SkillSupportDefinition> supportRegistry = server.registryAccess().lookupOrThrow(SkillRegistries.SKILL_SUPPORT);

        ItemStack stack = SocketedSkills.getEquippedStack(player, selection.equipmentSlot());
        if (stack.isEmpty()) {
            return ResolutionResult.failure("No item is equipped in selected slot " + selection.equipmentSlot().serializedName());
        }

        if (SocketedSkills.getActiveSkill(stack).isEmpty()) {
            return ResolutionResult.failure("Selected item in slot " + selection.equipmentSlot().serializedName() + " does not declare an active skill");
        }

        if (!SocketedSkills.getActiveSkill(stack).orElseThrow().equals(selection.skillId())) {
            return ResolutionResult.failure("Selected slot " + selection.equipmentSlot().serializedName() + " now points to "
                    + SocketedSkills.getActiveSkill(stack).orElseThrow() + " instead of " + selection.skillId());
        }

        SocketedSkillLoadResult loadResult = SocketedSkills.resolveDefinitions(stack, skillRegistry, supportRegistry);
        if (loadResult.activeSkill().isEmpty()) {
            List<String> warnings = new ArrayList<>(loadResult.warnings());
            if (warnings.isEmpty()) {
                warnings.add("Selected active skill " + selection.skillId() + " could not be resolved");
            }
            return new ResolutionResult(Optional.empty(), List.copyOf(warnings));
        }

        return new ResolutionResult(
                Optional.of(new ResolvedSelectedSkill(selection, stack, loadResult.activeSkill().orElseThrow(), loadResult.linkedSupports())),
                loadResult.warnings()
        );
    }

    public record ResolvedSelectedSkill(
            SelectedActiveSkillRef selection,
            ItemStack stack,
            SkillDefinition activeSkill,
            List<SkillSupportDefinition> linkedSupports
    ) {
        public ResolvedSelectedSkill {
            Objects.requireNonNull(selection, "selection");
            Objects.requireNonNull(stack, "stack");
            Objects.requireNonNull(activeSkill, "activeSkill");
            Objects.requireNonNull(linkedSupports, "linkedSupports");
            linkedSupports = List.copyOf(linkedSupports);
        }
    }

    public record ResolutionResult(
            Optional<ResolvedSelectedSkill> resolvedSkill,
            List<String> warnings
    ) {
        public ResolutionResult {
            Objects.requireNonNull(resolvedSkill, "resolvedSkill");
            Objects.requireNonNull(warnings, "warnings");
            warnings = List.copyOf(warnings);
        }

        public static ResolutionResult failure(String warning) {
            Objects.requireNonNull(warning, "warning");
            return new ResolutionResult(Optional.empty(), List.of(warning));
        }

        public boolean success() {
            return resolvedSkill.isPresent();
        }
    }
}
