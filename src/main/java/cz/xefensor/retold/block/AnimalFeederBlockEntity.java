package cz.xefensor.retold.block;

import cz.xefensor.retold.behavior.profiles.RetoldMobRules;
import cz.xefensor.retold.registry.RetoldBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class AnimalFeederBlockEntity extends BlockEntity implements Container {
    private ItemStack food = ItemStack.EMPTY;

    public AnimalFeederBlockEntity(BlockPos pos, BlockState state) {
        super(RetoldBlockEntities.ANIMAL_FEEDER.get(), pos, state);
    }

    public static boolean canStore(ItemStack stack) {
        return RetoldMobRules.isAnimalFeederFood(stack);
    }

    public boolean hasFoodFor(PathfinderMob mob) {
        return !this.food.isEmpty()
                && RetoldMobRules.canEatDroppedItem(mob, this.food);
    }

    public int insert(ItemStack source) {
        if (!canStore(source)) {
            return 0;
        }

        if (!this.food.isEmpty()
                && !ItemStack.isSameItemSameComponents(this.food, source)) {
            return 0;
        }

        int storedCount = this.food.isEmpty() ? 0 : this.food.getCount();
        int maximum = Math.min(64, source.getMaxStackSize());
        int inserted = Math.min(source.getCount(), maximum - storedCount);

        if (inserted <= 0) {
            return 0;
        }

        if (this.food.isEmpty()) {
            this.food = source.copyWithCount(inserted);
        } else {
            this.food.grow(inserted);
        }

        source.shrink(inserted);
        this.setChanged();
        return inserted;
    }

    public ItemStack takeAll() {
        ItemStack removed = this.food;

        if (removed.isEmpty()) {
            return ItemStack.EMPTY;
        }

        this.food = ItemStack.EMPTY;
        this.setChanged();
        return removed;
    }

    public ItemStack takeOneFor(PathfinderMob mob) {
        if (!this.hasFoodFor(mob)) {
            return ItemStack.EMPTY;
        }

        ItemStack removed = this.food.split(1);

        if (this.food.isEmpty()) {
            this.food = ItemStack.EMPTY;
        }

        this.setChanged();
        return removed;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("food", ItemStack.OPTIONAL_CODEC, this.food);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        ItemStack loaded = input.read("food", ItemStack.OPTIONAL_CODEC)
                .orElse(ItemStack.EMPTY);
        this.food = canStore(loaded) ? loaded : ItemStack.EMPTY;
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return this.food.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot == 0 ? this.food : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        if (slot != 0 || count <= 0) {
            return ItemStack.EMPTY;
        }

        ItemStack removed = this.food.split(count);

        if (!removed.isEmpty()) {
            if (this.food.isEmpty()) {
                this.food = ItemStack.EMPTY;
            }
            this.setChanged();
        }

        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot != 0) {
            return ItemStack.EMPTY;
        }

        ItemStack removed = this.food;
        this.food = ItemStack.EMPTY;
        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot != 0) {
            return;
        }

        if (stack.isEmpty()) {
            this.food = ItemStack.EMPTY;
        } else if (canStore(stack)) {
            this.food = stack.copyWithCount(Math.min(64, stack.getCount()));
        } else {
            return;
        }

        this.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == 0
                && canStore(stack)
                && (this.food.isEmpty()
                || ItemStack.isSameItemSameComponents(this.food, stack));
    }

    @Override
    public void clearContent() {
        if (this.food.isEmpty()) {
            return;
        }

        this.food = ItemStack.EMPTY;
        this.setChanged();
    }
}
