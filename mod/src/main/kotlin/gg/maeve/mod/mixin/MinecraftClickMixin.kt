package gg.maeve.mod.mixin

import gg.maeve.mod.module.hud.ClickTracker
import net.minecraft.client.Minecraft
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

/** Counts physical mouse presses for the CPS HUD: startAttack = left, startUseItem = right. */
@Mixin(Minecraft::class)
class MinecraftClickMixin {
    @Inject(method = ["startAttack"], at = [At("HEAD")])
    private fun maeve_onStartAttack(cir: CallbackInfoReturnable<Boolean>) {
        ClickTracker.onLeft()
    }

    @Inject(method = ["startUseItem"], at = [At("HEAD")])
    private fun maeve_onStartUseItem(ci: CallbackInfo) {
        ClickTracker.onRight()
    }
}
