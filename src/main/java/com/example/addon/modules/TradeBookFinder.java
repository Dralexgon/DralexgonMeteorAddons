package com.example.addon.modules;

import com.example.addon.Addon;
import meteordevelopment.meteorclient.events.entity.player.InteractBlockEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringListSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
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
        .description("The exact name of the enchant you want to find")
        .defaultValue(List.of("mending", "blast_protection"))
        .build()
    );

    public static State state;
    public static long waitingTimestamp;
    public static int nbLecterns;
    public static BlockPos lecternPos;
    public static String lastEnchantedBook;

    public TradeBookFinder() {
        super(Addon.CATEGORY, "TradeBookFinder", "Place and break lecterns to find a book with a specific enchant");
    }

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
        if (!mc.player.getStackInHand(Hand.OFF_HAND).getItem().equals(Items.LECTERN)) {
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
        this.error("This module is not finished yet.");
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
        if (isActive() && state == State.READY) {
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
        //mc.player.sendMessage(Text.of("test1 passed !"), false);
        if (!isActive()) return;
        //mc.player.sendMessage(Text.of("test2 passed !"), false);

        if (packet instanceof OpenScreenS2CPacket) {
            mc.player.sendMessage(Text.of("test3 passed !"), false);
            if (!(state == State.WAITING_FOR_TRADE_OFFER)) return;
            mc.player.sendMessage(Text.of("test4 passed !"), false);
            OpenScreenS2CPacket openScreenS2CPacket = (OpenScreenS2CPacket) packet;
            if (openScreenS2CPacket.getScreenHandlerType() == ScreenHandlerType.MERCHANT) event.cancel();
            mc.player.sendMessage(Text.of("test5 passed !"), false);
        }
        if (packet instanceof SetTradeOffersS2CPacket) {
            mc.player.sendMessage(Text.of("test6 passed !"), false);
            if (!(state == State.WAITING_FOR_TRADE_OFFER)) return;
            lastEnchantedBook = null;
            SetTradeOffersS2CPacket setTradeOffersS2CPacket = (SetTradeOffersS2CPacket) packet;
            setTradeOffersS2CPacket.getOffers().forEach(tradeOffer -> {
                ItemStack item = tradeOffer.getSellItem();
                mc.player.sendMessage(Text.of("test7 passed !"), false);
                if (item.hasNbt()) {
                    item.getNbt().getKeys().forEach(key -> {
                        if (key.equals("StoredEnchantments")) {
                            String id = item.getNbt().get("StoredEnchantments").asString().split("\"")[1].split(":")[1];
                            String lvl = item.getNbt().get("StoredEnchantments").asString().split("lvl:")[1].split("s}")[0];
                            if (enchant.get().contains(id)) {
                                toggle();
                                state = State.FOUND_ENCHANTED_BOOK;
                                Text text = Text.of("§2Found " + enchant + " book!");
                                mc.player.sendMessage(text);
                                mc.player.sendMessage(text, true);
                                mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0F));
                            } else {
                                mc.player.sendMessage(Text.of("§4Found " + id + " book."), true);
                                lastEnchantedBook = lvl;
                            }
                        }
                    });
                }
            });
            mc.player.sendMessage(Text.of("test8 passed !"), false);
            if (state == State.FOUND_ENCHANTED_BOOK) {
                mc.player.sendMessage(Text.of("test9 passed !"), false);
                return;
            }
            if (lastEnchantedBook == null) mc.player.sendMessage(Text.of("§4No enchanted book this trade."), true);
            event.cancel();
            littleReset();
            breakLectern();
            mc.player.sendMessage(Text.of("test10 passed !"), false);
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
            mc.interactionManager.interactEntity(mc.player, closestVillager, mc.player.getActiveHand());
            state = State.WAITING_FOR_TRADE_OFFER;
        }
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        Addon.LOG.info("tick");
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
}
