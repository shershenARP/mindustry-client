package mindustry.ui.fragments;

import arc.Core;
import arc.Events;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.input.KeyCode;
import arc.scene.Group;
import arc.scene.event.Touchable;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.Image;
import arc.scene.ui.ImageButton.ImageButtonStyle;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Scl;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.*;
import mindustry.Vars;
import mindustry.client.ClientVars;
import mindustry.client.Spectate;
import mindustry.client.antigrief.Moderation;
import mindustry.client.navigation.AssistPath;
import mindustry.client.navigation.Navigation;
import mindustry.client.navigation.UnAssistPath;
import mindustry.client.utils.ClientUtils;
import mindustry.client.utils.Server;
import mindustry.content.StatusEffects;
import mindustry.content.UnitTypes;
import mindustry.core.ActionsHistory;
import mindustry.entities.units.BuildPlan;
import mindustry.game.EventType;
import mindustry.game.Teams;
import mindustry.gen.*;
import mindustry.graphics.Pal;
import mindustry.net.NetConnection;
import mindustry.net.Packets.AdminAction;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;

import java.util.Iterator;

import static mindustry.Vars.*;
import static mindustry.client.ClientVars.assistuser;
import static mindustry.client.ClientVars.nameforplans;

public class PlayerBlockListFragment {
    public Table content = new Table().marginRight(13f).marginLeft(13f);
    private boolean visible = false;
    private final Interval timer = new Interval();
    private TextField search;
    private Seq<Player> searchplayers = new Seq<>();


    public void build(Group parent){
        content.name = "players";

        parent.fill(cont -> {
            cont.name = "playerblocklist";
            cont.visible(() -> visible);
            cont.update(() -> {
                if(!state.isGame()){
                    visible = false;
                    return;
                }

                if(visible && timer.get(60) /*&& !Core.input.keyDown(KeyCode.mouseLeft) && !(Core.scene.hit(Core.input.mouseX(), Core.input.mouseY(), true) instanceof Image || Core.scene.hit(Core.input.mouseX(), Core.input.mouseY(), true) instanceof ImageButton)*/){
                    rebuild();
                }
            });

            cont.table(Tex.buttonTrans, pane -> {
                pane.label(() -> Core.bundle.format("players" + (ActionsHistory.playeratmap.size == 1 && (ui.join.lastHost == null || ui.join.lastHost.playerLimit <= 0) ? ".single" : ""), ActionsHistory.playeratmap.size + " (" + Groups.player.count(p -> p.fooUser || p.isLocal()) + Iconc.wrench + ") " + (ui.join.lastHost != null && ui.join.lastHost.playerLimit > 0 ? " / " + ui.join.lastHost.playerLimit : "")));
                pane.row();

                search = pane.field(null, text -> rebuild()).grow().pad(8).name("search").maxTextLength(maxNameLength).get();
                search.setMessageText(Core.bundle.get("players.search"));

                if(Core.settings.getBool("blocksplayersplan")){
                    pane.row();
                    pane.table(info -> {
                        info.button("[#00ff]-" , () -> {
                            nameforplans = search.getText();
                        }).maxHeight(25).minWidth(80);
                        info.button("[#0000ff]-" , () -> {
                            nameforplans = search.getText();
                        }).maxHeight(25).minWidth(80);
                        info.button("[#ff]-"  , () -> {
                            nameforplans = null;
                        }).maxHeight(25).minWidth(80);
                        info.button("[#cccccc]fix", () -> {
                            Iterator<ActionsHistory.BlockPlayerPlan> broken = ActionsHistory.blocksplayersplans.iterator();
                            while(broken.hasNext()){
                                ActionsHistory.BlockPlayerPlan plan = broken.next();
                                if(search.getText().equals(plan.lastacs) && plan.wasbreaking) {
                                    player.unit().addBuild(new BuildPlan(plan.x, plan.y, plan.rotation, Vars.content.block(plan.block), plan.config));
                                }
                            }
                        }).maxHeight(25).minWidth(50);
                    });
                }
                if (Core.settings.getBool("modenabled")) {
                    pane.button("a.l.", ()->{
                        String message = "!admin leaves";
                        ClientVars.clientCommandHandler.handleMessage(message, player);
                    }).height(25).minWidth(50);
                }
                pane.row();
                pane.pane(content).grow().scrollX(false);
                pane.row();

                pane.table(menu -> {
                    menu.defaults().pad(5).growX().height(50f).fillY();
                    menu.name = "menu";

                    menu.button("@server.bans", ui.bans::show).disabled(b -> net.client()).get().getLabel().setWrap(false);
                    menu.button("@server.admins", ui.admins::show).disabled(b -> net.client()).get().getLabel().setWrap(false);
                    menu.button("@close", this::toggle).get().getLabel().setWrap(false);
                }).margin(0f).pad(10f).growX();

            }).touchable(Touchable.enabled).margin(14f).minWidth(400f);
        });

        rebuild();
        Events.on(EventType.PlayerJoin.class, event -> {
            readd(event.player);
        });
    }

