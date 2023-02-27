package com.dralexgon.meteorite.modules;

import com.dralexgon.meteorite.Meteorite;
import meteordevelopment.meteorclient.systems.modules.Module;

public class FlyingGroup extends Module {

    public FlyingGroup() {
        super(Meteorite.CATEGORY, "FlyingGroup", "Follow a player");
    }

}
