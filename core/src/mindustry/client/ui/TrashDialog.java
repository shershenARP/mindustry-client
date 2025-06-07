package mindustry.client.ui;

import arc.*;
import arc.graphics.*;
import arc.input.*;
import arc.math.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.client.ClientVars;
import mindustry.client.utils.AutoTransfer;
import mindustry.content.UnitTypes;
import mindustry.ctype.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.input.InputHandler;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.BaseDialog;

import static arc.Core.*;
import static mindustry.Vars.*;
import static mindustry.client.ClientVars.last_select_units_type;

public class TrashDialog extends BaseDialog {
    private TextField search;
    private final Table all = new Table();

    public static boolean showblockseffects = false;
    public static boolean showunitseffects = false;
    public static float iconunitsize = 48f;
    public static float timeoffsetseccontr = 0f;
    public static float timeoffsetseccommand = 0f;



    public TrashDialog(){
        super("@trashbase");

        shouldPause = true;
        addCloseButton();
        shown(this::rebuild);
        onResize(this::rebuild);

        all.margin(20).marginTop(0f);
        cont.pane(all).scrollX(false);
    }
    void rebuild(){
        all.clear();


        Seq<Content>[] allContent = Vars.content.getContentMap();

        for(int j = 0; j < allContent.length; j++){
            ContentType type = ContentType.all[j];

            //Seq<Content> array = allContent[j].select(c -> c instanceof UnitType u &&  (u.techNode != null));
            Seq<Content> array = allContent[j].select(c -> c instanceof UnitType u );
            if(array.size == 0) continue;


            all.table(sl -> {
                sl.add("@client.fdtrash.slidicon").padRight(8).left();
                var s = new Slider(4, 128, 4, false);
                s.setValue(iconunitsize);
                var l = new Label("", Styles.outlineLabel);
                var c = new Table().add(l).getTable();
                c.margin(3f);
                c.touchable = Touchable.disabled;
                s.changed(() -> {
                    l.setText(((int)s.getValue()) == 0 ? "@off" : Integer.toString((int)s.getValue()));
                    iconunitsize =  (int)s.getValue();
                });
                s.change();
                sl.stack(s, c).left().width(400);
            }).left();

            all.row();
            all.image().growX().pad(5).padLeft(0).padRight(0).height(3).color(Pal.accent);
            all.row();

            all.table(list -> {
                list.left();

                int cols = (int) Mathf.clamp((graphics.getWidth() - Scl.scl(30)) / Scl.scl(32 + 52), 1, 50);
                int count = 0;

                for(int i = 0; i < array.size; i++){

                    UnlockableContent unlock = (UnlockableContent)array.get(i);

                    Image image =  new Image(unlock.uiIcon).setScaling(Scaling.fit);

                    list.add(image).size(iconunitsize).pad(3);

                    ClickListener listener = new ClickListener();
                    image.addListener(listener);

                    image.addListener(new HandCursorListener());
                    image.update(() -> image.color.lerp(!listener.isOver() ? Color.lightGray : Color.white, Mathf.clamp(0.4f * Time.delta)));

                    image.clicked(() -> {
                        if(input.keyDown(KeyCode.shiftLeft) && Fonts.getUnicode(unlock.name) != 0){
                            Vars.ui.trashbase.hide();
                            UnitType lastunittype = cringeconvertertypes(unlock.name);
                            if(lastunittype != null) last_select_units_type = lastunittype;
                            InputHandler.selectUnitsType(lastunittype);

                        }else{
                            Vars.ui.trashbase.hide();
                            String message = "!uc " + unlock.localizedName;
                            ClientVars.clientCommandHandler.handleMessage(message, player);
                        }
                    });
                    image.addListener(new Tooltip(t -> t.background(Tex.button).add(unlock.localizedName + (settings.getBool("console") ? "\n[gray]" + unlock.name : ""))));

                    if((++count) % cols == 0){
                        list.row();
                    }
                }
            }).growX().left().padBottom(10);
        }
        all.row();
        all.add("@client.fdtrash.assistset").padRight(8).left();
        all.row();
        all.image().growX().pad(5).padLeft(0).padRight(0).height(3).color(Pal.accent);
        all.row();
        all.table(tas->{
            tas.check("@setting.circleassist.name", Core.settings.getBool("circleassist"), i -> Core.settings.put("circleassist", !Core.settings.getBool("circleassist"))).growX().fillY().padTop(4f).padBottom(4f).get();

            tas.check("@setting.assistbutnuance.name", Core.settings.getBool("assistbutnuance"), i -> Core.settings.put("assistbutnuance", !Core.settings.getBool("assistbutnuance"))).growX().fillY().padTop(4f).padBottom(4f).get();

            //tas.check("@setting.circleantigrief.name", Core.settings.getBool("circleantigrief"), i -> Core.settings.put("circleantigrief", !Core.settings.getBool("circleantigrief"))).growX().fillY().padTop(4f).padBottom(4f).get();

            tas.row();

            tas.add("@client.fdtrash.circleassistspeed").padRight(8).left();
            var s1 = new Slider(0, 100, 1, false);
            s1.setValue(floattoint(Core.settings.getFloat("circleassistspeed") * 100) );
            var l1 = new Label("%", Styles.outlineLabel);
            var c1 = new Table().add(l1).getTable();
            c1.margin(3f);
            c1.touchable = Touchable.disabled;
            s1.changed(() -> {
                l1.setText(((int)s1.getValue()) == 0 ? "@off" : Integer.toString((int)s1.getValue()));
                Core.settings.put("circleassistspeed", (int)s1.getValue() / 100f);
            });
            s1.change();
            tas.stack(s1, c1).width(800);
            tas.row();
            tas.add("@client.fdtrash.assistdistance").padRight(8).left();
            var s2 = new Slider(0, 100, 1, false);
            s2.setValue(floattoint(Core.settings.getFloat("assistdistance")));
            var l2 = new Label("", Styles.outlineLabel);
            var c2 = new Table().add(l2).getTable();
            c2.margin(3f);
            c2.touchable = Touchable.disabled;
            s2.changed(() -> {
                l2.setText(((int)s2.getValue()) == 0 ? "@off" : Integer.toString((int)s2.getValue()));
                Core.settings.put("assistdistance", (int)s2.getValue() * 1f);
            });
            s2.change();
            tas.stack(s2, c2).width(800);


        }).growX().left().padBottom(10);

        all.row();
        all.add("@client.fdtrash.sliddeath").padRight(8).left();
        all.row();
        all.image().growX().pad(5).padLeft(0).padRight(0).height(3).color(Pal.accent);
        all.row();
        all.table(t->{

            t.label(() -> "Last seconds for show units. 0 sec - Unlimited: ").left();
            t.row();

            t.add("@client.fdtrash.sliddeathpl").padRight(8).left();
            var s1 = new Slider(0, 3600, 10, false);
            s1.setValue(timeoffsetseccontr);
            var l1 = new Label("", Styles.outlineLabel);
            var c1 = new Table().add(l1).getTable();
            c1.margin(3f);
            c1.touchable = Touchable.disabled;
            s1.changed(() -> {
                l1.setText(((int)s1.getValue()) == 0 ? "@off" : Integer.toString((int)s1.getValue()));
                timeoffsetseccontr =  (int)s1.getValue();
            });
            s1.change();
            t.stack(s1, c1).width(800);
            t.row();
            t.add("@client.fdtrash.sliddeathpc").padRight(8).left();
            var s2 = new Slider(0, 3600, 10, false);
            s2.setValue(timeoffsetseccommand);
            var l2 = new Label("", Styles.outlineLabel);
            var c2 = new Table().add(l2).getTable();
            c2.margin(3f);
            c2.touchable = Touchable.disabled;
            s2.changed(() -> {
                l2.setText(((int)s2.getValue()) == 0 ? "@off" : Integer.toString((int)s2.getValue()));
                timeoffsetseccommand =  (int)s2.getValue();
            });
            s2.change();
            t.stack(s2, c2).width(800);

            t.row();
        }).left();
        all.row();
        all.table(tt->{
            tt.row();
            tt.check("@client.fdtrash.showunitseffects", showunitseffects, res -> showunitseffects = !showunitseffects).growX().fillY().padTop(4f).padBottom(4f).get().getLabelCell().growX();
            tt.row();
            tt.check("@setting.offrender.name",  Core.settings.getBool("offrender"), res -> Core.settings.put("offrender", res)).growX().fillY().padTop(4f).padBottom(4f).get().getLabelCell().growX();
            tt.row();
        }).left();
        all.row();
        all.add("@client.fdtrash.autotransfer").padRight(8).left();
        all.row();
        all.image().growX().pad(5).padLeft(0).padRight(0).height(3).color(Pal.accent);
        all.row();
        all.table(ttt->{
            ttt.row();
            ttt.add("@client.fdtrash.autotransferminres").padRight(8).left();
            var s1 = new Slider(0, 1000, 10, false);
            s1.setValue(AutoTransfer.Settings.getMinCoreItems());
            var l1 = new Label("", Styles.outlineLabel);
            var c1 = new Table().add(l1).getTable();
            c1.margin(3f);
            c1.touchable = Touchable.disabled;
            s1.changed(() -> {
                l1.setText(Integer.toString((int)s1.getValue()));
                AutoTransfer.setminres((int)s1.getValue());
            });
            s1.change();
            ttt.stack(s1, c1).width(800);
            ttt.row();

            ttt.add("@client.fdtrash.autotransferdelay").padRight(8).left();
            var s2 = new Slider(0, 240, 10, false);
            s2.setValue(AutoTransfer.Settings.getDelay());
            var l2 = new Label("", Styles.outlineLabel);
            var c2 = new Table().add(l2).getTable();
            c2.margin(3f);
            c2.touchable = Touchable.disabled;
            s2.changed(() -> {
                l2.setText(Float.toString((float)s2.getValue()));
                AutoTransfer.setdelay((float)s2.getValue());
            });
            s2.change();
            ttt.stack(s2, c2).width(800);
        }).left();
        all.row();

        all.check("@setting.onlyYFAT", Core.settings.getBool("onlyYFAT"), i -> Core.settings.put("onlyYFAT", !Core.settings.getBool("onlyYFAT"))).left();
        all.row();
        all.check("@setting.noHeAT", Core.settings.getBool("noHeAT"), i -> Core.settings.put("noHeAT", !Core.settings.getBool("noHeAT"))).left();
        all.row();
        all.check("@setting.noOvAT", Core.settings.getBool("noOvAT"), i -> Core.settings.put("noOvAT", !Core.settings.getBool("noOvAT"))).left();
        all.row();
        all.check("@setting.noShAT", Core.settings.getBool("noShAT"), i -> Core.settings.put("noShAT", !Core.settings.getBool("noShAT"))).left();
        all.row();



        all.row();
        all.add("@client.fdtrash.alarmtochat").padRight(8).left();
        all.row();
        all.image().growX().pad(5).padLeft(0).padRight(0).height(3).color(Pal.accent);
        all.row();
        all.table(ttt->{
            ttt.row();
            ttt.add("@client.fdtrash.cutalarmlength").padRight(8).left();
            var s2 = new Slider(0, 500, 1, false);
            s2.setValue(PanelFragment.meslen);
            var l2 = new Label("", Styles.outlineLabel);
            var c2 = new Table().add(l2).getTable();
            c2.margin(3f);
            c2.touchable = Touchable.disabled;
            s2.changed(() -> {
                l2.setText(((int)s2.getValue()) == 0 ? "@off" : Integer.toString((int)s2.getValue()));
                PanelFragment.meslen =  (int)s2.getValue();
            });
            s2.change();
            ttt.stack(s2, c2).width(800);
        }).left();
        all.row();
        all.table(ttt->{
            ttt.row();

            ttt.add("@client.fdtrash.unitdeathalarmhp").padRight(8).left();

            var s1 = new Slider(0, 24000, 10, false);
            s1.setValue(Core.settings.getInt("unitdeathalarmhp"));
            var l1 = new Label("", Styles.outlineLabel);
            var c1 = new Table().add(l1).getTable();
            c1.margin(3f);
            c1.touchable = Touchable.disabled;
            s1.changed(() -> {
                l1.setText(((int)s1.getValue()) == 0 ? "@off" : Integer.toString((int)s1.getValue()));
                Core.settings.put("unitdeathalarmhp", (int)s1.getValue());
            });
            s1.change();
            ttt.stack(s1, c1).width(800);
            ttt.row();

            ttt.add("@client.fdtrash.playerunitdeathalarmhp").padRight(8).left();
            var s2 = new Slider(0, 24000, 10, false);
            s2.setValue(Core.settings.getInt("playerunitdeathalarmhp"));
            var l2 = new Label("", Styles.outlineLabel);
            var c2 = new Table().add(l2).getTable();
            c2.margin(3f);
            c2.touchable = Touchable.disabled;
            s2.changed(() -> {
                l2.setText(((int)s2.getValue()) == 0 ? "@off" : Integer.toString((int)s2.getValue()));
                Core.settings.put("playerunitdeathalarmhp", (int)s2.getValue());
            });
            s2.change();
            ttt.stack(s2, c2).width(800);
        }).left();
        all.row();
        all.add("@client.fdtrash.light").padRight(8).left();
        all.row();
        all.image().growX().pad(5).padLeft(0).padRight(0).height(3).color(Pal.accent);
        all.row();
        all.table(tt->{
            tt.row();
            tt.check("@client.fdtrash.enableDarkness", enableDarkness, res ->  enableDarkness = !enableDarkness).growX().fillY().padTop(4f).padBottom(4f).get().getLabelCell().growX();
            tt.row();
            tt.check("@client.fdtrash.enableLight", enableLight, res ->  enableLight = !enableLight).growX().fillY().padTop(4f).padBottom(4f).get().getLabelCell().growX();
            tt.row();
            tt.check("@client.fdtrash.fog", state.rules.fog, res ->  state.rules.fog = !state.rules.fog).growX().fillY().padTop(4f).padBottom(4f).get().getLabelCell().growX();
            tt.row();
            //tt.check("@client.fdtrash.hidingWalls", ClientVars.hidingWalls, res ->  ClientVars.hidingWalls = !ClientVars.hidingWalls).growX().fillY().padTop(4f).padBottom(4f).get().getLabelCell().growX();
            //tt.row();

        }).left();

        if(all.getChildren().isEmpty()){
            all.add("@none.found");
        }
    }

