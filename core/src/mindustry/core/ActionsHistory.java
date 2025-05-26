package mindustry.core;

import arc.ApplicationListener;
import arc.struct.Queue;
import arc.struct.Seq;
import mindustry.game.Teams;
import mindustry.gen.Player;
import mindustry.gen.Unit;
import mindustry.type.Item;
import mindustry.type.UnitType;
import mindustry.world.Tile;

import java.time.LocalTime;

public class ActionsHistory extends Logic {
    public static Queue<BlockPlayerPlan> blocksplayersplans = new Queue<>();

    public static Queue<ItemPlayerPlan> playeritemsplans = new Queue<>();
    public static Queue<BlockConfigPlayerPlan> blockconfplayersplans = new Queue<>();
    public static Queue<UnitsKilledByPlayers> deathunitsplan = new Queue<>();
    public static Queue<UnitsKilledByControllPlayers> deathunitscontrolplan = new Queue<>();
    public static final Seq<Player> playeratmap = new Seq<>();

    public static void clearactionhistory() {
        blocksplayersplans.clear();
        blockconfplayersplans.clear();
        deathunitsplan.clear();
        deathunitscontrolplan.clear();
        playeratmap.clear();
    }


    //Blocks built/destroy by players
    public static class BlockPlayerPlan{
        public final short x, y, rotation, block;
        public final String lastacs;
        public final Object config;
        public boolean wasbreaking;

        public BlockPlayerPlan(int x, int y, short rotation, short block, Object config, String lastacs, boolean wasbreaking){
            this.x = (short)x;
            this.y = (short)y;
            this.rotation = rotation;
            this.block = block;
            this.config = config;
            this.lastacs = lastacs;
            this.wasbreaking = wasbreaking;
        }
    }

    //Blocks touched by players
    public static class BlockConfigPlayerPlan{
        public final short x, y, block;
        public final String lastacs;

        public BlockConfigPlayerPlan(int x, int y, short block, String lastacs){
            this.x = (short)x;
            this.y = (short)y;
            this.block = block;

            this.lastacs = lastacs;
        }
    }

    public static class UnitsKilledByPlayers{
        public Player kplayer;
        public Unit kunit;
        public final float x, y;
        public LocalTime localTime;

        public UnitsKilledByPlayers(Player kplayer, Unit kunit, float x, float y, LocalTime localTime){
            this.kplayer = kplayer;
            this.kunit = kunit;
            this.x = x;
            this.y = y;
            this.localTime = localTime;
        }
    }

    public static class UnitsKilledByControllPlayers{
        public String kplayer;
        public Unit kunit;
        public final float x, y;
        public LocalTime localTime;

        public UnitsKilledByControllPlayers(String kplayer, Unit kunit, float x, float y, LocalTime localTime){
            this.kplayer = kplayer;
            this.kunit = kunit;
            this.x = x;
            this.y = y;
            this.localTime = localTime;
        }
    }
    public static class UnitAtControl{
        public UnitType type;
        public float procX;
        public float procY;
        public int count;
        public UnitAtControl(UnitType type, float procX, float procY, int count){
            this.type = type;
            this.procX = procX;
            this.procY = procY;
            this.count = count;
        }
    }
    public static class ItemPlayerPlan{
        public Player player;
        public Tile tile;
        public Item item;
        public boolean take;
        public ItemPlayerPlan(Player player, Tile tile, Item item, boolean take){
            this.player = player;
            this.tile = tile;
            this.item = item;
            this.take = take;
        }
    }
}