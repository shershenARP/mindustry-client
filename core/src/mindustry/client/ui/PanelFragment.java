package mindustry.client.ui;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Font;
import arc.graphics.g2d.GlyphLayout;
import arc.graphics.g2d.Lines;

import arc.math.Mathf;

import arc.scene.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.Timer;
import arc.util.pooling.Pools;
import mindustry.*;
import mindustry.client.navigation.BuildPath;
import mindustry.client.navigation.MinePath;
import mindustry.client.navigation.Navigation;
import mindustry.client.navigation.RepairPath;
import mindustry.client.utils.AutoTransfer;
import mindustry.content.*;
import mindustry.core.NetClient;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.input.InputHandler;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.ui.fragments.ChatFragment;
import mindustry.world.*;
import mindustry.world.blocks.ConstructBlock.*;
import mindustry.world.blocks.sandbox.ItemSource;
import mindustry.world.blocks.sandbox.LiquidSource;
import mindustry.world.blocks.sandbox.PowerSource;
import mindustry.world.blocks.sandbox.PowerVoid;
import mindustry.world.blocks.storage.CoreBlock;
import mindustry.world.blocks.units.Reconstructor;
import mindustry.world.blocks.units.UnitFactory;
import java.lang.Thread;

import static arc.Core.*;
import static mindustry.Vars.*;

public class PanelFragment extends InputHandler{
    public Table fdpanel; //Создание интерфейса дял кнопок
    public static Seq<Item> itemtomine = new Seq<>(); //Создание выборки для копания
    private static boolean minecopper, minelead, minetitan, minesand, minecoal, minescrap;
    private static boolean mineBerylliumwall, mineGraphiticwall;
    private float brokenFade = 0f;
    public static int meslen = 146;

    private boolean eneblemining = false;
    private boolean playerbuild = false;
    private boolean viewunitshealth = false;
    private boolean showunitseffects = false;
    private boolean viewprogressunit = false;
    private boolean viewprogresbuild  = false;

    public static boolean deserter  = false;
    public static boolean needhealmode  = false;
    public static int currentfollowmode = 0; // 1 - mine, 2 - build, 3 - heal
    public static int prevfollowmode = 0;

    public PanelFragment(){ //Основной класс

        Events.run(Trigger.draw, () -> { //Постоянный вызов прорисовки
            draw2();
            draw3();
            draw4();
            draw5();
            viewunitshealth=Core.settings.getBool("viewunitshealth");
            showunitseffects=Core.settings.getBool("showunitseffects");
            viewprogressunit=Core.settings.getBool("viewprogressunit");
            viewprogresbuild=Core.settings.getBool("viewprogresbuild");
        });


        Events.on(WorldLoadEvent.class, e -> { //Ивент, срабатывающий при загрузке карты
            rebuild();
            minecopper = true; minelead = true; minetitan = true;
            mineBerylliumwall = true; mineGraphiticwall = false;
            //minesand = false; minecoal = false;
            minescrap = false;
            itemtomine.clear();
            updatemineitems();
            //if(Core.settings.getBool("autortv")){ClientVars.wasaoutortv = false;}
        });
        Events.on(UnitControlEvent.class, e -> { //Проверка ресурсов при смене юнита
            updatemineitems();
        });
        Events.on(UnitChangeEvent.class, e -> { //Проверка ресурсов при смене юнита
            updatemineitems();
        });

        Events.run(Trigger.update, () -> { //currentfollowmode // 1 - mine, 2 - build, 3 - heal //переключения в режиме афк
            //Core.settings.getBool("mineafterheal")
            if(Core.settings.getBool("afkmode")){
                if(Navigation.currentlyFollowing == null){currentfollowmode = 1; startmining();}
                else if(Navigation.currentlyFollowing.toString().contains("BuildPath")){
                    if(player.unit().plans.size == 0 && control.input.isBuilding ){ currentfollowmode = 1; if(prevfollowmode == 1){startmining();}else{Navigation.follow(new RepairPath(), true); } }
                } else if(Navigation.currentlyFollowing.toString().contains("MinePath")){
                    if(player.unit().plans.size != 0 && control.input.isBuilding ) {prevfollowmode = 1; currentfollowmode = 1; Navigation.follow(new BuildPath("self")); } else return;
                }else if(Navigation.currentlyFollowing.toString().contains("RepairPath")){
                    if(player.unit().plans.size != 0 && control.input.isBuilding ) {prevfollowmode = 3; currentfollowmode = 3; Navigation.follow(new BuildPath("self")); } else return;
                }
            }
        });
        Events.on(ClientLoadEvent.class, e -> {
            if (Core.settings.getBool("ExtendedUI-auto-fill"))
                AutoFillExtendedUI.init();
        });
    }

