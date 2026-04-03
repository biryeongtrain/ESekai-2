package kim.biryeong.esekai2.impl.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import kim.biryeong.esekai2.api.ailment.AilmentPayload;
import kim.biryeong.esekai2.api.ailment.AilmentType;
import kim.biryeong.esekai2.api.ailment.Ailments;
import kim.biryeong.esekai2.api.item.socket.SocketLinkGroup;
import kim.biryeong.esekai2.api.item.socket.SocketSlotType;
import kim.biryeong.esekai2.api.item.socket.SocketedEquipmentSlot;
import kim.biryeong.esekai2.api.item.socket.SocketedSkillItemState;
import kim.biryeong.esekai2.api.item.socket.SocketedSkillRef;
import kim.biryeong.esekai2.api.item.socket.SocketedSkills;
import kim.biryeong.esekai2.api.player.level.PlayerLevelState;
import kim.biryeong.esekai2.api.player.level.PlayerLevels;
import kim.biryeong.esekai2.api.player.resource.PlayerResources;
import kim.biryeong.esekai2.api.player.skill.PlayerActiveSkills;
import kim.biryeong.esekai2.api.player.skill.PlayerSkillBursts;
import kim.biryeong.esekai2.api.player.skill.PlayerSkillChargeState;
import kim.biryeong.esekai2.api.player.skill.PlayerSkillCharges;
import kim.biryeong.esekai2.api.player.skill.PlayerSkillCooldowns;
import kim.biryeong.esekai2.api.player.skill.SelectedActiveSkillRef;
import kim.biryeong.esekai2.api.player.stat.PlayerCombatStats;
import kim.biryeong.esekai2.api.skill.definition.SkillDefinition;
import kim.biryeong.esekai2.api.skill.definition.SkillRegistries;
import kim.biryeong.esekai2.api.skill.execution.PreparedSkillUse;
import kim.biryeong.esekai2.api.skill.execution.SelectedSkillCastResult;
import kim.biryeong.esekai2.api.skill.execution.SelectedSkillUseResult;
import kim.biryeong.esekai2.api.skill.execution.SkillExecutionContext;
import kim.biryeong.esekai2.api.skill.execution.SkillExecutionResult;
import kim.biryeong.esekai2.api.skill.execution.SkillUseContext;
import kim.biryeong.esekai2.api.skill.execution.SkillUseContexts;
import kim.biryeong.esekai2.api.skill.execution.Skills;
import kim.biryeong.esekai2.api.skill.support.SkillSupportDefinition;
import kim.biryeong.esekai2.api.stat.definition.StatDefinition;
import kim.biryeong.esekai2.api.stat.definition.StatRegistries;
import kim.biryeong.esekai2.impl.player.resource.PlayerResourceRegistryAccess;
import kim.biryeong.esekai2.impl.player.skill.PlayerSkillBurstService;
import kim.biryeong.esekai2.impl.player.skill.PlayerSkillCooldownService;
import kim.biryeong.esekai2.impl.player.stat.PlayerCombatStatService;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * Debug commands for manually testing skill and player runtime state on a live server.
 */
public final class EsekaiDebugCommands {
    private static final String ESEKAI_NAMESPACE = "esekai2";
    private static boolean bootstrapped;
    private static final SuggestionProvider<CommandSourceStack> SKILL_ID_SUGGESTIONS = (context, builder) ->
            suggestIdentifiers(skillRegistry(context.getSource()).keySet(), builder);
    private static final SuggestionProvider<CommandSourceStack> SUPPORT_ID_SUGGESTIONS = (context, builder) ->
            suggestIdentifiers(supportRegistry(context.getSource()).keySet(), builder);
    private static final SuggestionProvider<CommandSourceStack> SUPPORT_ID_LIST_SUGGESTIONS = EsekaiDebugCommands::suggestSupportIdList;
    private static final SuggestionProvider<CommandSourceStack> STAT_ID_SUGGESTIONS = (context, builder) ->
            suggestIdentifiers(statRegistry(context.getSource()).keySet(), builder);
    private static final SuggestionProvider<CommandSourceStack> RESOURCE_ID_SUGGESTIONS = (context, builder) ->
            suggestResourceIds(context.getSource().getServer(), builder);
    private static final SuggestionProvider<CommandSourceStack> SLOT_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(
                    Stream.of(SocketedEquipmentSlot.values()).map(SocketedEquipmentSlot::serializedName),
                    builder
            );

