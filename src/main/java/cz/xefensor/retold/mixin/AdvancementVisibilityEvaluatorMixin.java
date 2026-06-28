package cz.xefensor.retold.mixin;

import net.minecraft.advancements.Advancement;
import net.minecraft.server.advancements.AdvancementVisibilityEvaluator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AdvancementVisibilityEvaluator.class)
public abstract class AdvancementVisibilityEvaluatorMixin {
    private static final Object RETOLD_HIDE_RULE = retold$getVisibilityRule("HIDE");

    @Inject(
            method = "evaluateVisibilityRule(Lnet/minecraft/advancements/Advancement;Z)Lnet/minecraft/server/advancements/AdvancementVisibilityEvaluator$VisibilityRule;",
            at = @At("HEAD"),
            cancellable = true
    )
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void retold$hideUnfinishedAdvancements(
            Advancement advancement,
            boolean isDone,
            CallbackInfoReturnable cir
    ) {
        if (!isDone) {
            cir.setReturnValue(RETOLD_HIDE_RULE);
        }
    }

    private static Object retold$getVisibilityRule(String name) {
        try {
            Class<?> ruleClass = Class.forName(
                    "net.minecraft.server.advancements.AdvancementVisibilityEvaluator$VisibilityRule"
            );

            for (Object constant : ruleClass.getEnumConstants()) {
                if (constant instanceof Enum<?> enumConstant && enumConstant.name().equals(name)) {
                    return constant;
                }
            }

            throw new IllegalStateException("Missing AdvancementVisibilityEvaluator.VisibilityRule." + name);
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Could not find AdvancementVisibilityEvaluator.VisibilityRule", exception);
        }
    }
}