    private void updatemineitems() { //Выбор руд
        if (minecopper){if(!itemtomine.contains(Items.copper)) itemtomine.add(Items.copper);} else {if(itemtomine.contains(Items.copper)){itemtomine.remove(Items.copper);}}
        if (minelead){if(!itemtomine.contains(Items.lead)) itemtomine.add(Items.lead);} else {if(itemtomine.contains(Items.lead)){itemtomine.remove(Items.lead);}}
        if (minesand){if(!itemtomine.contains(Items.sand)) itemtomine.add(Items.sand);} else {if(itemtomine.contains(Items.sand)){itemtomine.remove(Items.sand);}}
        if (minecoal){if(!itemtomine.contains(Items.coal)) itemtomine.add(Items.coal);} else {if(itemtomine.contains(Items.coal)){itemtomine.remove(Items.coal);}}
        if (minescrap){if(!itemtomine.contains(Items.scrap)) itemtomine.add(Items.scrap);} else {if(itemtomine.contains(Items.scrap)){itemtomine.remove(Items.scrap);}}
        if (minetitan){if(!itemtomine.contains(Items.titanium)) itemtomine.add(Items.titanium);} else {if(itemtomine.contains(Items.titanium)){itemtomine.remove(Items.titanium);}}
        if (mineBerylliumwall){if(!itemtomine.contains(Items.beryllium)) itemtomine.add(Items.beryllium);} else {if(itemtomine.contains(Items.beryllium)){itemtomine.remove(Items.beryllium);}}
        if (mineGraphiticwall){if(!itemtomine.contains(Items.graphite)) itemtomine.add(Items.graphite);} else {if(itemtomine.contains(Items.graphite)){itemtomine.remove(Items.graphite);}}

        if((player.unit().type == UnitTypes.evoke)||(player.unit().type == UnitTypes.incite)||(player.unit().type == UnitTypes.emanate)){
            if(itemtomine.contains(Items.copper)){itemtomine.remove(Items.copper);}
            if(itemtomine.contains(Items.lead)){itemtomine.remove(Items.lead);}
            if(itemtomine.contains(Items.sand)){itemtomine.remove(Items.sand);}
            if(itemtomine.contains(Items.coal)){itemtomine.remove(Items.coal);}
            if(itemtomine.contains(Items.scrap)){itemtomine.remove(Items.scrap);}
            if(itemtomine.contains(Items.titanium)){itemtomine.remove(Items.titanium);}
        }

    }

