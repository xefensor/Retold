package cz.xefensor.retold.client.render;

import cz.xefensor.retold.worldgen.earth.EarthGuardianLifecycle;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public final class EarthGuardianRenderState extends LivingEntityRenderState {
    public EarthGuardianLifecycle lifecycle = EarthGuardianLifecycle.DORMANT;
    public float awakeningProgress;
    public float attackTicksRemaining;
}