    public void readd(Player p){
        if (!ActionsHistory.playeratmap.contains(p)) {
            //if(player.admin) {if(Core.settings.getBool("instantrace")) {Call.adminRequest(p, AdminAction.trace);}}
            ActionsHistory.playeratmap.add(p);
        }
    }

    public void rebuild(){
        content.clear();

        float h = 74f;
        boolean found = false;

        searchplayers.clear();
        searchplayers = ActionsHistory.playeratmap.copy();
        var target = Spectate.INSTANCE.getPos() instanceof Player p ? p :
                Navigation.currentlyFollowing instanceof AssistPath p && p.getAssisting() != null ? p.getAssisting() :
                        Navigation.currentlyFollowing instanceof UnAssistPath p ? p.target :
                                null;
        ActionsHistory.playeratmap.sort(Structs.comps(Structs.comparingBool(p -> p != target), Structs.comps(Structs.comparing(Player::team), Structs.comps(Structs.comparingBool(p -> !p.admin), Structs.comparingBool(p -> !(p.fooUser || p.isLocal()))))));
        if(search.getText().length() > 0) searchplayers.filter(p -> Strings.stripColors(p.name().toLowerCase()).contains(search.getText().toLowerCase()));

        for(var user : searchplayers){
            found = true;
            NetConnection connection = user.con;

            if(connection == null && net.server() && !user.isLocal()) return;

            Table button = new Table();
            button.left();
            button.margin(5).marginBottom(10);

            Table table = new Table(){
                @Override
                public void draw(){
                    super.draw();
                    Draw.color(Pal.gray);
                    Draw.alpha(parentAlpha);
                    Lines.stroke(Scl.scl(4f));
                    Lines.rect(x, y, width, height);
                    Draw.reset();
                }
            };
            table.margin(8);
            table.add(new Image(user.icon()).setScaling(Scaling.bounded)).grow();
            table.name = user.name();

            button.add(table).size(h);
            button.button( // This is by far the worst line of code I have ever written, its split so its not 500+ chars but still jesus
                    Core.input.shift() ? String.valueOf(user.id) :
                            Core.input.ctrl() ? "Groups.player.getByID(" + user.id + ")" :
                                    "[#" + user.color().toString().toUpperCase() + "]" + user.name() + (Core.settings.getBool("showuserid") ? " [accent](#" + user.id + ")" : ""),
                    Styles.nonetdef, () ->
                            Core.app.setClipboardText(Core.input.shift() ? String.valueOf(user.id) :
                                    Core.input.ctrl() ? "Groups.player.getByID(" + user.id + ")" :
                                            //Strings.stripColors(user.name))
                                            user.name)
            ).wrap().width(400).growY().pad(10);

            if (user.admin && !(!user.isLocal() && net.server())) button.image(Icon.admin).padRight(7.5f);
            if (user.fooUser || (user.isLocal() && Core.settings.getBool("displayasuser"))) button.image(Icon.wrench).padRight(7.5f).tooltip("@client.clientuser");


            var style = new ImageButtonStyle(){{
                down = Styles.none;
                up = Styles.none;
                imageCheckedColor = Pal.accent;
                imageDownColor = Pal.accent;
                imageUpColor = Color.white;
                imageOverColor = Color.lightGray;
            }};

            var ustyle = new ImageButtonStyle(){{
                down = Styles.none;
                up = Styles.none;
                imageDownColor = Pal.accent;
                imageUpColor = Color.white;
                imageOverColor = Color.lightGray;
            }};
            if(player.admin){
                button.add().growY();

                float bs = (h) / 2f;

                button.table(t -> {
                    t.defaults().size(bs);
                    t.button(Icon.admin, ustyle, () -> Call.adminRequest(user, AdminAction.trace, null));

                }).padRight(12).size(bs + 10f, bs);
            }

            if (user != player) {
                if(Core.settings.getBool("blocksplayersplan")){
                    content.table(info -> {
                        content.button("[#00ff] " + BlockBuDe(user, false) , () -> {
                            nameforplans = DeletePrefix(user.name);
                        }).maxHeight(25).minWidth(80);
                    });

                    content.button("[#0000ff]" +  BlockCon(user, true), () -> {
                        nameforplans = DeletePrefix(user.name);
                    }).maxHeight(25).minWidth(80);

                    content.button("[#ff] " +  + BlockBuDe(user, true) , () -> {
                        nameforplans = null;
                    }).maxHeight(25).minWidth(80);

                    content.button("[#cccccc]" + "fix", () -> {
                        Iterator<ActionsHistory.BlockPlayerPlan> broken = ActionsHistory.blocksplayersplans.iterator();
                        while(broken.hasNext()){
                            ActionsHistory.BlockPlayerPlan plan = broken.next();
                            if(user.name.equals(plan.lastacs) && plan.wasbreaking) {
                                player.unit().addBuild(new BuildPlan(plan.x, plan.y, plan.rotation, Vars.content.block(plan.block), plan.config));
                            }
                        }
                    }).maxHeight(25).minWidth(50);
                }

                button.button(Icon.zoom, ustyle, // Spectate/stalk
                        () -> Spectate.INSTANCE.spectate(user, Core.input.shift())).tooltip("@client.spectate");
            }

            content.add(button).padBottom(-6).width(750).maxHeight(h + 14);
            content.row();
            content.image().height(4f).color(state.rules.pvp ? user.team().color : Pal.gray).growX();
            content.row();
        }

        if(!found){
            content.add(Core.bundle.format("players.notfound")).padBottom(6).width(600f).maxHeight(h + 14);
        }

        content.marginBottom(5);
    }