    private void draw2() { //Отрисовка прогресса юнитов в фабирках
        if (!viewprogressunit) return;

        for(Building bui : Groups.build){
            float prog = 0;
            if(bui instanceof UnitFactory.UnitFactoryBuild build){
                prog = build.fraction();
            }
            if(bui instanceof Reconstructor.ReconstructorBuild buildr){
                prog = buildr.fraction();
            }
            if(prog  > 0.0001 && viewprogressunit) {
                float hw = bui.block.size * 4;
                float yofset = hw + 2;
                Draw.z(Layer.darkness + 1);
                Draw.color(Pal.darkerGray);
                Lines.stroke(4);
                Lines.line(bui.x - hw, bui.y + yofset, bui.x - hw + hw * 2 * prog, bui.y + yofset);
                Draw.color(bui.team.color);
                Lines.stroke(2);
                Lines.line(bui.x - hw, bui.y + yofset, bui.x - hw + hw * 2 * prog, bui.y + yofset);
                String text = Mathf.floor(prog * 100) + "%";
                Font font = Fonts.outline;

                GlyphLayout lay = Pools.obtain(GlyphLayout.class, GlyphLayout::new);
                font.setUseIntegerPositions(false);
                font.getData().setScale(0.25f / Scl.scl(1.0f));
                lay.setText(font, text);
                font.setColor(Color.white);
                font.draw(text, bui.x - lay.width / 2, bui.y + yofset + lay.height / 2 + 6);
                font.getData().setScale(1);
                Pools.free(lay);
                Draw.reset();
            }
        }
    }

    private void draw3() { //Отрисовка прогресса постройки зданий
        if (!viewprogresbuild) return;
        for(Building bui : Groups.build) {
            float prog = 0;
            if(bui instanceof ConstructBuild entity){
                prog = ((ConstructBuild) bui).progress;
                if(prog > 0.0001 && prog < 1){
                    float hw = bui.block.size * 4;
                    Draw.z(Layer.darkness + 1);
                    String text = Mathf.floor(prog * 100) + "%";
                    Font font = Fonts.outline;

                    GlyphLayout lay = Pools.obtain(GlyphLayout.class, GlyphLayout::new);
                    font.setUseIntegerPositions(false);
                    font.getData().setScale(0.25f / Scl.scl(1.0f));
                    lay.setText(font, text);
                    font.setColor(Color.white);
                    font.draw(text, bui.x - lay.width / 2, bui.y + lay.height / 2);
                    font.getData().setScale(1);
                    Pools.free(lay);
                    Draw.reset();
                }

            }
        }
    }
    private void draw4() { //draw units health bar
        if (!viewunitshealth) return;
        brokenFade = Mathf.lerpDelta(brokenFade, 1f, 0.1f);
        for(Unit curunit : Groups.unit){
            if(curunit.health < curunit.maxHealth){
                float prog = curunit.health / curunit.maxHealth;
                if(prog > 0.0001 && prog < 1){
                    float hw = curunit.hitSize;
                    float yofset = hw + 2;
                    Draw.z(Layer.darkness + 1);
                    Color progcolor = new Color(255, 255, 255);

                    progcolor.lerp(Color.black, 1f - prog);
                    Draw.color(progcolor);
                    Lines.stroke(4);
                    Lines.line(curunit.x - hw, curunit.y + yofset, curunit.x - hw + hw * 2, curunit.y + yofset);
                    Draw.color(curunit.team.color);
                    Lines.stroke(2);
                    Lines.line(curunit.x - hw, curunit.y + yofset, curunit.x - hw + hw * 2 * prog, curunit.y + yofset);
                    String text = Mathf.floor(prog * 100) + "%";
                    Font font = Fonts.outline;

                    GlyphLayout lay = Pools.obtain(GlyphLayout.class, GlyphLayout::new);
                    font.setUseIntegerPositions(false);
                    font.getData().setScale(0.25f / Scl.scl(1.0f));
                    lay.setText(font, text);
                    font.setColor(Color.white);
                    font.draw(text, curunit.x - lay.width / 2, curunit.y + yofset + lay.height / 2 + 6);
                    font.getData().setScale(1);
                    Pools.free(lay);
                    Draw.reset();
                }
            }
        }
        Draw.reset();
    }
    private void draw5() { //draw units effects
        if (!showunitseffects) return;
        brokenFade = Mathf.lerpDelta(brokenFade, 1f, 0.1f);
        for(Unit stateunit : Groups.unit){
            Bits statuses = new Bits();
            Bits applied = stateunit.statusBits();
            if(!statuses.equals(applied)){
                if(applied != null){

                    Font font = Fonts.outline;
                    GlyphLayout layout = Pools.obtain(GlyphLayout.class, GlyphLayout::new);
                    font.setUseIntegerPositions(false);
                    font.getData().setScale(0.25F / Scl.scl(1.0F));

                    //font.setColor(Color.white);
                    font.getData().setScale(1);
                    Pools.free(layout);
                    Draw.alpha(0.90f * brokenFade); // Default is 0.33f
                    //Draw.mixcol(Color.red, 0.2f + Mathf.absin(Time.globalTime, 6f, 0.2f));
                    int i = 0;
                    String icons = "";
                    for(StatusEffect effect : content.statusEffects()){
                        if(applied.get(effect.id) && !effect.isHidden()){
                            icons = icons + effect.uiIcon + " ";
                            Draw.rect(effect.uiIcon, stateunit.x + i * effect.uiIcon.width / 4f, stateunit.y);
                            i = i + 1;
                            //t.image(effect.uiIcon).size(iconMed).get();
                        }
                    }
                    statuses.set(applied);
                }
            }
        }
        Draw.reset();
    }

