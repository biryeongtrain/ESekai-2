package kim.biryeong.esekai2.api.player.resource;

import kim.biryeong.esekai2.impl.player.resource.PlayerResourceService;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * Public entry points for interacting with ESekai's server-side player resources.
 *
 * <p>The existing mana API remains as a convenience wrapper over the generic named-resource
 * storage introduced for future multi-resource work. Registry-backed overloads are the
 * authoritative runtime contract used by skill execution, value expressions, predicates, and
 * regeneration. Explicit-max overloads remain compatibility seams for ad-hoc named buckets that
 * live outside the registry-backed runtime graph.</p>
 */
public final class PlayerResources {
    private PlayerResources() {
    }

    /**
     * Returns whether the provided resource id is supported by the current runtime.
     *
     * <p>This only reflects registry-backed runtime support. Callers may still read or write
     * explicit-max compatibility buckets even when this method returns {@code false}.</p>
     *
     * @param resource resource id to inspect
     * @return {@code true} when the runtime currently supports the resource id
     */
    public static boolean supports(String resource) {
        return PlayerResourceService.supports(resource);
    }

    /**
     * Returns the current persistent state for one registered player resource.
     *
     * @param player player to inspect
     * @param resource registered resource id to inspect
     * @return current persistent resource state
     */
    public static PlayerTrackedResourceState get(ServerPlayer player, String resource) {
        return PlayerResourceService.get(player, resource);
    }

    /**
     * Returns the current persistent state for one named player resource, creating it from the
     * provided maximum amount when missing.
     *
     * <p>This overload is a compatibility seam for callers that manage ad-hoc named buckets
     * outside the registry-backed runtime graph. Registered skill/runtime execution, regeneration,
     * and predicate/value evaluation only honor registered player resources, and calling this
     * overload does not make the provided id {@linkplain #supports(String) supported}.</p>
     *
     * @param player player to inspect
     * @param resource named resource to inspect
     * @param maxAmount current maximum amount used to initialize or clamp stored state
     * @return current persistent resource state
     */
    public static PlayerTrackedResourceState get(ServerPlayer player, String resource, double maxAmount) {
        return PlayerResourceService.get(player, resource, maxAmount);
    }

    /**
     * Returns the current amount for one registered player resource.
     *
     * @param player player to inspect
     * @param resource registered resource id to inspect
     * @return current amount stored for the resource
     */
    public static double getAmount(ServerPlayer player, String resource) {
        return PlayerResourceService.get(player, resource).currentAmount();
    }

    /**
     * Returns the current amount for one named player resource, creating it from the provided
     * maximum amount when missing.
     *
     * <p>This overload is a compatibility seam for callers that manage ad-hoc named buckets
     * outside the registry-backed runtime graph. Registered skill/runtime execution, regeneration,
     * and predicate/value evaluation only honor registered player resources, and calling this
     * overload does not make the provided id {@linkplain #supports(String) supported}.</p>
     *
     * @param player player to inspect
     * @param resource named resource to inspect
     * @param maxAmount current maximum amount used to initialize or clamp stored state
     * @return current amount stored for the resource
     */
    public static double getAmount(ServerPlayer player, String resource, double maxAmount) {
        return PlayerResourceService.get(player, resource, maxAmount).currentAmount();
    }

    /**
     * Replaces one registered player resource amount.
     *
     * @param player player to update
     * @param resource registered resource id to update
     * @param currentAmount replacement current amount
     * @return updated persistent resource state
     */
    public static PlayerTrackedResourceState set(ServerPlayer player, String resource, double currentAmount) {
        return PlayerResourceService.set(player, resource, currentAmount);
    }

    /**
     * Replaces one named player resource amount, clamped to the provided maximum amount.
     *
     * <p>This overload is a compatibility seam for callers that manage ad-hoc named buckets
     * outside the registry-backed runtime graph. Registered skill/runtime execution, regeneration,
     * and predicate/value evaluation only honor registered player resources, and calling this
     * overload does not make the provided id {@linkplain #supports(String) supported}.</p>
     *
     * @param player player to update
     * @param resource named resource to update
     * @param currentAmount replacement current amount
     * @param maxAmount current maximum amount used to clamp stored state
     * @return updated persistent resource state
     */
    public static PlayerTrackedResourceState set(ServerPlayer player, String resource, double currentAmount, double maxAmount) {
        return PlayerResourceService.set(player, resource, currentAmount, maxAmount);
    }

