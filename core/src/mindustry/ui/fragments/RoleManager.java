package mindustry.ui.fragments;

import mindustry.gen.Player;
import arc.struct.ObjectMap;
import arc.graphics.Color;

public class RoleManager {
    private static final ObjectMap<Player, String> roles = new ObjectMap<>();

    public static void setRole(Player player, String role) {
        roles.put(player, role);
    }

    public static String getRole(Player player) {
        return roles.get(player, "No Role");
    }

    public static Color roleColorFor(String role) {
        switch(role) {
            case "Core": return Color.yellow;
            case "Defense": return Color.orange;
            default: return Color.gray;
        }
    }
}