    void rebuild(){
        //category does not change on rebuild anymore, only on new world load
        Group group = fdpanel.parent;
        int index = fdpanel.getZIndex();
        fdpanel.remove();
        build(group);
        fdpanel.setZIndex(index);
        //startmining();
    }

    public void build(Group parent){
        parent.fill(full -> {
            fdpanel = full;
            full.center().left().visible(() -> ui.hudfrag.shown);
            fdpanel.table(t -> {
                //t.setBackground(Styles.black3);
                ImageButton.ImageButtonStyle sstyle = Styles.clearNonei;
                ImageButton.ImageButtonStyle sstylet = Styles.clearNoneTogglei;
                t.defaults().size(Core.settings.getInt("buttonsizefdpamel") * 1f);
                t.label(() -> "HP:" + Mathf.floor(player.unit().health * 10) / 10f + "/" + Mathf.floor(player.unit().maxHealth * 10) / 10f).height(17f);
                t.row();
                t.label(() -> "Shield:" + Mathf.floor(player.unit().shield * 10) / 10f).height(17f);

                t.row();
                t.table(tb->{
                    tb.defaults().size(Core.settings.getInt("buttonsizefdpamel") / 2f);
                    tb.button(Icon.mapSmall, sstylet, () -> {
                        minesand = !minesand;
                        updatemineitems();
                    }).update(i -> i.setChecked(minesand)).name("minesand").tooltip("Mine sand");

                    tb.button(Icon.mapSmall, sstylet, () -> {
                        minecoal = !minecoal;
                        updatemineitems();
                    }).update(i -> i.setChecked(minecoal)).name("minecoal").tooltip("Mine coal");
                    tb.row();
                    tb.button(Icon.mapSmall, sstylet, () -> {
                        minelead = !minelead;
                        updatemineitems();
                    }).update(i -> i.setChecked(minelead)).name("minelead").tooltip("Mine lead");

                    tb.button(Icon.mapSmall, sstylet, () -> {
                        minecopper = !minecopper;
                        updatemineitems();
                    }).update(i -> i.setChecked(minecopper)).name("minecopper").tooltip("Mine copper");
                });

                t.table(tb->{
                    tb.defaults().size(Core.settings.getInt("buttonsizefdpamel") / 2f);

                    tb.button(Icon.mapSmall, sstylet, () -> {
                        minescrap = !minescrap;
                        updatemineitems();
                    }).update(i -> i.setChecked(minescrap)).name("minescrap").tooltip("Mine scrap");

                    tb.button(Icon.mapSmall, sstylet, () -> {
                        mineBerylliumwall = !mineBerylliumwall;
                        updatemineitems();
                    }).update(i -> i.setChecked(mineBerylliumwall)).name("Beryllium").tooltip("Mine Beryllium wall");

                    tb.row();

                    tb.button(Icon.mapSmall, sstylet, () -> {
                        minetitan = !minetitan;
                        updatemineitems();
                    }).update(i -> i.setChecked(minetitan)).name("minetitan").tooltip("Mine titan");

                    tb.button(Icon.mapSmall, sstylet, () -> {
                        mineGraphiticwall = !mineGraphiticwall;
                        updatemineitems();
                    }).update(i -> i.setChecked(mineGraphiticwall)).name("mineGraphiticwall").tooltip("Mine Graphitic wall");

                });

                t.button(Icon.distributionSmall, sstyle, () -> {
                    currentfollowmode = 3;
                    Navigation.follow(new RepairPath(), true);
                    //Navigation.currentlyFollowing;
                }).name("healer").tooltip("Heal");

                t.button(Icon.distributionSmall, sstyle, () -> {
                    currentfollowmode = 2;
                    Navigation.follow(new BuildPath("self"));
                }).name("builder").tooltip("Self builder");

                t.button(Icon.terminalSmall, sstyle, () -> {
                    eneblemining = !eneblemining;
                    startmining();
                }).name("miner").tooltip("Mine!");

                t.row();


                t.button(Icon.planetSmall, sstylet, () -> {
                    Core.settings.put("viewunitshealth", !Core.settings.getBool("viewunitshealth"));
                }).update(i -> i.setChecked(Core.settings.getBool("viewunitshealth"))).name("viewunitshealth").tooltip("Units health bar");

                t.button(Icon.effectSmall, sstylet, () -> {
                    Core.settings.put("showunitseffects", !Core.settings.getBool("showunitseffects"));
                }).update(i -> i.setChecked(Core.settings.getBool("showunitseffects"))).name("showunitseffects").tooltip("Units effects");

                t.button(Icon.unitsSmall, sstylet, () -> {
                    Core.settings.put("viewprogressunit", !Core.settings.getBool("viewprogressunit"));
                }).update(i -> i.setChecked(Core.settings.getBool("viewprogressunit"))).name("ubprogress").tooltip("Units build progress");


                t.button(Icon.craftingSmall, sstylet, () -> {
                    Core.settings.put("viewprogresbuild", !Core.settings.getBool("viewprogresbuild"));
                }).update(i -> i.setChecked(Core.settings.getBool("viewprogresbuild"))).name("bbprogress").tooltip("Buildings build progress");


                t.button(Icon.chatSmall, sstylet, () -> {
                    Core.settings.put("unitatchat", !Core.settings.getBool("unitatchat"));
                }).update(i -> i.setChecked(Core.settings.getBool("unitatchat"))).name("unitatchat").tooltip("unitatchat");

                t.row();

                t.button(Icon.eyeSmall, sstyle, this::checkunits).tooltip("Eye of Sauron: Units");

                t.button(Icon.eyeSmall, sstyle, this::checkcores).tooltip("Eye of Sauron: Cores");

                t.button(Icon.eyeSmall, sstyle, this::checkspawns).tooltip("Eye of Sauron: Spawns");

                t.button(Icon.eyeSmall, sstyle, this::checkvoids).tooltip("Eye of Sauron: Voids");

                t.button(Icon.eyeSmall, sstyle, this::checksources).tooltip("Eye of Sauron: Sources");

                t.row();

                t.button(Icon.diagonalSmall, sstylet, () -> {
                    if(!Core.settings.getBool("afkmode")){
                        eneblemining = true;
                        startmining();
                    } else {Navigation.stopFollowing();}
                    Core.settings.put("afkmode", !Core.settings.getBool("afkmode"));
                    new Toast(1).add(bundle.get("setting.afkmode.name") + ": " + bundle.get((settings.getBool("afkmode") ? "mod.enabled" : "mod.disabled")));
                }).update(i -> i.setChecked(Core.settings.getBool("afkmode"))).name("AFK").tooltip("AFK");

                t.button(Icon.starSmall, sstylet, () -> {
                    Core.settings.put("autotarget", !Core.settings.getBool("autotarget"));
                    new Toast(1).add(bundle.get("setting.autotarget.name") + ": " + bundle.get((settings.getBool("autotarget") ? "mod.enabled" : "mod.disabled")));
                }).update(i -> i.setChecked(Core.settings.getBool("autotarget"))).name("autotarget").tooltip("autotarget");

                /*t.button(Icon.cancelSmall, sstylet, () -> {
                    Core.settings.put("ignoreunit", !Core.settings.getBool("ignoreunit"));
                }).update(i -> i.setChecked(Core.settings.getBool("ignoreunit"))).name("ignoreunit").tooltip("ignoreunit");*/
                t.button(Icon.eyeSmall, sstyle, this::checkworldprocc).tooltip("Eye of Sauron: World Processor");

                //t.row();
                t.button(Icon.craftingSmall, sstylet, () -> {
                    AutoTransfer.enabled ^= true;
                    new Toast(1).add(bundle.get("client.autotransfer") + ": " + bundle.get(AutoTransfer.enabled ? "mod.enabled" : "mod.disabled"));
                    Core.settings.put("autotransfer", !Core.settings.getBool("autotransfer"));
                }).update(i -> i.setChecked(Core.settings.getBool("autotransfer"))).name("autotransfer").tooltip("autotransfer");

                t.button(Icon.filtersSmall, sstylet, () -> {
                    Core.settings.put("ExtendedUI-auto-fill", !Core.settings.getBool("ExtendedUI-auto-fill"));
                    AutoFillExtendedUI.init();
                }).update(i -> i.setChecked(Core.settings.getBool("ExtendedUI-auto-fill"))).name("ExtendedUI-auto-fill").tooltip("autofill Extended UI");

                t.row();

                t.button(Icon.trelloSmall, sstylet, () -> {
                    Core.settings.put("mobilegayming", !Core.settings.getBool("mobilegayming"));
                }).update(i -> i.setChecked(Core.settings.getBool("mobilegayming"))).name("mobilegayming").tooltip("mobilegayming");

                t.button(Icon.starSmall, sstylet, () -> {
                    Core.settings.put("snipermode", !Core.settings.getBool("snipermode"));
                }).update(i -> i.setChecked(Core.settings.getBool("snipermode"))).name("snipermode").tooltip("snipermode");

                t.button(Icon.commandRallySmall, sstylet, () -> {
                    Core.settings.put("deserter", !Core.settings.getBool("deserter"));
                }).update(i -> i.setChecked(Core.settings.getBool("deserter"))).name("deserter").tooltip("Deserter");

                t.button(Icon.eyeOffSmall, sstyle, () -> {
                    Vars.enableLight = !Vars.enableLight;
                }).name("light").tooltip("light");

                if(settings.getBool("canioffrender")) {
                    t.button(Icon.wrenchSmall, sstylet, () -> {
                        Core.settings.put("offrender", !Core.settings.getBool("offrender"));
                        Core.settings.put("targetfpsoff", !Core.settings.getBool("targetfpsoff"));
                    }).update(i -> i.setChecked(Core.settings.getBool("offrender"))).name("offrender").tooltip("offrender");
                }

            }).padTop(Core.settings.getBool("fdpanelenable") ? Core.settings.getInt("yoffssetfdpamel") * 1f : 5000f);
        });
    }