    private EsekaiDebugCommands() {
    }

    public static void bootstrap() {
        if (bootstrapped) {
            return;
        }
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher));
        bootstrapped = true;
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("esekai")
                .requires(DebugCommandPermissions::canUse)
                .then(Commands.literal("debug")
                        .then(skillCommands())
                        .then(ailmentCommands())
                        .then(resourceCommands())
                        .then(levelCommands())
                        .then(statCommands())
                        .then(runtimeCommands())));
    }

    private static com.mojang.brigadier.builder.ArgumentBuilder<CommandSourceStack, ?> skillCommands() {
        return Commands.literal("skill")
                .then(Commands.literal("prepare")
                        .then(Commands.argument("skill_id", IdentifierArgument.id())
                                .suggests(SKILL_ID_SUGGESTIONS)
                                .executes(context -> prepareSkill(context, Optional.empty()))
                                .then(Commands.argument("target", EntityArgument.entity())
                                        .executes(context -> prepareSkill(
                                                context,
                                                Optional.of(EntityArgument.getEntity(context, "target"))
                                        )))))
                .then(Commands.literal("cast")
                        .then(Commands.argument("skill_id", IdentifierArgument.id())
                                .suggests(SKILL_ID_SUGGESTIONS)
                                .executes(context -> castSkill(context, Optional.empty()))
                                .then(Commands.argument("target", EntityArgument.entity())
                                        .executes(context -> castSkill(
                                                context,
                                                Optional.of(EntityArgument.getEntity(context, "target"))
                                        )))))
                .then(Commands.literal("bind")
                        .then(Commands.argument("slot", StringArgumentType.word())
                                .suggests(SLOT_SUGGESTIONS)
                                .then(Commands.argument("skill_id", IdentifierArgument.id())
                                        .suggests(SKILL_ID_SUGGESTIONS)
                                        .executes(context -> bindSelectedSkill(context, Optional.empty()))
                                        .then(Commands.argument("supports", StringArgumentType.greedyString())
                                                .suggests(SUPPORT_ID_LIST_SUGGESTIONS)
                                                .executes(context -> bindSelectedSkill(
                                                        context,
                                                        Optional.of(StringArgumentType.getString(context, "supports"))
                                                ))))))
                .then(Commands.literal("show_selected")
                        .executes(EsekaiDebugCommands::showSelectedSkill))
                .then(Commands.literal("clear_selected")
                        .executes(EsekaiDebugCommands::clearSelectedSkill))
                .then(Commands.literal("cast_selected")
                        .executes(context -> castSelectedSkill(context, Optional.empty()))
                        .then(Commands.argument("target", EntityArgument.entity())
                                .executes(context -> castSelectedSkill(
                                        context,
                                        Optional.of(EntityArgument.getEntity(context, "target"))
                                ))));
    }

    private static com.mojang.brigadier.builder.ArgumentBuilder<CommandSourceStack, ?> resourceCommands() {
        return Commands.literal("resource")
                .then(Commands.literal("get")
                        .then(Commands.argument("resource", IdentifierArgument.id())
                                .suggests(RESOURCE_ID_SUGGESTIONS)
                                .executes(context -> getResource(context, sourcePlayer(context)))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> getResource(context, EntityArgument.getPlayer(context, "player"))))))
                .then(Commands.literal("set")
                        .then(Commands.argument("resource", IdentifierArgument.id())
                                .suggests(RESOURCE_ID_SUGGESTIONS)
                                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0))
                                        .executes(context -> setResource(context, sourcePlayer(context)))
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(context -> setResource(
                                                        context,
                                                        EntityArgument.getPlayer(context, "player")
                                                ))))));
    }

    private static com.mojang.brigadier.builder.ArgumentBuilder<CommandSourceStack, ?> ailmentCommands() {
        return Commands.literal("ailment")
                .then(Commands.literal("inspect")
                        .executes(context -> inspectAilments(context, sourcePlayer(context)))
                        .then(Commands.argument("target", EntityArgument.entity())
                                .executes(context -> inspectAilments(
                                        context,
                                        EntityArgument.getEntity(context, "target")
                                ))));
    }

    private static com.mojang.brigadier.builder.ArgumentBuilder<CommandSourceStack, ?> levelCommands() {
        return Commands.literal("level")
                .then(Commands.literal("get")
                        .executes(context -> getLevel(context, sourcePlayer(context)))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> getLevel(context, EntityArgument.getPlayer(context, "player")))))
                .then(Commands.literal("set")
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 100))
                                .executes(context -> setLevel(context, sourcePlayer(context)))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> setLevel(context, EntityArgument.getPlayer(context, "player"))))))
                .then(Commands.literal("add_xp")
                        .then(Commands.argument("amount", LongArgumentType.longArg(0L))
                                .executes(context -> addExperience(context, sourcePlayer(context)))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> addExperience(
                                                context,
                                                EntityArgument.getPlayer(context, "player")
                                        )))));
    }

    private static com.mojang.brigadier.builder.ArgumentBuilder<CommandSourceStack, ?> statCommands() {
        return Commands.literal("stat")
                .then(Commands.literal("get")
                        .then(Commands.argument("stat_id", IdentifierArgument.id())
                                .suggests(STAT_ID_SUGGESTIONS)
                                .executes(context -> getStat(context, sourcePlayer(context)))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> getStat(context, EntityArgument.getPlayer(context, "player"))))))
                .then(Commands.literal("set")
                        .then(Commands.argument("stat_id", IdentifierArgument.id())
                                .suggests(STAT_ID_SUGGESTIONS)
                                .then(Commands.argument("value", DoubleArgumentType.doubleArg())
                                        .executes(context -> setStat(context, sourcePlayer(context)))
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(context -> setStat(
                                                        context,
                                                        EntityArgument.getPlayer(context, "player")
                                                ))))));
    }

    private static com.mojang.brigadier.builder.ArgumentBuilder<CommandSourceStack, ?> runtimeCommands() {
        return Commands.literal("runtime")
                .then(Commands.literal("inspect")
                        .then(Commands.argument("skill_id", IdentifierArgument.id())
                                .suggests(SKILL_ID_SUGGESTIONS)
                                .executes(context -> inspectRuntime(context, sourcePlayer(context)))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> inspectRuntime(context, EntityArgument.getPlayer(context, "player"))))))
                .then(Commands.literal("reset")
                        .then(Commands.argument("skill_id", IdentifierArgument.id())
                                .suggests(SKILL_ID_SUGGESTIONS)
                                .executes(context -> resetRuntime(context, sourcePlayer(context), Optional.empty()))
                                .then(Commands.argument("max_charges", IntegerArgumentType.integer(0))
                                        .executes(context -> resetRuntime(
                                                context,
                                                sourcePlayer(context),
                                                Optional.of(IntegerArgumentType.getInteger(context, "max_charges"))
                                        ))
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(context -> resetRuntime(
                                                        context,
                                                        EntityArgument.getPlayer(context, "player"),
                                                        Optional.of(IntegerArgumentType.getInteger(context, "max_charges"))
                                                ))))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> resetRuntime(
                                                context,
                                                EntityArgument.getPlayer(context, "player"),
                                                Optional.empty()
                                        )))));
    }

    private static int prepareSkill(CommandContext<CommandSourceStack> context, Optional<Entity> target) throws CommandSyntaxException {
        ServerPlayer player = sourcePlayer(context);
        SkillDefinition skill = requireSkill(context.getSource(), IdentifierArgument.getId(context, "skill_id"));
        SkillUseContext useContext = liveUseContext(player, target);
        PreparedSkillUse prepared = Skills.prepareUse(skill, useContext);
        sendInfo(context.getSource(), "Prepared " + skill.identifier()
                + " resource=" + prepared.resource()
                + " cost=" + trim(prepared.resourceCost())
                + " cast_time=" + prepared.useTimeTicks()
                + " cooldown=" + prepared.cooldownTicks()
                + " on_cast_actions=" + prepared.onCastActions().size()
                + " components=" + prepared.components().size());
        sendWarnings(context.getSource(), prepared.warnings());
        return 1;
    }

    private static int castSkill(CommandContext<CommandSourceStack> context, Optional<Entity> target) throws CommandSyntaxException {
        ServerPlayer player = sourcePlayer(context);
        SkillDefinition skill = requireSkill(context.getSource(), IdentifierArgument.getId(context, "skill_id"));
        SkillUseContext useContext = liveUseContext(player, target);
        PreparedSkillUse prepared = Skills.prepareUse(skill, useContext);
        SkillExecutionResult result = Skills.executeOnCast(
                SkillExecutionContext.forCast(prepared, player.level(), player, target)
        );
        sendInfo(context.getSource(), "Cast " + skill.identifier()
                + " executed=" + result.executedActions()
                + " skipped=" + result.skippedActions()
                + " resource=" + prepared.resource()
                + " cost=" + trim(prepared.resourceCost()));
        sendWarnings(context.getSource(), prepared.warnings());
        sendWarnings(context.getSource(), result.warnings());
        return 1;
    }

    private static int bindSelectedSkill(CommandContext<CommandSourceStack> context, Optional<String> supportsRaw) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = sourcePlayer(context);
        SocketedEquipmentSlot slot = requireSlot(StringArgumentType.getString(context, "slot"));
        Identifier skillId = resolveProjectScopedId(skillRegistry(source), IdentifierArgument.getId(context, "skill_id"));
        Registry<SkillDefinition> skillRegistry = skillRegistry(source);
        Registry<SkillSupportDefinition> supportRegistry = supportRegistry(source);
        if (skillRegistry.getOptional(skillId).isEmpty()) {
            throw failure("Unknown skill: " + skillId);
        }

        List<Identifier> supportIds = parseSupportIds(supportsRaw);
        for (Identifier supportId : supportIds) {
            if (supportRegistry.getOptional(supportId).isEmpty()) {
                throw failure("Unknown support: " + supportId);
            }
        }

        ItemStack stack = SocketedSkills.getEquippedStack(player, slot);
        if (stack.isEmpty()) {
            throw failure("No item equipped in slot " + slot.serializedName());
        }

        int socketCount = 1 + supportIds.size();
        List<SocketedSkillRef> refs = new ArrayList<>(socketCount);
        refs.add(new SocketedSkillRef(0, SocketSlotType.SKILL, skillId));
        for (int index = 0; index < supportIds.size(); index++) {
            refs.add(new SocketedSkillRef(index + 1, SocketSlotType.SUPPORT, supportIds.get(index)));
        }
        List<SocketLinkGroup> groups = socketCount > 1
                ? List.of(new SocketLinkGroup(0, java.util.stream.IntStream.range(0, socketCount).boxed().toList()))
                : List.of();
        SocketedSkills.set(stack, new SocketedSkillItemState(Optional.of(skillId), socketCount, groups, refs));
        PlayerActiveSkills.select(player, new SelectedActiveSkillRef(slot, skillId));

        sendInfo(source, "Bound selected skill " + skillId + " to " + slot.serializedName()
                + " supports=" + supportIds.size());
        if (!supportIds.isEmpty()) {
            sendInfo(source, "Linked supports: " + joinIds(supportIds));
        }
        return 1;
    }

    private static int showSelectedSkill(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = sourcePlayer(context);
        Optional<SelectedActiveSkillRef> selection = PlayerActiveSkills.get(player);
        if (selection.isEmpty()) {
            sendInfo(context.getSource(), "No selected active skill");
            return 1;
        }
        SelectedActiveSkillRef ref = selection.orElseThrow();
        sendInfo(context.getSource(), "Selected skill slot=" + ref.equipmentSlot().serializedName() + " skill=" + ref.skillId());
        return 1;
    }

    private static int clearSelectedSkill(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = sourcePlayer(context);
        Optional<SelectedActiveSkillRef> cleared = PlayerActiveSkills.clear(player);
        sendInfo(context.getSource(), cleared.map(ref -> "Cleared selected skill " + ref.skillId()).orElse("No selected active skill to clear"));
        return 1;
    }

    private static int castSelectedSkill(CommandContext<CommandSourceStack> context, Optional<Entity> target) throws CommandSyntaxException {
        ServerPlayer player = sourcePlayer(context);
        SelectedSkillCastResult result = Skills.castSelectedSkill(player, liveUseContext(player, target), target);
        sendInfo(context.getSource(), "Selected cast success=" + result.success()
                + " prepared=" + result.preparedUse().isPresent()
                + " executed=" + result.executionResult().map(SkillExecutionResult::executedActions).orElse(0)
                + " skipped=" + result.executionResult().map(SkillExecutionResult::skippedActions).orElse(0));
        sendWarnings(context.getSource(), result.warnings());
        return result.success() ? 1 : 0;
    }

    private static int getResource(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        String resource = resourceArgument(context);
        if (!PlayerResources.supports(resource)) {
            throw failure("Unsupported resource: " + resource);
        }
        double current = PlayerResources.getAmount(player, resource);
        double max = PlayerResources.maxAmount(player, resource);
        double regeneration = PlayerResources.regenerationPerSecond(player, resource);
        sendInfo(context.getSource(), "Resource " + resource
                + " player=" + playerName(player)
                + " current=" + trim(current)
                + " max=" + trim(max)
                + " regen_per_second=" + trim(regeneration));
        return 1;
    }

    private static int inspectAilments(CommandContext<CommandSourceStack> context, Entity target) {
        if (!(target instanceof LivingEntity livingTarget)) {
            throw failure("Target must be a living entity");
        }

        CommandSourceStack source = context.getSource();
        boolean found = false;
        sendInfo(source, "Ailments target=" + targetName(livingTarget));
        for (AilmentType type : AilmentType.values()) {
            Optional<AilmentPayload> payload = Ailments.get(livingTarget).flatMap(state -> state.get(type));
            MobEffectInstance effectInstance = effectInstance(type, livingTarget).orElse(null);
            if (payload.isEmpty() && effectInstance == null) {
                continue;
            }

            found = true;
            String payloadSummary = payload.map(value -> "payload{potency=" + trim(value.potency())
                    + ", remaining=" + value.remainingTicks()
                    + "/" + value.durationTicks()
                    + ", tick_interval=" + value.tickIntervalTicks()
                    + ", source_skill=" + value.sourceSkillId()
                    + "}").orElse("payload{none}");
            String effectSummary = effectInstance != null
                    ? "effect{duration=" + effectInstance.getDuration()
                    + ", amplifier=" + effectInstance.getAmplifier()
                    + ", visible=" + effectInstance.isVisible()
                    + ", icon=" + effectInstance.showIcon()
                    + ", ambient=" + effectInstance.isAmbient()
                    + "}"
                    : "effect{none}";
            sendInfo(source, type.serializedName() + " " + payloadSummary + " " + effectSummary);
        }

        if (!found) {
            sendInfo(source, "No ailment payloads or ailment MobEffects are currently active");
        }
        return found ? 1 : 0;
    }

    private static int setResource(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        String resource = resourceArgument(context);
        if (!PlayerResources.supports(resource)) {
            throw failure("Unsupported resource: " + resource);
        }
        double amount = DoubleArgumentType.getDouble(context, "amount");
        PlayerResources.set(player, resource, amount);
        sendInfo(context.getSource(), "Set resource " + resource
                + " player=" + playerName(player)
                + " current=" + trim(PlayerResources.getAmount(player, resource))
                + " max=" + trim(PlayerResources.maxAmount(player, resource)));
        return 1;
    }

    private static int getLevel(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        PlayerLevelState state = PlayerLevels.get(player);
        sendInfo(context.getSource(), "Level player=" + playerName(player)
                + " level=" + state.level()
                + " in_level_xp=" + state.experienceInLevel()
                + " total_xp=" + state.totalExperience()
                + " xp_to_next=" + PlayerLevels.experienceToNextLevel(player));
        return 1;
    }

    private static int setLevel(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        int level = IntegerArgumentType.getInteger(context, "level");
        PlayerLevelState state = PlayerLevels.setLevel(player, level);
        sendInfo(context.getSource(), "Set level player=" + playerName(player)
                + " level=" + state.level()
                + " total_xp=" + state.totalExperience());
        return 1;
    }

    private static int addExperience(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        long amount = LongArgumentType.getLong(context, "amount");
        PlayerLevelState state = PlayerLevels.addExperience(player, amount);
        sendInfo(context.getSource(), "Added xp player=" + playerName(player)
                + " added=" + amount
                + " level=" + state.level()
                + " total_xp=" + state.totalExperience());
        return 1;
    }

    private static int getStat(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        Registry<StatDefinition> statRegistry = statRegistry(context.getSource());
        Identifier statId = resolveProjectScopedId(statRegistry, IdentifierArgument.getId(context, "stat_id"));
        if (statRegistry.getOptional(statId).isEmpty()) {
            throw failure("Unknown stat: " + statId);
        }
        ResourceKey<StatDefinition> statKey = ResourceKey.create(StatRegistries.STAT, statId);
        sendInfo(context.getSource(), "Stat " + statId
                + " player=" + playerName(player)
                + " value=" + trim(PlayerCombatStats.resolvedValue(player, statKey)));
        return 1;
    }

    private static int setStat(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        Registry<StatDefinition> statRegistry = statRegistry(context.getSource());
        Identifier statId = resolveProjectScopedId(statRegistry, IdentifierArgument.getId(context, "stat_id"));
        if (statRegistry.getOptional(statId).isEmpty()) {
            throw failure("Unknown stat: " + statId);
        }
        double value = DoubleArgumentType.getDouble(context, "value");
        ResourceKey<StatDefinition> statKey = ResourceKey.create(StatRegistries.STAT, statId);
        PlayerCombatStatService.setBaseValue(player, statKey, value);
        sendInfo(context.getSource(), "Set stat " + statId
                + " player=" + playerName(player)
                + " value=" + trim(PlayerCombatStats.resolvedValue(player, statKey)));
        return 1;
    }

    private static int inspectRuntime(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        CommandSourceStack source = context.getSource();
        Registry<SkillDefinition> skillRegistry = skillRegistry(source);
        Identifier skillId = resolveProjectScopedId(skillRegistry, IdentifierArgument.getId(context, "skill_id"));
        int maxCharges = skillRegistry.getOptional(skillId)
                .map(skill -> skill.config().charges())
                .orElse(0);
        long gameTime = player.level().getGameTime();
        long cooldownRemaining = PlayerSkillCooldowns.remainingTicks(player, skillId, gameTime);
        PlayerSkillChargeState.SkillChargeEntry charges = PlayerSkillCharges.get(player, skillId, maxCharges, gameTime);
        int burstRemaining = PlayerSkillBursts.remainingCasts(player, skillId, gameTime);
        boolean burstActive = PlayerSkillBursts.hasActiveBurst(player, skillId, gameTime);
        sendInfo(source, "Runtime " + skillId
                + " player=" + playerName(player)
                + " cooldown_remaining=" + cooldownRemaining
                + " charges=" + charges.currentCharges() + "/" + maxCharges
                + " pending_recharges=" + charges.pendingReadyGameTimes().size()
                + " burst_remaining=" + burstRemaining
                + " burst_active=" + burstActive);
        return 1;
    }

    private static int resetRuntime(
            CommandContext<CommandSourceStack> context,
            ServerPlayer player,
            Optional<Integer> explicitMaxCharges
    ) {
        CommandSourceStack source = context.getSource();
        Registry<SkillDefinition> skillRegistry = skillRegistry(source);
        Identifier skillId = resolveProjectScopedId(skillRegistry, IdentifierArgument.getId(context, "skill_id"));
        int maxCharges = explicitMaxCharges.orElseGet(() -> skillRegistry.getOptional(skillId)
                .map(skill -> skill.config().charges())
                .orElse(0));
        long gameTime = player.level().getGameTime();
        PlayerSkillCooldownService.clear(player, skillId, gameTime);
        PlayerSkillBurstService.clear(player, skillId, gameTime);
        PlayerSkillCharges.setAvailableCharges(player, skillId, maxCharges, maxCharges, gameTime);
        sendInfo(source, "Reset runtime " + skillId
                + " player=" + playerName(player)
                + " charges=" + maxCharges
                + " cooldown_remaining=" + PlayerSkillCooldowns.remainingTicks(player, skillId, gameTime)
                + " burst_remaining=" + PlayerSkillBursts.remainingCasts(player, skillId, gameTime));
        return 1;
    }

    private static SkillUseContext liveUseContext(ServerPlayer player, Optional<Entity> target) {
        return SkillUseContexts.forPlayer(player, target, 0.0, 0.0);
    }

    private static Registry<SkillDefinition> skillRegistry(CommandSourceStack source) {
        return source.getServer().registryAccess().lookupOrThrow(SkillRegistries.SKILL);
    }

    private static Registry<SkillSupportDefinition> supportRegistry(CommandSourceStack source) {
        return source.getServer().registryAccess().lookupOrThrow(SkillRegistries.SKILL_SUPPORT);
    }

    private static Registry<StatDefinition> statRegistry(CommandSourceStack source) {
        return source.getServer().registryAccess().lookupOrThrow(StatRegistries.STAT);
    }

    private static SkillDefinition requireSkill(CommandSourceStack source, Identifier requestedSkillId) {
        Identifier skillId = resolveProjectScopedId(skillRegistry(source), requestedSkillId);
        return skillRegistry(source).getOptional(skillId)
                .orElseThrow(() -> failure("Unknown skill: " + skillId));
    }

    private static ServerPlayer sourcePlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return context.getSource().getPlayerOrException();
    }

    private static SocketedEquipmentSlot requireSlot(String rawSlot) {
        for (SocketedEquipmentSlot slot : SocketedEquipmentSlot.values()) {
            if (slot.serializedName().equals(rawSlot)) {
                return slot;
            }
        }
        throw failure("Unknown slot: " + rawSlot);
    }

    private static Identifier requireIdentifier(String rawId, String fieldName) {
        Identifier identifier = Identifier.tryParse(rawId);
        if (identifier == null) {
            throw failure("Invalid " + fieldName + ": " + rawId);
        }
        return identifier;
    }

    private static List<Identifier> parseSupportIds(Optional<String> supportsRaw) {
        if (supportsRaw.isEmpty() || supportsRaw.orElseThrow().isBlank()) {
            return List.of();
        }
        String[] parts = supportsRaw.orElseThrow().split(",");
        List<Identifier> identifiers = new ArrayList<>(parts.length);
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            Identifier parsed = Identifier.tryParse(trimmed);
            if (parsed == null) {
                throw failure("Invalid support_id: " + trimmed);
            }
            identifiers.add(parsed);
        }
        return List.copyOf(identifiers);
    }

    private static String joinIds(List<Identifier> identifiers) {
        return identifiers.stream().map(Identifier::toString).reduce((left, right) -> left + ", " + right).orElse("");
    }

    private static String trim(double value) {
        if (Math.rint(value) == value) {
            return Long.toString((long) value);
        }
        return Double.toString(value);
    }

    private static String playerName(ServerPlayer player) {
        return player.getScoreboardName();
    }

    private static String targetName(Entity entity) {
        return entity.getName().getString() + "#" + entity.getId();
    }

    private static String resourceArgument(CommandContext<CommandSourceStack> context) {
        MinecraftServer server = context.getSource().getServer();
        Identifier requested = IdentifierArgument.getId(context, "resource");
        Registry<?> registry = PlayerResourceRegistryAccess.resourceRegistry(server);
        Identifier resolved = resolveProjectScopedId(registry, requested);
        if (ESEKAI_NAMESPACE.equals(resolved.getNamespace())) {
            return resolved.getPath();
        }
        return resolved.toString();
    }

    private static Identifier resolveProjectScopedId(Registry<?> registry, Identifier requested) {
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(requested, "requested");
        if (registry.getOptional(requested).isPresent()) {
            return requested;
        }
        if ("minecraft".equals(requested.getNamespace())) {
            Identifier projectScoped = Identifier.fromNamespaceAndPath(ESEKAI_NAMESPACE, requested.getPath());
            if (registry.getOptional(projectScoped).isPresent()) {
                return projectScoped;
            }
        }
        return requested;
    }

    private static CompletableFuture<Suggestions> suggestIdentifiers(Iterable<Identifier> identifiers, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(identifierSuggestions(identifiers).stream(), builder);
    }

    private static CompletableFuture<Suggestions> suggestResourceIds(MinecraftServer server, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(
                identifierSuggestions(PlayerResourceRegistryAccess.resourceRegistry(server).keySet()).stream(),
                builder
        );
    }

    private static CompletableFuture<Suggestions> suggestSupportIdList(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder
    ) {
        String remaining = builder.getRemaining();
        int lastComma = remaining.lastIndexOf(',');
        String prefix = lastComma >= 0 ? remaining.substring(0, lastComma + 1) : "";
        String token = lastComma >= 0 ? remaining.substring(lastComma + 1).trim() : remaining.trim();
        for (String suggestion : identifierSuggestions(supportRegistry(context.getSource()).keySet())) {
            if (token.isEmpty() || SharedSuggestionProvider.matchesSubStr(token, suggestion)) {
                String insertion = prefix.isEmpty() ? suggestion : prefix + " " + suggestion;
                builder.suggest(insertion);
            }
        }
        return builder.buildFuture();
    }

    private static List<String> identifierSuggestions(Iterable<Identifier> identifiers) {
        LinkedHashSet<String> suggestions = new LinkedHashSet<>();
        for (Identifier identifier : identifiers) {
            suggestions.add(identifier.toString());
            if (ESEKAI_NAMESPACE.equals(identifier.getNamespace())) {
                suggestions.add(identifier.getPath());
            }
        }
        return List.copyOf(suggestions);
    }

    private static Optional<MobEffectInstance> effectInstance(AilmentType type, LivingEntity entity) {
        MobEffect effect = BuiltInRegistries.MOB_EFFECT.getOptional(type.effectId()).orElse(null);
        if (effect == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(entity.getEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect)));
    }

    private static void sendWarnings(CommandSourceStack source, List<String> warnings) {
        Objects.requireNonNull(warnings, "warnings");
        for (String warning : warnings) {
            if (warning == null || warning.isBlank()) {
                continue;
            }
            sendInfo(source, "warning: " + warning);
        }
    }

    private static void sendInfo(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal("[ESekai Debug] " + message), false);
    }

    private static IllegalArgumentException failure(String message) {
        return new IllegalArgumentException(message);
    }
}
