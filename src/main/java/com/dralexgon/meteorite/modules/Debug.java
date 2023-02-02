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
import net.minecraft.text.Text;

public class Debug extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> receivedPackets = sgGeneral.add(new BoolSetting.Builder()
        .name("received-packets")
        .description("Prints all received packets.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> sentPackets = sgGeneral.add(new BoolSetting.Builder()
        .name("sent-packets")
        .description("Prints all sent packets.")
        .defaultValue(true)
        .build()
    );

    public Debug() {
        super(Meteorite.CATEGORY, "Debug", "Prints all received packets");
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onReceivePacket(PacketEvent.Receive event) {
        if (!receivedPackets.get()) return;
        info(Text.of(event.packet.toString()));
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onSendPacket(PacketEvent.Send event) {
        if (!sentPackets.get()) return;
        info(Text.of(event.packet.toString()));
    }
}
