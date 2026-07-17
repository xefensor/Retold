package cz.xefensor.retold;

import com.mojang.logging.LogUtils;
import cz.xefensor.retold.module.RetoldSubsystems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(Retold.MODID)
public final class Retold {
    public static final String MODID = "retold";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Retold(IEventBus modEventBus) {
        RetoldSubsystems.register(modEventBus, NeoForge.EVENT_BUS);
    }
}
