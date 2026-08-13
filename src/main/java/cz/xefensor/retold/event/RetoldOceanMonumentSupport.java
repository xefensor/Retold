package cz.xefensor.retold.event;

import cz.xefensor.retold.registry.RetoldTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

final class RetoldOceanMonumentSupport {
    private RetoldOceanMonumentSupport() {
    }

    static StructureStart findAt(ServerLevel level, ElderGuardian elderGuardian) {
        return findAt(level, elderGuardian.blockPosition());
    }

    static StructureStart findAt(ServerLevel level, BlockPos pos) {
        Registry<Structure> structures = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        Structure oceanMonument = structures.getValueOrThrow(BuiltinStructures.OCEAN_MONUMENT);

        return level.structureManager().getStructureWithPieceAt(pos, oceanMonument);
    }

    static boolean isProtectedBlock(BlockState state) {
        return state.is(RetoldTags.OCEAN_MONUMENT_PROTECTED_BLOCKS);
    }

    static boolean isValidMonumentAt(ServerLevel level, BlockPos pos) {
        StructureStart monumentStart = findAt(level, pos);

        return monumentStart != null && monumentStart.isValid();
    }
}
