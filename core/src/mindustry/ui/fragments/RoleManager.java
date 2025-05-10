public class RoleManager {
    private static final ObjectMap<Player, String> roles = new ObjectMap<>();

    public static void setRole(Player player, String role) {
        roles.put(player, role);
    }

    public static String getRole(Player player) {
        return roles.get(player, "No Role");
    }
}
