package mindustry.ui.fragments;

import mindustry.game.Team;
import mindustry.gen.Groups;

import java.util.ArrayList;

public class UnitPanelFragment {
    ArrayList<Team> unitTeams = new ArrayList<>();
    public UnitPanelFragment() {
        Groups.unit.each(unit -> {
            Team t = unit.team;
            if (!unitTeams.contains(t)) {
                unitTeams.add(t);
            }
        });
    }
}
