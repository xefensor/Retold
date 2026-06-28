package cz.xefensor.retold.mixin;

import cz.xefensor.retold.client.RetoldTeachingPreviewClient;
import cz.xefensor.retold.network.RetoldLearnRecipePayload;
import cz.xefensor.retold.network.RetoldRequestTeachingPreviewPayload;
import cz.xefensor.retold.villager.RetoldTeachingGui;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MerchantMenu;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MerchantScreen.class)
public abstract class MerchantScreenMixin extends AbstractContainerScreen<MerchantMenu> {
    @Unique
    private Button retold$learnButton;

    @Unique
    private StringWidget retold$titleLabel;

    @Unique
    private StringWidget retold$statusLabel;

    @Unique
    private StringWidget retold$costLabel;

    private MerchantScreenMixin(MerchantMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void retold$addTeachingWidgets(CallbackInfo ci) {
        RetoldTeachingPreviewClient.reset();

        this.retold$titleLabel = new StringWidget(
                this.leftPos + RetoldTeachingGui.PANEL_X,
                this.topPos + RetoldTeachingGui.TITLE_Y,
                RetoldTeachingGui.PANEL_WIDTH,
                20,
                Component.literal("Teaching"),
                this.font
        );

        this.retold$statusLabel = new StringWidget(
                this.leftPos + RetoldTeachingGui.PANEL_X,
                this.topPos + RetoldTeachingGui.STATUS_Y,
                RetoldTeachingGui.PANEL_WIDTH,
                12,
                Component.literal(RetoldTeachingPreviewClient.status()),
                this.font
        );

        this.retold$costLabel = new StringWidget(
                this.leftPos + RetoldTeachingGui.PANEL_X,
                this.topPos + RetoldTeachingGui.COST_Y,
                RetoldTeachingGui.PANEL_WIDTH,
                12,
                Component.literal(RetoldTeachingPreviewClient.cost()),
                this.font
        );

        this.retold$learnButton = Button.builder(
                Component.literal(RetoldTeachingPreviewClient.buttonLabel()),
                button -> ClientPacketDistributor.sendToServer(new RetoldLearnRecipePayload())
        ).bounds(
                this.leftPos + RetoldTeachingGui.PANEL_X,
                this.topPos + RetoldTeachingGui.BUTTON_Y,
                RetoldTeachingGui.PANEL_WIDTH,
                20
        ).tooltip(Tooltip.create(
                Component.literal(RetoldTeachingPreviewClient.tooltip())
        )).build();

        this.addRenderableWidget(this.retold$titleLabel);
        this.addRenderableWidget(this.retold$statusLabel);
        this.addRenderableWidget(this.retold$costLabel);
        this.addRenderableWidget(this.retold$learnButton);

        RetoldTeachingPreviewClient.setRefreshCallback(this::retold$updateTeachingWidgets);

        this.retold$updateTeachingWidgets();

        ClientPacketDistributor.sendToServer(new RetoldRequestTeachingPreviewPayload());
    }

    @Unique
    private void retold$updateTeachingWidgets() {
        if (this.retold$learnButton == null) {
            return;
        }

        this.retold$learnButton.active = RetoldTeachingPreviewClient.active();
        this.retold$learnButton.setMessage(Component.literal(RetoldTeachingPreviewClient.buttonLabel()));
        this.retold$learnButton.setTooltip(Tooltip.create(
                Component.literal(RetoldTeachingPreviewClient.tooltip())
        ));

        if (this.retold$statusLabel != null) {
            this.retold$statusLabel.setMessage(Component.literal(RetoldTeachingPreviewClient.status()));
        }

        if (this.retold$costLabel != null) {
            this.retold$costLabel.setMessage(Component.literal(RetoldTeachingPreviewClient.cost()));
        }
    }
}