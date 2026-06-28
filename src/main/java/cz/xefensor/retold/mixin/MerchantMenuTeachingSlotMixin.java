package cz.xefensor.retold.mixin;

import cz.xefensor.retold.villager.RetoldTeachingSlotMenu;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.Merchant;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import cz.xefensor.retold.villager.RetoldTeachingGui;
import cz.xefensor.retold.villager.RetoldVillagerTeaching;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MerchantMenu.class)
public abstract class MerchantMenuTeachingSlotMixin extends AbstractContainerMenu implements RetoldTeachingSlotMenu {
    @Unique
    private final SimpleContainer retold$teachingContainer = new SimpleContainer(1);

    @Unique
    private Slot retold$teachingSlot;

    @Unique
    private int retold$teachingSlotIndex = -1;

    @Unique
    private Player retold$player;

    protected MerchantMenuTeachingSlotMixin(MenuType<?> menuType, int containerId) {
        super(menuType, containerId);
    }

    @Inject(
            method = "<init>(ILnet/minecraft/world/entity/player/Inventory;)V",
            at = @At("TAIL"),
            require = 0
    )
    private void retold$addTeachingSlotClientConstructor(
            int containerId,
            Inventory playerInventory,
            CallbackInfo ci
    ) {
        this.retold$installTeachingSlot(playerInventory);
    }

    @Inject(
            method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/item/trading/Merchant;)V",
            at = @At("TAIL"),
            require = 0
    )
    private void retold$addTeachingSlotServerConstructor(
            int containerId,
            Inventory playerInventory,
            Merchant merchant,
            CallbackInfo ci
    ) {
        this.retold$installTeachingSlot(playerInventory);
    }

    @Unique
    private void retold$installTeachingSlot(Inventory playerInventory) {
        this.retold$player = playerInventory.player;

        if (this.retold$teachingSlot != null) {
            return;
        }

        this.retold$teachingSlot = this.addSlot(new Slot(
                this.retold$teachingContainer,
                0,
                RetoldTeachingGui.SLOT_X,
                RetoldTeachingGui.SLOT_Y
        ) {
            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                return !stack.isEmpty() && !stack.is(Items.EMERALD);
            }

            @Override
            public void setChanged() {
                super.setChanged();
                MerchantMenuTeachingSlotMixin.this.broadcastChanges();
                MerchantMenuTeachingSlotMixin.this.retold$sendTeachingPreview();
            }
        });

        this.retold$teachingSlotIndex = this.slots.indexOf(this.retold$teachingSlot);
    }

    @Inject(method = "removed", at = @At("HEAD"), require = 0)
    private void retold$returnTeachingSlotItem(Player player, CallbackInfo ci) {
        this.clearContainer(player, this.retold$teachingContainer);
    }

    @Override
    public ItemStack retold$getTeachingItem() {
        if (this.retold$teachingSlot == null) {
            return ItemStack.EMPTY;
        }

        return this.retold$teachingSlot.getItem();
    }

    @Override
    public int retold$getTeachingSlotIndex() {
        return this.retold$teachingSlotIndex;
    }

    @Unique
    private void retold$sendTeachingPreview() {
        if (this.retold$player instanceof ServerPlayer serverPlayer) {
            RetoldVillagerTeaching.sendPreviewToClient(serverPlayer);
        }
    }

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void retold$quickMoveTeachingSlot(
            Player player,
            int index,
            CallbackInfoReturnable<ItemStack> cir
    ) {
        if (index != this.retold$teachingSlotIndex) {
            return;
        }

        Slot slot = this.slots.get(index);

        if (!slot.hasItem()) {
            cir.setReturnValue(ItemStack.EMPTY);
            return;
        }

        ItemStack stackInSlot = slot.getItem();
        ItemStack originalStack = stackInSlot.copy();

        // Vanilla merchant menu má player inventory před naším teaching slotem.
        // Proto přesouváme do rozsahu 3 až teachingSlotIndex.
        if (!this.moveItemStackTo(stackInSlot, 3, this.retold$teachingSlotIndex, true)) {
            cir.setReturnValue(ItemStack.EMPTY);
            return;
        }

        if (stackInSlot.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        this.retold$sendTeachingPreview();

        cir.setReturnValue(originalStack);
    }
}