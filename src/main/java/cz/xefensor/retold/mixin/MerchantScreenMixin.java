package cz.xefensor.retold.mixin;

import cz.xefensor.retold.client.RetoldTeachingPreviewClient;
import cz.xefensor.retold.network.RetoldLearnRecipePayload;
import cz.xefensor.retold.network.RetoldRequestTeachingPreviewPayload;
import cz.xefensor.retold.network.RetoldTeachingPreviewPayload;
import cz.xefensor.retold.villager.RetoldTeachingGui;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MerchantMenu;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER;

@Mixin(MerchantScreen.class)
public abstract class MerchantScreenMixin extends AbstractContainerScreen<MerchantMenu> {
    @Unique
    private static final int RETOLD$FEEDBACK_HIGHLIGHT_TICKS = 20;

    @Unique
    private Button retold$learnButton;

    @Unique
    private MultiLineTextWidget retold$statusLabel;

    @Unique
    private StringWidget retold$costLabel;

    @Unique
    private MultiLineTextWidget retold$detailsLabel;

    @Unique
    private boolean retold$learnPending;

    @Unique
    private int retold$feedbackHighlightTicks;

    @Unique
    private int retold$feedbackBorder = RetoldTeachingGui.SUCCESS_BORDER;

    private MerchantScreenMixin(MerchantMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void retold$addTeachingWidgets(CallbackInfo ci) {
        RetoldTeachingPreviewClient.reset();
        this.retold$learnPending = false;

        this.retold$statusLabel = new MultiLineTextWidget(
                this.leftPos + RetoldTeachingGui.CONTENT_X,
                this.topPos + RetoldTeachingGui.STATUS_Y,
                RetoldTeachingPreviewClient.status(),
                this.font
        ).setMaxWidth(RetoldTeachingGui.CONTENT_WIDTH)
                .setMaxRows(2)
                .setCentered(true);

        this.retold$costLabel = new StringWidget(
                this.leftPos + RetoldTeachingGui.CONTENT_X,
                this.topPos + RetoldTeachingGui.COST_Y,
                RetoldTeachingGui.CONTENT_WIDTH,
                RetoldTeachingGui.TEXT_HEIGHT,
                RetoldTeachingPreviewClient.cost(),
                this.font
        ).setMaxWidth(RetoldTeachingGui.CONTENT_WIDTH);

        this.retold$detailsLabel = new MultiLineTextWidget(
                this.leftPos + RetoldTeachingGui.CONTENT_X,
                this.topPos + RetoldTeachingGui.DETAILS_Y,
                RetoldTeachingPreviewClient.tooltip().copy()
                        .withStyle(ChatFormatting.GRAY),
                this.font
        ).setMaxWidth(RetoldTeachingGui.CONTENT_WIDTH)
                .setMaxRows(3)
                .setCentered(true);

        this.retold$learnButton = Button.builder(
                RetoldTeachingPreviewClient.buttonLabel(),
                button -> this.retold$requestTeaching()
        ).bounds(
                this.leftPos + RetoldTeachingGui.BUTTON_X,
                this.topPos + RetoldTeachingGui.BUTTON_Y,
                RetoldTeachingGui.BUTTON_WIDTH,
                RetoldTeachingGui.BUTTON_HEIGHT
        ).tooltip(Tooltip.create(
                RetoldTeachingPreviewClient.tooltip()
        )).build();

        this.addRenderableWidget(this.retold$statusLabel);
        this.addRenderableWidget(this.retold$costLabel);
        this.addRenderableWidget(this.retold$detailsLabel);
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

        RetoldTeachingPreviewPayload.Feedback feedback =
                RetoldTeachingPreviewClient.takeFeedback();
        if (feedback != RetoldTeachingPreviewPayload.Feedback.NONE) {
            this.retold$showTeachingFeedback(feedback);
        }

        this.retold$learnPending = false;
        this.retold$learnButton.active = RetoldTeachingPreviewClient.active();
        this.retold$learnButton.setMessage(RetoldTeachingPreviewClient.buttonLabel());
        this.retold$learnButton.setTooltip(Tooltip.create(
                RetoldTeachingPreviewClient.tooltip()
        ));

        if (this.retold$statusLabel != null) {
            this.retold$statusLabel.setMessage(RetoldTeachingPreviewClient.status());
            this.retold$statusLabel.setX(
                    this.leftPos + RetoldTeachingGui.PANEL_X
                            + (RetoldTeachingGui.PANEL_WIDTH
                            - this.retold$statusLabel.getWidth()) / 2
            );
        }

        if (this.retold$costLabel != null) {
            this.retold$costLabel.setMessage(RetoldTeachingPreviewClient.cost());
            this.retold$costLabel.setX(
                    this.leftPos + RetoldTeachingGui.PANEL_X
                            + (RetoldTeachingGui.PANEL_WIDTH
                            - this.retold$costLabel.getWidth()) / 2
            );
        }

        if (this.retold$detailsLabel != null) {
            this.retold$detailsLabel.setMessage(
                    RetoldTeachingPreviewClient.tooltip().copy()
                            .withStyle(ChatFormatting.GRAY)
            );
            this.retold$detailsLabel.setX(
                    this.leftPos + RetoldTeachingGui.PANEL_X
                            + (RetoldTeachingGui.PANEL_WIDTH
                            - this.retold$detailsLabel.getWidth()) / 2
            );
        }
    }

    @Unique
    private void retold$requestTeaching() {
        if (this.retold$learnPending || !RetoldTeachingPreviewClient.active()) {
            return;
        }

        this.retold$learnPending = true;
        this.retold$learnButton.active = false;
        ClientPacketDistributor.sendToServer(new RetoldLearnRecipePayload());
    }

    @Unique
    private void retold$showTeachingFeedback(
            RetoldTeachingPreviewPayload.Feedback feedback
    ) {
        boolean success = feedback == RetoldTeachingPreviewPayload.Feedback.SUCCESS;
        this.retold$feedbackBorder = success
                ? RetoldTeachingGui.SUCCESS_BORDER
                : RetoldTeachingGui.FAILURE_BORDER;
        this.retold$feedbackHighlightTicks = RETOLD$FEEDBACK_HIGHLIGHT_TICKS;
        if (this.minecraft.player != null) {
            this.minecraft.player.playSound(
                    success ? SoundEvents.VILLAGER_YES : SoundEvents.VILLAGER_NO,
                    0.55F,
                    success ? 1.1F : 0.9F
            );
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (this.retold$feedbackHighlightTicks > 0) {
            this.retold$feedbackHighlightTicks--;
        }
    }

    @Inject(method = "extractBackground", at = @At("TAIL"))
    private void retold$extractTeachingPanel(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float partialTick,
            CallbackInfo ci
    ) {
        int panelX = this.leftPos + RetoldTeachingGui.PANEL_X;
        int panelY = this.topPos + RetoldTeachingGui.PANEL_Y;
        int slotFrameX = this.leftPos + RetoldTeachingGui.SLOT_X
                - (RetoldTeachingGui.SLOT_FRAME_SIZE - RetoldTeachingGui.SLOT_SIZE) / 2;
        int slotFrameY = this.topPos + RetoldTeachingGui.SLOT_Y
                - (RetoldTeachingGui.SLOT_FRAME_SIZE - RetoldTeachingGui.SLOT_SIZE) / 2;

        graphics.fill(
                panelX,
                panelY,
                panelX + RetoldTeachingGui.PANEL_WIDTH,
                panelY + RetoldTeachingGui.PANEL_HEIGHT,
                RetoldTeachingGui.PANEL_BACKGROUND
        );
        graphics.outline(
                panelX,
                panelY,
                RetoldTeachingGui.PANEL_WIDTH,
                RetoldTeachingGui.PANEL_HEIGHT,
                RetoldTeachingGui.PANEL_BORDER
        );
        graphics.fill(
                panelX + RetoldTeachingGui.PANEL_PADDING,
                this.topPos + RetoldTeachingGui.STATUS_Y
                        - RetoldTeachingGui.CONTROL_GAP,
                panelX + RetoldTeachingGui.PANEL_WIDTH
                        - RetoldTeachingGui.PANEL_PADDING,
                this.topPos + RetoldTeachingGui.COST_Y
                        + RetoldTeachingGui.TEXT_HEIGHT
                        + RetoldTeachingGui.CONTROL_GAP,
                RetoldTeachingGui.SECTION_BACKGROUND
        );
        graphics.fill(
                slotFrameX,
                slotFrameY,
                slotFrameX + RetoldTeachingGui.SLOT_FRAME_SIZE,
                slotFrameY + RetoldTeachingGui.SLOT_FRAME_SIZE,
                RetoldTeachingGui.SECTION_BACKGROUND
        );
        graphics.outline(
                slotFrameX,
                slotFrameY,
                RetoldTeachingGui.SLOT_FRAME_SIZE,
                RetoldTeachingGui.SLOT_FRAME_SIZE,
                this.retold$feedbackHighlightTicks > 0
                        ? this.retold$feedbackBorder
                        : RetoldTeachingPreviewClient.active()
                        ? RetoldTeachingGui.READY_BORDER
                        : RetoldTeachingGui.PANEL_BORDER
        );
        graphics.centeredText(
                this.font,
                Component.translatable("container.retold.teaching.title"),
                panelX + RetoldTeachingGui.PANEL_WIDTH / 2,
                this.topPos + RetoldTeachingGui.TITLE_Y,
                RetoldTeachingGui.TEXT_COLOR
        );
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (!event.hasControlDown()
                && !event.hasAltDown()
                && (key == GLFW_KEY_ENTER || key == GLFW_KEY_KP_ENTER)) {
            if (this.retold$learnButton != null && this.retold$learnButton.active) {
                this.retold$requestTeaching();
            }
            return true;
        }
        return super.keyPressed(event);
    }

}