    private void checkspawns() {
        if(state.hasSpawns()){
            String uspawns = ": ";
            int num = 0;
            for(Tile tile: spawner.getSpawns()){
                uspawns = uspawns + "(" + Mathf.ceil(tile.x) + "," + Mathf.ceil(tile.y) + ");";
                num = num+1;
            }
            if(uspawns.length() > 2){
                if(settings.getBool("unitatchat")){
                    uspawns = "Spawns(" + num + ")" + uspawns;
                    if ((uspawns.length() >= meslen)&&(meslen != 0)) uspawns = uspawns.substring(0,meslen);
                    Call.sendChatMessage(uspawns);
                } else{
                    uspawns = "Spawns(" + num + ")" + uspawns;
                    if ((uspawns.length() >= meslen)&&(meslen != 0))  uspawns = uspawns.substring(0,meslen);
                    //player.sendMessage(uspawns);
                    ChatFragment.ChatMessage msg = ui.chatfrag.addMessage(uspawns, null, null, "", uspawns);
                    NetClient.findCoords(msg);
                }
            }
        }
    }

    public static void startmining() {
        if(player.team().data().core() == null) return;
        currentfollowmode = 1;
        Navigation.follow( new MinePath(itemtomine, player.team().data().core().storageCapacity), true);
    }