    /**
     * Applies a signed delta to one registered player resource.
     *
     * @param player player to update
     * @param resource registered resource id to update
     * @param amount signed resource delta
     * @return updated persistent resource state
     */
    public static PlayerTrackedResourceState add(ServerPlayer player, String resource, double amount) {
        return PlayerResourceService.add(player, resource, amount);
    }

    /**
     * Applies a signed delta to one named player resource and clamps the result to
     * {@code [0, maxAmount]}.
     *
     * <p>This overload is a compatibility seam for callers that manage ad-hoc named buckets
     * outside the registry-backed runtime graph. Registered skill/runtime execution, regeneration,
     * and predicate/value evaluation only honor registered player resources, and calling this
     * overload does not make the provided id {@linkplain #supports(String) supported}.</p>
     *
     * @param player player to update
     * @param resource named resource to update
     * @param amount signed resource delta
     * @param maxAmount current maximum amount used to initialize or clamp stored state
     * @return updated persistent resource state
     */
    public static PlayerTrackedResourceState add(ServerPlayer player, String resource, double amount, double maxAmount) {
        return PlayerResourceService.add(player, resource, amount, maxAmount);
    }

    /**
     * Attempts to spend one registered player resource.
     *
     * @param player player to update
     * @param resource registered resource id to spend
     * @param amount amount to spend
     * @return updated state when the spend succeeded
     */
    public static Optional<PlayerTrackedResourceState> spend(ServerPlayer player, String resource, double amount) {
        return PlayerResourceService.spend(player, resource, amount);
    }

    /**
     * Attempts to spend one named player resource.
     *
     * <p>This overload is a compatibility seam for callers that manage ad-hoc named buckets
     * outside the registry-backed runtime graph. Registered skill/runtime execution, regeneration,
     * and predicate/value evaluation only honor registered player resources, and calling this
     * overload does not make the provided id {@linkplain #supports(String) supported}.</p>
     *
     * @param player player to update
     * @param resource named resource to spend
     * @param amount amount to spend
     * @param maxAmount current maximum amount used to initialize or clamp stored state
     * @return updated state when the spend succeeded
     */
    public static Optional<PlayerTrackedResourceState> spend(ServerPlayer player, String resource, double amount, double maxAmount) {
        return PlayerResourceService.spend(player, resource, amount, maxAmount);
    }

    /**
     * Returns the current maximum amount for one registered player resource.
     *
     * @param player player to inspect
     * @param resource registered resource id to inspect
     * @return current maximum amount resolved from player combat stats
     */
    public static double maxAmount(ServerPlayer player, String resource) {
        return PlayerResourceService.maxAmount(player, resource);
    }

    /**
     * Returns the current regeneration per second for one registered player resource.
     *
     * @param player player to inspect
     * @param resource registered resource id to inspect
     * @return current regeneration rate resolved from player combat stats
     */
    public static double regenerationPerSecond(ServerPlayer player, String resource) {
        return PlayerResourceService.regenerationPerSecond(player, resource);
    }

    /**
     * Returns the current persistent mana state for the player, creating it from the provided max
     * mana when missing.
     *
     * <p>This overload is the mana-specific compatibility seam for callers that still manage an
     * explicit max value outside the registered resource graph. Registered skill/runtime
     * execution, regeneration, and predicate/value evaluation continue to follow the registry-
     * backed mana definition instead.</p>
     *
     * @param player player to inspect
     * @param maxMana current maximum mana used to initialize or clamp stored state
     * @return current persistent mana state
     */
    public static PlayerResourceState get(ServerPlayer player, double maxMana) {
        return manaState(get(player, PlayerResourceIds.MANA, maxMana));
    }

