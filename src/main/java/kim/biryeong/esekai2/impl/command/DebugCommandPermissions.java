package kim.biryeong.esekai2.impl.command;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandSourceStack;

/**
 * Shared permission gate for ESekai debug commands.
 */
public final class DebugCommandPermissions {
    public static final String DEBUG_COMMANDS = "esekai.debug_commands";

    private DebugCommandPermissions() {
    }

    public static boolean canUse(CommandSourceStack source) {
        return Permissions.check(source, DEBUG_COMMANDS, 2);
    }
}
