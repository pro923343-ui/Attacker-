package com.ranggadev.adminsecattacker.auth;

import java.util.Locale;
import java.util.Set;

/**
 * Deterministic Phase 7 authorization contract model.
 *
 * This is a QA model, not a replacement for AdminSecurity authorization.
 * It encodes the security boundaries observed in AdminSecurity 4.2.x so the
 * AttackerLab can regression-test those expectations without executing commands.
 */
public final class AuthorizationContract {
    public enum Role { MASTER_OWNER, OWNER, ADMIN, STAFF, PLAYER }
    public enum Decision { ALLOW, DENY }

    private AuthorizationContract() {}

    public static Decision command(boolean authenticated, Role role, boolean permission, String commandRoot) {
        if (!authenticated) return Decision.DENY;
        if (commandRoot == null || commandRoot.isBlank()) return Decision.DENY;
        String root = commandRoot.toLowerCase(Locale.ROOT);
        if (Set.of("op", "deop", "lp", "luckperms", "pex", "permissions").contains(root)
                && role != Role.MASTER_OWNER && role != Role.OWNER) return Decision.DENY;
        return permission || role == Role.MASTER_OWNER || role == Role.OWNER
                ? Decision.ALLOW : Decision.DENY;
    }

    public static Decision targetAction(boolean authenticated, Role actor, Role target, boolean permission,
                                        boolean selfTarget, String action) {
        if (!authenticated || actor == null || target == null || action == null || action.isBlank()) return Decision.DENY;
        if (selfTarget && Set.of("role.manage", "authentication.manage", "session.revoke",
                "profile.kick", "profile.freeze", "profile.ban", "profile.unban",
                "profile.remove", "profile.reset2fa", "profile.clear", "profile.kill",
                "profile.irp").contains(action.toLowerCase(Locale.ROOT))) return Decision.DENY;
        if (!isMaster(actor) && level(target) >= level(actor)) return Decision.DENY;
        return permission || isMaster(actor) || actor != Role.PLAYER ? Decision.ALLOW : Decision.DENY;
    }

    private static boolean isMaster(Role role) { return role == Role.MASTER_OWNER; }
    private static int level(Role role) {
        return switch (role) {
            case MASTER_OWNER -> 4;
            case OWNER -> 3;
            case ADMIN -> 2;
            case STAFF -> 1;
            case PLAYER -> 0;
        };
    }
}
