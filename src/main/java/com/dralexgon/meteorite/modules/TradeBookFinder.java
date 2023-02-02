package com.dralexgon.meteorite.modules;

import com.dralexgon.meteorite.Meteorite;
import meteordevelopment.meteorclient.events.entity.player.InteractBlockEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import net.minecraft.network.packet.s2c.play.SetTradeOffersS2CPacket;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class TradeBookFinder extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<String>> enchant = sgGeneral.add(new StringListSetting.Builder()
        .name("enchant")
        .description("The exact name of the enchant you want to find.")
        .defaultValue(List.of("mending", "blast_protection"))
        .build()
    );

    private final Setting<Boolean> onlyMaxLevel = sgGeneral.add(new BoolSetting.Builder()
        .name("only-max-level")
        .description("Only find books with the max level of the enchant.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> maxEmeraldPrice = sgGeneral.add(new IntSetting.Builder()
        .name("max-emerald-price")
        .description("The max emerald price of the book.")
        .defaultValue(20)
        .min(1)
        .sliderRange(1, 64)
        .build());

    public TradeBookFinder() {
        super(Meteorite.CATEGORY, "Trade Book Finder", "Place and break lecterns to find a book with a specific enchant");
    }

    public static State state;
    public static long waitingTimestamp;
    public static int nbLecterns;
    public static BlockPos lecternPos;
    public static String lastEnchantedBook;


    public static void littleReset() {
        waitingTimestamp = 0;
        nbLecterns = 0;
    }

    public static void reset() {
        state = State.READY;
        waitingTimestamp = 0;
        nbLecterns = 0;
        lecternPos = null;
    }



    @Override
    public void onActivate() {
        if (countLectern() == 0) {
            error("§4You need at least one lectern in your inventory");
            mc.player.sendMessage(Text.of("§4You need at least one lectern in your inventory"), true);
            toggle();
            return;
        }
        if (!mc.player.getOffHandStack().getItem().equals(Items.LECTERN)) {
            error("§4You need to put a lectern in your offhand");
            toggle();
            return;
            /*
            int lecternSlot = -1;
            for (int i = 0; i < mc.player.getInventory().size(); i++) {
                if (mc.player.getInventory().getStack(i).getItem().equals(Items.LECTERN)) {
                    lecternSlot = i;
                    break;
                }
            }
            mc.player.getInventory().swapSlotWithHotbar(lecternSlot);
            mc.player.swingHand(Hand.OFF_HAND);*/
        }
        reset();
        ChatUtils.info("§2Right click a lectern to start the research");
    }

    @Override
    public void onDeactivate() {
    }

    public int countLectern() {
        PlayerEntity player = mc.player;
        int nbOfLecterns = 0;
        for (int i = 0; i < player.getInventory().size(); i++) {
            if (player.getInventory().getStack(i).getItem().equals(Items.LECTERN)) {
                nbOfLecterns += player.getInventory().getStack(i).getCount();
            }
        }
        return nbOfLecterns;
    }

    public void breakLectern() {
        nbLecterns = countLectern();
        Packet packetStartDestroyBlock = new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, lecternPos, Direction.UP);
        mc.getNetworkHandler().sendPacket(packetStartDestroyBlock);
        Packet packetStopDestroyBlock = new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, lecternPos, Direction.UP);
        mc.getNetworkHandler().sendPacket(packetStopDestroyBlock);
        state = State.MINING;
    }

    public void placeLectern() {//TODO use packet instead
        BlockPos lp = lecternPos;
        BlockHitResult hitResult = new BlockHitResult(new Vec3d(lp.getX(), lp.getY(), lp.getZ()), Direction.UP, lp, false);
        mc.interactionManager.interactBlock(
            mc.player, Hand.OFF_HAND, hitResult);
        state = State.SEARCHING_VILLAGER;
    }

    @EventHandler
    public void onInteract(InteractBlockEvent event) {
        BlockHitResult hitResult = event.result;
        Block block = mc.world.getBlockState(hitResult.getBlockPos()).getBlock();
        if (state == State.READY) {
            if (block.equals(Blocks.LECTERN)) {
                lecternPos = hitResult.getBlockPos();
                mc.player.sendMessage(Text.of("§2Lectern register"), true);
                breakLectern();
            }
        }
    }

    @EventHandler
    public void onPacketS2C(PacketEvent.Receive event) {
        Packet packet = event.packet;

        if (packet instanceof OpenScreenS2CPacket) {
            if (!(state == State.WAITING_FOR_TRADE_OFFER)) return;
            OpenScreenS2CPacket openScreenS2CPacket = (OpenScreenS2CPacket) packet;
            if (openScreenS2CPacket.getScreenHandlerType() == ScreenHandlerType.MERCHANT) event.cancel();
        }
        if (packet instanceof SetTradeOffersS2CPacket) {
            if (!(state == State.WAITING_FOR_TRADE_OFFER)) return;
            lastEnchantedBook = null;
            SetTradeOffersS2CPacket setTradeOffersS2CPacket = (SetTradeOffersS2CPacket) packet;
            setTradeOffersS2CPacket.getOffers().forEach(tradeOffer -> {
                ItemStack item = tradeOffer.getSellItem();
                if (item.hasNbt()) {
                    item.getNbt().getKeys().forEach(key -> {
                        if (key.equals("StoredEnchantments")) {
                            String id = item.getNbt().get("StoredEnchantments").asString().split("\"")[1].split(":")[1];
                            String lvl = item.getNbt().get("StoredEnchantments").asString().split("lvl:")[1].split("s}")[0];
                            int price = tradeOffer.getAdjustedFirstBuyItem().getCount();
                            boolean isValid = enchant.get().contains(id) && (!onlyMaxLevel.get() || getMaxEnchant(id) == Integer.parseInt(lvl) && price <= maxEmeraldPrice.get());
                            Text text = Text.of("§" + (isValid?"2":"4") + "Found " + id + " " + lvl + " book for " + price + " emeralds" + (isValid?"!":"."));
                            mc.player.sendMessage(text);
                            if (isValid) {
                                toggle();
                                state = State.FOUND_ENCHANTED_BOOK;
                                mc.player.sendMessage(text, true);
                                mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0F));
                            } else {
                                lastEnchantedBook = lvl;
                            }
                        }
                    });
                }
            });
            if (state == State.FOUND_ENCHANTED_BOOK) return;
            if (lastEnchantedBook == null) mc.player.sendMessage(Text.of("§4No enchanted book this trade."), true);
            event.cancel();
            littleReset();
            breakLectern();
            //The loop of breaking lectern is closed
        }
    }

    public void searchingVillager() {
        if (mc.world == null) {
            toggle();
            return;
        }

        double minDistance = Double.MAX_VALUE;
        Entity closestVillager = null;

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof VillagerEntity) {
                Vec3d pos = entity.getPos();
                if ((pos.distanceTo(mc.player.getPos()) < minDistance)) {
                    minDistance = pos.distanceTo(mc.player.getPos());
                    closestVillager = entity;
                }
            }
        }
        if (closestVillager != null) {
            mc.interactionManager.interactEntity(mc.player, closestVillager, Hand.OFF_HAND);
            state = State.WAITING_FOR_TRADE_OFFER;
        }
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        Meteorite.LOG.info("tick");
        if (mc == null || mc.world == null || mc.player == null) {
            return;
        }
        if (state == State.WAITING_FOR_TRADE_OFFER) {
            if (System.currentTimeMillis() - waitingTimestamp > 3000) {
                state = State.SEARCHING_VILLAGER;
            }
        }
        if (state == State.SEARCHING_VILLAGER) {
            searchingVillager();
        }else if (state == State.MINING) {
            if (countLectern() == nbLecterns + 1) {
                placeLectern();
            }
        }
    }

    public enum State {
        READY,
        MINING,
        SEARCHING_VILLAGER,
        WAITING_FOR_TRADE_OFFER,
        FOUND_ENCHANTED_BOOK
    }

    public int getMaxEnchant(String enchant) {
        switch (enchant) {
            case "protection":
                return 4;
            case "fire_protection":
                return 4;
            case "feather_falling":
                return 4;
            case "blast_protection":
                return 4;
            case "projectile_protection":
                return 4;
            case "respiration":
                return 3;
            case "aqua_affinity":
                return 1;
            case "thorns":
                return 3;
            case "depth_strider":
                return 3;
            case "frost_walker":
                return 2;
            case "binding_curse":
                return 1;
            case "sharpness":
                return 5;
            case "smite":
                return 5;
            case "bane_of_arthropods":
                return 5;
            case "knockback":
                return 2;
            case "fire_aspect":
                return 2;
            case "looting":
                return 3;
            case "sweeping":
                return 3;
            case "efficiency":
                return 5;
            case "silk_touch":
                return 1;
            case "unbreaking":
                return 3;
            case "fortune":
                return 3;
            case "power":
                return 5;
            case "punch":
                return 2;
            case "flame":
                return 1;
            case "infinity":
                return 1;
            case "luck_of_the_sea":
                return 3;
            case "lure":
                return 3;
            case "loyalty":
                return 3;
            case "impaling":
                return 5;
            case "riptide":
                return 3;
            case "channeling":
                return 1;
            case "multishot":
                return 1;
            case "quick_charge":
                return 3;
            case "piercing":
                return 4;
            case "mending":
                return 1;
            case "vanishing_curse":
                return 1;
            default:
                error("§4Enchant : " + enchant + " not found");
                return 0;
        }
    }
}
