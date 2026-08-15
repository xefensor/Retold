package cz.xefensor.retold.villager;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/** Owns persistent Farmer-crop provenance and player offense handling. */
public final class RetoldVillageCropOwnership {
    private RetoldVillageCropOwnership() {
    }

    public static void afterFarmerWork(
            ServerLevel level,
            BlockPos pos,
            BlockState before,
            BlockState after
    ) {
        if (level == null || pos == null || before == null || after == null) {
            return;
        }

        RetoldVillageCropOwnershipData data =
                RetoldVillageCropOwnershipData.get(level);

        if (!(after.getBlock() instanceof CropBlock afterCrop)) {
            data.clear(level, pos);
            return;
        }

        if (!before.equals(after) && afterCrop.getAge(after) == 0) {
            data.mark(level, pos);
        }
    }

    static void handlePlayerPlacement(
            ServerLevel level,
            BlockPos pos,
            BlockState placed,
            Entity placer
    ) {
        if (level == null
                || pos == null
                || placed == null
                || !(placer instanceof ServerPlayer)
                || !(placed.getBlock() instanceof CropBlock)) {
            return;
        }

        RetoldVillageCropOwnershipData.get(level).clear(level, pos);
    }

    static boolean handlePlayerBreak(
            ServerLevel level,
            BlockPos pos,
            BlockState broken,
            ServerPlayer player
    ) {
        if (level == null
                || pos == null
                || broken == null
                || !(broken.getBlock() instanceof CropBlock crop)
                || !isOwned(level, pos)) {
            return false;
        }

        RetoldVillageCropOwnershipData.get(level).clear(level, pos);
        RetoldVillageWitnessReputation.report(
                level,
                player,
                pos,
                crop.isMaxAge(broken)
                        ? RetoldVillageWitnessReputation.Offense
                        .MATURE_CROP_THEFT
                        : RetoldVillageWitnessReputation.Offense
                        .CROP_VANDALISM
        );
        return true;
    }

    static boolean handleFarmlandTrample(
            ServerLevel level,
            BlockPos farmlandPos,
            Entity trampler
    ) {
        if (level == null || farmlandPos == null) {
            return false;
        }

        BlockPos cropPos = farmlandPos.above();

        if (!isOwned(level, cropPos)) {
            return false;
        }

        RetoldVillageCropOwnershipData.get(level).clear(level, cropPos);

        if (trampler instanceof ServerPlayer player) {
            RetoldVillageWitnessReputation.report(
                    level,
                    player,
                    cropPos,
                    RetoldVillageWitnessReputation.Offense.CROP_VANDALISM
            );
        }

        return true;
    }

    public static boolean isOwned(ServerLevel level, BlockPos pos) {
        RetoldVillageCropOwnershipData data =
                RetoldVillageCropOwnershipData.get(level);

        if (!data.isOwned(level, pos)) {
            return false;
        }

        if (!(level.getBlockState(pos).getBlock() instanceof CropBlock)) {
            data.clear(level, pos);
            return false;
        }

        return true;
    }

    static void mark(ServerLevel level, BlockPos pos) {
        RetoldVillageCropOwnershipData.get(level).mark(level, pos);
    }

    static void clear(ServerLevel level, BlockPos pos) {
        RetoldVillageCropOwnershipData.get(level).clear(level, pos);
    }

    static List<BlockPos> ownedCropsNear(
            ServerLevel level,
            BlockPos center,
            int horizontalRadius,
            int verticalRadius,
            int limit
    ) {
        return RetoldVillageCropOwnershipData.get(level).nearby(
                level,
                center,
                horizontalRadius,
                verticalRadius,
                limit
        );
    }
}
