package com.dralexgon.meteorite.modules;

import com.dralexgon.meteorite.Meteorite;
import meteordevelopment.meteorclient.events.entity.player.AttackEntityEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.entity.EntityType;
import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket;

import javax.swing.text.html.parser.Entity;

public class Richarde extends Module {
    public Richarde() {
        super(Meteorite.CATEGORY, "Richarde", "Prevent you to attack Richarde");
    }

    @EventHandler
    public void onSendPacket(AttackEntityEvent event) {
        //if (!isActive()) return;

        if (event.entity.getType() == EntityType.SPIDER) {
            error("Attaquer Richarde est un péché !");
            event.cancel();
        }
    }
}