    private void checkvoids() {
        String ucont = ":" ;
        for(Tile tile : world.tiles) {
            if (tile.block() instanceof PowerVoid) {
                ucont = ucont + "(" + Mathf.ceil(tile.x) + "," + Mathf.ceil(tile.y) + ");";
            }
        }
        if(ucont.length() > 1) {
            if(Core.settings.getBool("unitatchat")){
                ucont = "[#fa]Power Voids:[white] " + ucont;
                if ((ucont.length() >= meslen)&&(meslen != 0)) ucont = ucont.substring(0,meslen);
                if(state.rules.pvp) { Call.sendChatMessage("/t " + ucont);} else { Call.sendChatMessage(ucont);}
            }else{
                ucont = "[#fa]Power Voids:[white] " + ucont;
                if ((ucont.length() >= meslen)&&(meslen != 0))  ucont = ucont.substring(0,meslen);
                //player.sendMessage(ucont);
                ChatFragment.ChatMessage msg = ui.chatfrag.addMessage(ucont, null, null, "", ucont);
                NetClient.findCoords(msg);
            }
        }

    }
    private void checksources() {
        for(Team cteam : Team.all) {
            String ucont = ":" ;
            for(Tile tile : world.tiles) {
                if(tile.build == null) continue;
                if((tile.build.block instanceof ItemSource its) && (tile.build.team == cteam)) ucont = ucont + Fonts.getUnicodeStr(its.name) + "(" + Mathf.ceil(tile.build.x/8) + "," + Mathf.ceil(tile.build.y/8) + ");";
                if((tile.build.block instanceof PowerSource its) && (tile.build.team == cteam)) ucont = ucont + Fonts.getUnicodeStr(its.name) + "(" + Mathf.ceil(tile.build.x/8) + "," + Mathf.ceil(tile.build.y/8) + ");";
                if((tile.build.block instanceof LiquidSource its) && (tile.build.team == cteam)) ucont = ucont + Fonts.getUnicodeStr(its.name) + "(" + Mathf.ceil(tile.build.x/8) + "," + Mathf.ceil(tile.build.y/8) + ");";
            }
            if(ucont.length() > 1) {
                if(Core.settings.getBool("unitatchat")){
                    ucont = "[#" + cteam.color + "]" + cteam.name + "[white]" + ucont;
                    if ((ucont.length() >= meslen)&&(meslen != 0))  ucont = ucont.substring(0,meslen);
                    if(state.rules.pvp) { Call.sendChatMessage("/t " + ucont);} else { Call.sendChatMessage(ucont);}
                }else{
                    ucont = "[#" + cteam.color + "]" + cteam.name + "[]" + ucont;
                    if ((ucont.length() >= meslen)&&(meslen != 0))  ucont = ucont.substring(0,meslen);
                    //player.sendMessage(ucont);
                    ChatFragment.ChatMessage msg = ui.chatfrag.addMessage(ucont, null, null, "", ucont);
                    NetClient.findCoords(msg);
                }
            }
        }
    }

