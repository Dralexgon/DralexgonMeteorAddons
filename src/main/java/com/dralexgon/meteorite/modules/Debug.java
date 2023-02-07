package com.dralexgon.meteorite.modules;

import com.dralexgon.meteorite.Meteorite;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.PacketListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.network.PacketUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket;
import net.minecraft.text.Text;

import java.util.Set;

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

    private final Setting<Set<Class<? extends Packet<?>>>> s2cPacketsBlackList = sgGeneral.add(new PacketListSetting.Builder()
        .name("S2C-packets")
        .description("Server-to-client packets to black-list.")
        .filter(aClass -> PacketUtils.getS2CPackets().contains(aClass))
        .build()
    );

    private final Setting<Set<Class<? extends Packet<?>>>> c2sPacketsBlackList = sgGeneral.add(new PacketListSetting.Builder()
        .name("C2S-packets")
        .description("Client-to-server packets to black-list.")
        .filter(aClass -> PacketUtils.getC2SPackets().contains(aClass))
        .build()
    );

    public Debug() {
        super(Meteorite.CATEGORY, "Debug", "Prints all received packets");
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onReceivePacket(PacketEvent.Receive event) {
        if (!receivedPackets.get()) return;
        if (!PacketUtils.getS2CPackets().contains(event.packet.getClass())) return;
        if (s2cPacketsBlackList.get().contains(event.packet.getClass())) return;
        info(Text.of("S2C" + event.packet.getClass().getSimpleName()));
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onSendPacket(PacketEvent.Send event) {
        if (!sentPackets.get()) return;
        if (!PacketUtils.getC2SPackets().contains(event.packet.getClass())) return;
        if (c2sPacketsBlackList.get().contains(event.packet.getClass())) return;
        info(Text.of("C2S" + event.packet.getClass().getSimpleName()));
    }
}
