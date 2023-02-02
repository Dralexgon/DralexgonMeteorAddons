package com.dralexgon.meteorite.modules;

import com.dralexgon.meteorite.Meteorite;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket;

public class Feur extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> ignorePunctuation = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-punctuation")
        .description("Ignore punctuation when checking if the message ends with \"quoi\".")
        .defaultValue(true)
        .build()
    );

    public Feur() {
        super(Meteorite.CATEGORY, "Feur", "Answer \"feur\" to messages ending with \"quoi\"");
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onReceivePacket(PacketEvent.Receive event) {
        if (!isActive()) return;
        if (!(event.packet instanceof ChatMessageS2CPacket)) return;

        ChatMessageS2CPacket packet = (ChatMessageS2CPacket) event.packet;
        String message = packet.body().content();
        if (ignorePunctuation.get()) message = message.replaceAll("[ .!?]", "");
        if (!packet.sender().equals(mc.player.getUuid()) && message.endsWith("quoi"))
            mc.player.networkHandler.sendChatMessage("feur !");
    }
}
