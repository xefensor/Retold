package cz.xefensor.retold.registry;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.serialization.Codec;
import cz.xefensor.retold.Retold;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;
import net.minecraft.world.level.gamerules.GameRuleType;
import net.minecraft.world.level.gamerules.GameRuleTypeVisitor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class RetoldGameRules {
    public static final DeferredRegister<GameRule<?>> GAME_RULES =
            DeferredRegister.create(Registries.GAME_RULE, Retold.MODID);

    public static final DeferredHolder<GameRule<?>, GameRule<Boolean>> DO_BED_NIGHT_SKIPPING =
            GAME_RULES.register("do_bed_night_skipping", () -> new GameRule<Boolean>(
                    GameRuleCategory.PLAYER,
                    GameRuleType.BOOL,
                    BoolArgumentType.bool(),
                    GameRuleTypeVisitor::visitBoolean,
                    Codec.BOOL,
                    value -> value ? 1 : 0,
                    false,
                    FeatureFlagSet.of()
            ));

    private RetoldGameRules() {
    }

    public static void register(IEventBus modEventBus) {
        GAME_RULES.register(modEventBus);
    }

    public static boolean doBedNightSkipping(ServerLevel level) {
        GameRule<Boolean> rule = DO_BED_NIGHT_SKIPPING.get();
        return level.getGameRules().get(rule);
    }
}