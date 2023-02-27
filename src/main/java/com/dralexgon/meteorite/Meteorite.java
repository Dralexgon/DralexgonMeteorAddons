package com.dralexgon.meteorite;

import com.dralexgon.meteorite.commands.CommandExample;
import com.dralexgon.meteorite.hud.HudExample;
import com.dralexgon.meteorite.modules.*;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.commands.Commands;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;

public class Meteorite extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("Meteorite");
    public static final HudGroup HUD_GROUP = new HudGroup("ExampleHudDralexgon");

    @Override
    public void onInitialize() {
        LOG.info("Initializing Meteor Addon Template");

        // Modules

        Modules.get().add(new TradeBookFinder());
        if (Modules.get().get(TradeBookFinder.class).isActive())
            Modules.get().get(TradeBookFinder.class).toggle();
        Modules.get().add(new Feur());
        Modules.get().add(new Richarde());
        //Modules.get().add(new Debug());
        Modules.get().add(new AutoLight());


        // Commands
        Commands.get().add(new CommandExample());

        // HUD
        Hud.get().register(HudExample.INFO);
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "com.dralexgon.meteorite";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("Dralexgon", "Meteorite");
    }
}
