package mindustry.ui.fragments;

import mindustry.gen.Player;
import arc.struct.ObjectMap;
import arc.graphics.Color;
import mindustry.graphics.Pal;

public class RoleManager {
    private static final ObjectMap<Player, String> roles = new ObjectMap<>();

    public static void setRole(Player player, String role) {
        if (role == "Core") {
            role = "\uE853";
            roles.put(player, role);
        };

        if (role == "Defense") {
            role = "\uE84D";
            roles.put(player, role);
        };
    }

    public static String getRole(Player player) {
        return roles.get(player, "404");
    }

    public static Color roleColorFor(String role) {
        switch(role) {
            case "\uE853": return Pal.stat;
            case "\uE84D": return Pal.unitBack;
            default: return Color.gray;
        }
    }
}
