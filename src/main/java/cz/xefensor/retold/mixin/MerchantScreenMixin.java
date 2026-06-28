package cz.xefensor.retold.mixin;

import cz.xefensor.retold.network.RetoldLearnRecipePayload;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MerchantMenu;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MerchantScreen.class)
public abstract class MerchantScreenMixin extends AbstractContainerScreen<MerchantMenu> {
    private MerchantScreenMixin(MerchantMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void retold$addLearnButton(CallbackInfo ci) {
        this.addRenderableWidget(
                Button.builder(
                        Component.literal("Learn"),
                        button -> ClientPacketDistributor.sendToServer(new RetoldLearnRecipePayload())
                ).bounds(
                        this.leftPos + this.imageWidth + 4,
                        this.topPos + 18,
                        70,
                        20
                ).build()
        );
    }
}