    private void checkworldprocc() {
        for(Team cteam : Team.all) {
            String ucont = ":" ;
            for(Tile tile : world.tiles) {
                if(tile.build == null) continue;
                if((tile.build.block == Blocks.worldProcessor) && (tile.build.team == cteam))
                    ucont = ucont + Fonts.getUnicodeStr(Blocks.worldProcessor.name) + "(" + Mathf.ceil(tile.build.x/8) + ", " + Mathf.ceil(tile.build.y/8) + "); ";
            }
            if(ucont.length() > 1) {
                if(Core.settings.getBool("unitatchat")){
                    ucont = "[#" + cteam.color + "]" + cteam.name + "[white]" + ucont;
                    if ((ucont.length() >= meslen)&&(meslen != 0))  ucont = ucont.substring(0,meslen);
                    if(state.rules.pvp) { Call.sendChatMessage("/t " + ucont);} else { Call.sendChatMessage(ucont);}
                }else{
                    ucont = "[#" + cteam.color + "]" + cteam.name + "[]" + ucont;
                    if ((ucont.length() >= meslen)&&(meslen != 0))  ucont = ucont.substring(0,meslen);
                    //player.sendMessage(ucont);
                    ChatFragment.ChatMessage msg = ui.chatfrag.addMessage(ucont, null, null, "", ucont);
                    NetClient.findCoords(msg);
                }
            }
        }
    }

