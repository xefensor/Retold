package cz.xefensor.retold.client.render;

import cz.xefensor.retold.worldgen.fire.Wildfire;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public final class WildfireRenderState extends LivingEntityRenderState {
    public int shieldCount = Wildfire.MAX_SHIELDS;
}
