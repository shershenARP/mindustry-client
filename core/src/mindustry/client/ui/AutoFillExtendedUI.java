package mindustry.client.ui;

import arc.Core;
import arc.struct.ObjectMap;
import arc.util.Nullable;
import mindustry.Vars;
import mindustry.content.Items;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Call;
import mindustry.gen.Player;
import mindustry.gen.Unit;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import mindustry.world.Build;
import mindustry.world.blocks.defense.turrets.ItemTurret;
import mindustry.world.blocks.storage.CoreBlock;
import mindustry.world.blocks.units.UnitFactory;
import mindustry.world.consumers.ConsumeItemDynamic;
import mindustry.world.consumers.ConsumeItemFilter;
import mindustry.world.consumers.ConsumeItems;

import mindustry.game.EventType.Trigger;
import arc.struct.Bits;
import arc.Events;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import mindustry.world.consumers.Consume;
import mindustry.world.modules.BlockModule;

import static mindustry.client.ui.InteractTimerExtendedUI.*;

public class AutoFillExtendedUI {
    public static void init() {
        Events.run(Trigger.update, () -> {
            if (!Core.settings.getBool("ExtendedUI-auto-fill", false) || !canInteract()) return;
            Player player = Vars.player;
            if (player.unit() == null) return;
            var stack = player.unit().stack;
            Team team = player.team();
            CoreBlock.CoreBuild core = player.closestCore();
            boolean isCoreAvailible = Core.settings.getBool("ExtendedUI-interact-core", false) && !core.isNull();

            Building[] request = new Building[1];
            Item[] requestItem = new Item[1];
            request[0] = null;
            int[] requestPriority = new int[]{-1};

            var config = Core.settings.getJson("ExtendedUI.autofill.priority", ObjectMap.class, () -> new ObjectMap());

            Vars.indexer.eachBlock(team, player.x, player.y, Vars.buildingRange, b -> true, b -> {
                if (!canInteract()) return;

                Block block = b.tile.block();

                boolean found = false;
                for (var c : block.consumers) {
                    if (c instanceof ConsumeItems || c instanceof ConsumeItemFilter || c instanceof ConsumeItemDynamic) {
                        found = true;
                        break;
                    }
                }
                if (!found) return;

                int blockPriority = (int) config.get(block.name, 0);

                if (blockPriority < requestPriority[0]) return;
                if (blockPriority == requestPriority[0] && request[0] != null) return;

                if (b.acceptStack(stack.item, stack.amount, player.unit()) >= 5) {
                    request[0] = b;
                    requestPriority[0] = blockPriority;
                    return;
                }

                if (blockPriority <= requestPriority[0]) return;

                Item newRequest = null;
                if (!isCoreAvailible) return;
                if (block instanceof ItemTurret) {
                    ItemTurret turret = (ItemTurret) block;
                    if (!b.<ItemTurret.ItemTurretBuild>as().ammo.isEmpty()) return;
                    newRequest = AutoFillExtendedUI.getBestAmmo(turret, core);
                } else if (block instanceof UnitFactory) {
                    UnitFactory.UnitFactoryBuild factoryBuild = (UnitFactory.UnitFactoryBuild) b;
                    UnitFactory factory = (UnitFactory) block;
                    newRequest = getUnitFactoryRequest(factoryBuild, factory, core);
                } else if (!b.items.empty()) {
                    newRequest = getItemRequest(b, block, core);
                }
                if (newRequest != null) {
                    request[0] = b;
                    requestItem[0] = newRequest;
                    requestPriority[0] = blockPriority;
                }
            });

            if (request[0] instanceof Building) {
                Call.transferInventory(player, request[0]);
                increase();
                return;
            }

            if (!isCoreAvailible || requestItem[0] != null || !player.within(core, Vars.buildingRange)) return;

            if (stack.amount != 0) {
                Call.transferInventory(player, core);
                if (stack.amount > 0) {
                    Call.dropItem(0);
                }
            } else {
                Call.requestItem(player, core, requestItem[0], 999);
            }
            increase();
        });
    }

    public static Item getBestAmmo(ItemTurret turret, CoreBlock.CoreBuild core) {
        final Item[] best = new Item[1];
        final float[] bestDamage = new float[1];
        turret.ammoTypes.each((item, ammo) ->{
           float totalDamage = ammo.damage + ammo.splashDamage;
           if (totalDamage > bestDamage[0] && core.items.get(item) >= 20) {
               best[0] = item;
               bestDamage[0] = totalDamage;
           }
        });
        return best[0];
    }

    public static Item getUnitFactoryRequest(UnitFactory.UnitFactoryBuild build, UnitFactory block, CoreBlock.CoreBuild core) {
        if (build.currentPlan == -1) return null;
        var stacks = block.plans.get(build.currentPlan).requirements;

        return findRequiredItem(stacks, build, core);
    }

    public static Item getItemRequest(Building build, Block block, CoreBlock.CoreBuild core) {
        Consume consumesItems = null;

        for (Consume c : block.consumers) {
            if (c instanceof ConsumeItems || c instanceof ConsumeItemFilter || c instanceof ConsumeItemDynamic) {
                consumesItems = c;
                break;
            }
        }

        if (consumesItems == null) return null;

        if (consumesItems instanceof ConsumeItemFilter) {
            return getFilterRequest((ConsumeItemFilter) consumesItems, build, core);
        } else if (consumesItems instanceof ConsumeItems) {
            return findRequiredItem(((ConsumeItems) consumesItems).items, build, core);
        } else {
            return null;
        }
    }

    public static Item getFilterRequest(ConsumeItemFilter filter, Building build, CoreBlock.CoreBuild core) {
        final Item[] request = new Item[1]; // для "mutable" результата внутри лямбды
        request[0] = null;

        final boolean[] stop = new boolean[1];
        stop[0] = false;

        Vars.content.items().each(item -> {
            if (filter.filter.get(item) && item != Items.blastCompound && core.items.get(item) >= 20) {
                if (build.acceptStack(item, 20, Vars.player.unit()) >= 5 && request[0] == null && !stop[0]) {
                    request[0] = item;
                } else {
                    stop[0] = true;
                }
            }
        });

        return request[0];
    }


    public static Item findRequiredItem(ItemStack[] stacks, UnitFactory.UnitFactoryBuild build, CoreBlock.CoreBuild core) {
        for (var itemStack : stacks) {
            Item item = itemStack.item;
            if (core.items.get(item) >= 20 && build.acceptStack(item, 20, Vars.player.unit()) >= 5) {
                return item;
            }
        }
        return null;
    }

    public static Item findRequiredItem(ItemStack[] stacks, Building build, CoreBlock.CoreBuild core) {
        for (var itemStack : stacks) {
            Item item = itemStack.item;
            if (core.items.get(item) >= 20 && build.acceptStack(item, 20, Vars.player.unit()) >= 5) {
                return item;
            }
        }
        return null;
    }
}