    private void checkcores() {
        for(Teams.TeamData team : state.teams.active) {
            String ucont = ":" ;
            int num = 0;
            for(CoreBlock.CoreBuild core : team.cores) {
                if(team.core() == null) continue;
                ucont = ucont + Fonts.getUnicodeStr(core.block.name) + "(" + Mathf.ceil(core.x/8) + ", " + Mathf.ceil(core.y/8) + "); ";
                num = num + 1;
            }
            if(ucont.length() > 1) {
                if(Core.settings.getBool("unitatchat")){
                    ucont = "[#" + team.team.color + "]" + team.team.name + "(" + num +")[white]" + ucont;
                    if ((ucont.length() >= meslen)&&(meslen != 0))  ucont = ucont.substring(0,meslen);
                    if(state.rules.pvp) { Call.sendChatMessage("/t " + ucont);} else { Call.sendChatMessage(ucont);}
                }else{
                    ucont = "[#" + team.team.color + "]" + team.team.name + "(" + num +")[]" + ucont;
                    if ((ucont.length() >= meslen)&&(meslen != 0))  ucont = ucont.substring(0,meslen);
                    //player.sendMessage(ucont);
                    ChatFragment.ChatMessage msg = ui.chatfrag.addMessage(ucont, null, null, "", ucont);
                    NetClient.findCoords(msg);
                }
            }
        }
    }
    private void checkunits() {

        for(Teams.TeamData team : state.teams.active) {
            String ucont = ":" ;
            for(UnitType type : content.units()) {
                if(team.typeCounts == null) continue;
                if(team.typeCounts[type.id] !=0) {
                    ucont = ucont + Fonts.getUnicodeStr(type.name) + team.typeCounts[type.id] + ";";
                }
            }
            if(ucont.length() > 1) {
                if(Core.settings.getBool("unitatchat")){
                    ucont = "[#" + team.team.color + "]" + team.team.name + "[white]" + ucont;
                    if ((ucont.length() >= meslen)&&(meslen != 0))  ucont = ucont.substring(0,meslen);
                    if(state.rules.pvp) { Call.sendChatMessage("/t " + ucont);} else {Call.sendChatMessage(ucont);}
                }else{
                    ucont = "[#" + team.team.color + "]" + team.team.name + "[]" + ucont;
                    if ((ucont.length() >= meslen)&&(meslen != 0))  ucont = ucont.substring(0,meslen);
                    ui.chatfrag.addMessage(ucont, null, null, "", ucont);
                    //player.sendMessage(ucont);
                }
            }
        }
    }
}