    private UnitType cringeconvertertypes(String name) {
        if(name.contains("flare")) return UnitTypes.flare;
        if(name.contains("horizon")) return UnitTypes.horizon;
        if(name.contains("zenith")) return UnitTypes.zenith;
        if(name.contains("antumbra")) return UnitTypes.antumbra;
        if(name.contains("eclipse")) return UnitTypes.eclipse;

        if(name.contains("mono")) return UnitTypes.mono;
        if(name.contains("poly")) return UnitTypes.poly;
        if(name.contains("mega")) return UnitTypes.mega;
        if(name.contains("quad")) return UnitTypes.quad;
        if(name.contains("oct")) return UnitTypes.oct;

        if(name.contains("nova")) return UnitTypes.nova;
        if(name.contains("pulsar")) return UnitTypes.pulsar;
        if(name.contains("quasar")) return UnitTypes.quasar;
        if(name.contains("vela")) return UnitTypes.vela;
        if(name.contains("corvus")) return UnitTypes.corvus;

        if(name.contains("dagger")) return UnitTypes.dagger;
        if(name.contains("mace")) return UnitTypes.mace;
        if(name.contains("fortress")) return UnitTypes.fortress;
        if(name.contains("scepter")) return UnitTypes.scepter;
        if(name.contains("reign")) return UnitTypes.reign;

        if(name.contains("crawler")) return UnitTypes.crawler;
        if(name.contains("atrax")) return UnitTypes.atrax;
        if(name.contains("spiroct")) return UnitTypes.spiroct;
        if(name.contains("arkyid")) return UnitTypes.arkyid;
        if(name.contains("toxopid")) return UnitTypes.toxopid;

        if(name.contains("risso")) return UnitTypes.risso;
        if(name.contains("minke")) return UnitTypes.minke;
        if(name.contains("bryde")) return UnitTypes.bryde;
        if(name.contains("sei")) return UnitTypes.sei;
        if(name.contains("omura")) return UnitTypes.omura;

        if(name.contains("retusa")) return UnitTypes.retusa;
        if(name.contains("oxynoe")) return UnitTypes.oxynoe;
        if(name.contains("cyerce")) return UnitTypes.cyerce;
        if(name.contains("aegires")) return UnitTypes.aegires;
        if(name.contains("navanax")) return UnitTypes.navanax;

        if(name.contains("stell")) return UnitTypes.stell;
        if(name.contains("locus")) return UnitTypes.locus;
        if(name.contains("precept")) return UnitTypes.precept;
        if(name.contains("vanquish")) return UnitTypes.vanquish;
        if(name.contains("conquer")) return UnitTypes.conquer;

        if(name.contains("merui")) return UnitTypes.merui;
        if(name.contains("cleroi")) return UnitTypes.cleroi;
        if(name.contains("anthicus")) return UnitTypes.anthicus;
        if(name.contains("tecta")) return UnitTypes.tecta;
        if(name.contains("collaris")) return UnitTypes.collaris;

        if(name.contains("elude")) return UnitTypes.elude;
        if(name.contains("avert")) return UnitTypes.avert;
        if(name.contains("obviate")) return UnitTypes.obviate;
        if(name.contains("quell")) return UnitTypes.quell;
        if(name.contains("disrupt")) return UnitTypes.disrupt;
        return null;
    }

