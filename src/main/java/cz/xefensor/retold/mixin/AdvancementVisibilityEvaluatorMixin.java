package cz.xefensor.retold.mixin;

import cz.xefensor.retold.Retold;
import net.minecraft.advancements.Advancement;
import net.minecraft.server.advancements.AdvancementVisibilityEvaluator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AdvancementVisibilityEvaluator.class)
public abstract class AdvancementVisibilityEvaluatorMixin {
    private static final Object RETOLD_HIDE_RULE =
            retold$findVisibilityRule("HIDE");

    private static final boolean RETOLD_ADVANCEMENT_HIDING_ENABLED =
            RETOLD_HIDE_RULE != null;

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
        if (isDone) {
            return;
        }

        if (!RETOLD_ADVANCEMENT_HIDING_ENABLED) {
            return;
        }

        cir.setReturnValue(RETOLD_HIDE_RULE);
    }

    private static Object retold$findVisibilityRule(String name) {
        try {
            Class ruleClass = Class.forName(
                    "net.minecraft.server.advancements.AdvancementVisibilityEvaluator$VisibilityRule"
            );

            Object[] constants = ruleClass.getEnumConstants();

            if (constants == null) {
                Retold.LOGGER.warn(
                        "Could not enable advancement hiding: VisibilityRule is not an enum"
                );

                return null;
            }

            for (Object constant : constants) {
                if (constant instanceof Enum enumConstant
                        && enumConstant.name().equals(name)) {
                    return constant;
                }
            }

            Retold.LOGGER.warn(
                    "Could not enable advancement hiding: missing VisibilityRule.{}",
                    name
            );

            return null;
        } catch (ReflectiveOperationException | LinkageError exception) {
            Retold.LOGGER.warn(
                    "Could not enable advancement hiding. Retold will leave advancement visibility unchanged.",
                    exception
            );

            return null;
        }
    }
}