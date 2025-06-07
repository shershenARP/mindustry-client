package mindustry.ui.fragments;

import mindustry.gen.Player;
import arc.struct.ObjectMap;
import arc.graphics.Color;
import mindustry.graphics.Pal;

public class RoleManager {
    private static final ObjectMap<Player, String> roles = new ObjectMap<>();

    public static void setRole(Player player, String role) {
        if (role == "Core") {
            role = "\uE853 Cores";
            roles.put(player, role);
        };

        if (role == "Defense") {
            role = "\uE84D Defender";
            roles.put(player, role);
        };

        if (role == "Attack") {
            role = "\uE86D Attacker" ;
            roles.put(player, role);
        }

        if (role == "Logist") {
            role = "\uE83E Logist";
            roles.put(player, role);
        }
    }

    public static String getRole(Player player) {
        return roles.get(player, "404 Unknown");
    }

    public static Color roleColorFor(String role) {
        switch(role) {
            case "\uE853 Cores": return Pal.stat;
            case "\uE84D Defender": return Pal.unitBack;
            case "\uE86D Attacker": return Pal.unitFront;
            case "\uE83E Logist": return Pal.ammo;
            default: return Color.gray;
        }
    }
}
