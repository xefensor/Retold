package cz.xefensor.retold.event;

import cz.xefensor.retold.command.RetoldCommands;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class RetoldCommandEvents {
    private RetoldCommandEvents() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        RetoldCommands.register(event.getDispatcher());
    }
}