    public static String DeletePrefix(String name) {
        int len = name.length();
        String loxb = "";
        for (int i = 0; i < len; i++) {
            if(i > 26) loxb = loxb + name.charAt(i);
        }
        return name;
    }
    public static int BlockBuDe(Player p, boolean bre){
        int blbu = 0, blbr = 0;
        for(ActionsHistory.BlockPlayerPlan block : ActionsHistory.blocksplayersplans) {
            //Block b = Vars.content.block(block.block);
            if(p.name.equals(block.lastacs)){
                if(block.wasbreaking){ blbr = blbr + 1; }
                else { blbu = blbu + 1;}
            }
        }
        if (bre) { return blbr; } else return blbu;
    }

    public static int BlockCon(Player p, boolean conf){
        int confb = 0;
        for(ActionsHistory.BlockConfigPlayerPlan block : ActionsHistory.blockconfplayersplans) {
            if(p.name.equals(block.lastacs)){
                confb = confb + 1;
            }
        }
        return confb;
    }


    public void toggle(){
        visible = !visible;
        if(visible){
            Core.scene.setKeyboardFocus(search);
            rebuild();
        }else{
            Core.scene.setKeyboardFocus(null);
            search.clearText();
        }
    }

    public boolean shown(){
        return visible;
    }

}