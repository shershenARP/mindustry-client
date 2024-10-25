package mindustry.ui.fragments;

import arc.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.actions.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.ctype.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.payloads.Constructor;
import mindustry.world.blocks.payloads.PayloadRouter;
import mindustry.world.blocks.payloads.PayloadSource;
import mindustry.world.blocks.units.UnitFactory;

import static mindustry.Vars.*;

/**
 * Displays the configuration UI for build plans before they have been placed.
 */
public class PlanConfigFragment {
    Table table = new Table();
    BuildPlan selected;

    public void build(Group parent) {
        table.visible = false;
        parent.addChild(table);

        Events.on(EventType.ResetEvent.class, e -> forceHide());
    }

    public void showConfig(BuildPlan plan) {
        if (this.selected == plan) {
            hide();
            return;
        }
        Block block = plan.block;
        if (!block.configurable) return;
        selected = plan;
        table.clear();

        //Only allows configuring content (items, liquids, units, blocks)
        //Each block manages its own configs, but that function requires an existing build
        //Try to guess it
        var options = new Seq<UnlockableContent>();
        if (block.configurations.containsKey(Item.class)) {
            options.add(content.items());
        }
        if (block.configurations.containsKey(Liquid.class)) {
            options.add(content.liquids());
        }
        if (block instanceof UnitFactory f) {
            options.add(f.plans.map(p -> p.unit).retainAll(u -> !u.isBanned()));
        } else if (block.configurations.containsKey(UnitType.class)) {
            options.add(content.units().retainAll(u -> !u.isBanned()));
        }
        if (block instanceof Constructor b) {
            options.add(content.blocks().select(b::canProduce));
        } else if (block instanceof PayloadSource b) {
            options.add(content.blocks().select(b::canProduce));
        } else if (block instanceof PayloadRouter b) {
            options.add(content.blocks().select(b::canSort));
        } else if (block.configurations.containsKey(Block.class)) {
            options.add(content.blocks());
        }
        if (options.isEmpty()) return;

        ItemSelection.buildTable(
            table, options,
            () -> selected != null ? (selected.config instanceof UnlockableContent c ? c : null) : null,
            content -> {
                selected.config = content;
                hide();
            },
            block.selectionRows, block.selectionColumns
        );
        table.pack();
        table.setTransform(true);
        table.visible = true;
        table.actions(Actions.scaleTo(0f, 1f), Actions.visible(true),
                Actions.scaleTo(1f, 1f, 0.07f, Interp.pow3Out));
        table.update(() -> {
            table.setOrigin(Align.center);
            if (plan.isDone() || !(control.input.selectPlans.contains(plan) || player.unit().plans.contains(plan))) {
                this.hide();
                return;
            }
            Vec2 pos = Core.input.mouseScreen(plan.drawx(), plan.drawy() - block.size * tilesize / 2.0F - 1);
            table.setPosition(pos.x, pos.y, Align.top);
        });
    }

    public void forceHide() {
        table.visible = false;
        selected = null;
    }

    public void hide() {
        selected = null;
        table.actions(Actions.scaleTo(0f, 1f, 0.06f, Interp.pow3Out), Actions.visible(false));
    }
}