    private int floattoint(float num) {
        int intt = 0;
        num = Mathf.ceil(num);
        intt = (int) num;
        return intt;
    }
    public static boolean ihateattems(String code){
        String attemFD = "greaterThanEq attem 83";
        String attemminus = "op mul fx @thisx -10000";
        String attemreadflag = "read flag cell1 0";
        String attemT5 = "write fullness cell1 8\n" +
                "write c cell1 10\n" +
                "write min cell1 11";
        String attemT53 = "ubind unitType\n" +
                "sensor i @unit @totalItems\n" +
                "sensor ic @unit @itemCapacity\n" +
                "write ic cell1 12\n" +
                "sensor f @unit @flag";
        String attemT52 = "sensor silicon5 reconstructor1 @silicon\n" +
                "sensor surge5 reconstructor1 @surge-alloy\n" +
                "sensor phase5 reconstructor1 @phase-fabric\n" +
                "sensor plast5 reconstructor1 @plastanium\n" +
                "op add silicon5 silicon5 v2Silicon\n" +
                "op add surge5 surge5 v2Surge\n" +
                "op add plast5 plast5 v2Plastanium\n" +
                "op add phase5 phase5 v2Phase";
        String attemT54 = "set index1 12\n" +
                "set index2 0\n" +
                "read amount1 cell1 index1\n" +
                "read amount1 cell1 index1\n" +
                "op add amount1 amount1 amount\n" +
                "op add amount2 amount2 amount\n" +
                "write amount1 cell1 index1\n" +
                "write amount2 cell1 index2";
        //String attemT5 = "";
        if(code.contains(attemFD)) return true;
        if(code.contains(attemminus)) return true;
        if(code.contains(attemT5)) return true;
        if(code.contains(attemT52)) return true;
        if(code.contains(attemT53)) return true;
        if(code.contains(attemT54)) return true;
        if((Core.settings.getBool("attemmreadflag"))&& code.contains(attemreadflag)) return true;
        return false;
    }
}