    /**
     * Returns the current mana value for the player, creating it from the provided max mana when
     * missing.
     *
     * <p>This overload is the mana-specific compatibility seam for callers that still manage an
     * explicit max value outside the registered resource graph. Registered skill/runtime
     * execution, regeneration, and predicate/value evaluation continue to follow the registry-
     * backed mana definition instead.</p>
     *
     * @param player player to inspect
     * @param maxMana current maximum mana used to initialize or clamp stored state
     * @return current mana value
     */
    public static double getMana(ServerPlayer player, double maxMana) {
        return getAmount(player, PlayerResourceIds.MANA, maxMana);
    }

    /**
     * Returns the current mana value using the registered mana resource definition.
     *
     * @param player player to inspect
     * @return current mana value
     */
    public static double getMana(ServerPlayer player) {
        return getAmount(player, PlayerResourceIds.MANA);
    }

    /**
     * Replaces the player's current mana, clamped to the provided max mana.
     *
     * <p>This overload is the mana-specific compatibility seam for callers that still manage an
     * explicit max value outside the registered resource graph. Registered skill/runtime
     * execution, regeneration, and predicate/value evaluation continue to follow the registry-
     * backed mana definition instead.</p>
     *
     * @param player player to update
     * @param currentMana replacement current mana
     * @param maxMana current maximum mana used to clamp stored state
     * @return updated persistent mana state
     */
    public static PlayerResourceState setMana(ServerPlayer player, double currentMana, double maxMana) {
        return manaState(set(player, PlayerResourceIds.MANA, currentMana, maxMana));
    }

    /**
     * Replaces the player's current mana using the registered mana resource definition.
     *
     * @param player player to update
     * @param currentMana replacement current mana
     * @return updated persistent mana state
     */
    public static PlayerResourceState setMana(ServerPlayer player, double currentMana) {
        return manaState(set(player, PlayerResourceIds.MANA, currentMana));
    }

    /**
     * Applies a signed mana delta to the player and clamps the resulting state to
     * {@code [0, maxMana]}.
     *
     * <p>This overload is the mana-specific compatibility seam for callers that still manage an
     * explicit max value outside the registered resource graph. Registered skill/runtime
     * execution, regeneration, and predicate/value evaluation continue to follow the registry-
     * backed mana definition instead.</p>
     *
     * @param player player to update
     * @param amount signed mana delta, where positive restores and negative spends
     * @param maxMana current maximum mana used to initialize or clamp stored state
     * @return updated persistent mana state
     */
    public static PlayerResourceState addMana(ServerPlayer player, double amount, double maxMana) {
        return manaState(add(player, PlayerResourceIds.MANA, amount, maxMana));
    }

    /**
     * Applies a signed mana delta using the registered mana resource definition.
     *
     * @param player player to update
     * @param amount signed mana delta
     * @return updated persistent mana state
     */
    public static PlayerResourceState addMana(ServerPlayer player, double amount) {
        return manaState(add(player, PlayerResourceIds.MANA, amount));
    }

    /**
     * Attempts to spend mana from the player.
     *
     * <p>This overload is the mana-specific compatibility seam for callers that still manage an
     * explicit max value outside the registered resource graph. Registered skill/runtime
     * execution, regeneration, and predicate/value evaluation continue to follow the registry-
     * backed mana definition instead.</p>
     *
     * @param player player to update
     * @param amount amount of mana to spend
     * @param maxMana current maximum mana used to initialize or clamp stored state
     * @return updated state when the spend succeeded
     */
    public static Optional<PlayerResourceState> spendMana(ServerPlayer player, double amount, double maxMana) {
        return spend(player, PlayerResourceIds.MANA, amount, maxMana).map(PlayerResources::manaState);
    }

    /**
     * Attempts to spend mana using the registered mana resource definition.
     *
     * @param player player to update
     * @param amount amount of mana to spend
     * @return updated state when the spend succeeded
     */
    public static Optional<PlayerResourceState> spendMana(ServerPlayer player, double amount) {
        return spend(player, PlayerResourceIds.MANA, amount).map(PlayerResources::manaState);
    }

    private static PlayerResourceState manaState(PlayerTrackedResourceState state) {
        return new PlayerResourceState(state.currentAmount());
    }
}
