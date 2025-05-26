package mindustry.client.ui;

import arc.Core;
import arc.Events;
import arc.util.Time;
import mindustry.game.EventType.WorldLoadEvent;
import arc.Events.*;

import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.logic.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.meta.*;
import arc.*;
import arc.func.*;

import static mindustry.Vars.*;

public class InteractTimerExtendedUI {

    public static float timer = 0;

    public static void load() {
        Events.on(WorldLoadEvent.class, e -> {
            timer = Time.time;
        });
    }

    public static void increase() {
        int delayMillis = Core.settings.getInt("eui-action-delay", 500);
        float delaySeconds = delayMillis / 1000f;
        timer = Time.time + Time.toSeconds * delaySeconds;
        timer += 0.01f; // предотвращение переполнения
    }

    // Проверка, можно ли взаимодействовать
    public static boolean canInteract() {
        return Time.time >= timer;
    }
}

