package com.example.addon.modules;

import com.example.addon.Addon;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket;
import net.minecraft.text.Text;

public class Feur extends Module {
    public Feur() {
        super(Addon.CATEGORY, "Feur", "Answer \"feur\" to messages ending with \"quoi\"");
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onReceivePacket(PacketEvent.Receive event) {
        if (!isActive()) return;
        if (!(event.packet instanceof ChatMessageS2CPacket)) return;

        ChatMessageS2CPacket packet = (ChatMessageS2CPacket) event.packet;
        Addon.LOG.info(packet.body().content());
        if (!packet.sender().equals(mc.player.getUuid()) && packet.body().content().endsWith("quoi"))
            mc.player.networkHandler.sendChatMessage("feur");
    }
}
