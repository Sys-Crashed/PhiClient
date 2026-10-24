package org.dpdns.syscrashed.phiclient.injection.mixin.mixins;

import org.dpdns.syscrashed.phiclient.PhiClient;
import org.dpdns.syscrashed.phiclient.management.event.impl.EventTick;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.Display;

/**
 * 对原版 {@link Minecraft} 的织入，是 PhiClient 的「第二层入口」：
 *
 * - {@code createDisplay}：窗口创建完成后改写窗口标题（原版写死的 "Minecraft 1.8.9"）；
 * - {@code startGame}：在 ingameGUI 字段赋值之后触发 {@link PhiClient#start()}（真正的业务初始化起点）；
 * - {@code runTick}：每帧末尾派发 {@link EventTick}，打通事件总线闭环；
 * - {@code shutdownMinecraftApplet}：触发 {@link PhiClient#stop()}。
 *
 * 新增功能请按「Mixin 钩生命周期 → 派发事件 → 模块监听」的方式扩展，
 * 不要把业务逻辑直接堆在这里。
 */
@Mixin(Minecraft.class)
public class MixinMinecraft {

    @Inject(method = "createDisplay", at = @At("TAIL"))
    private void phi$createDisplay(CallbackInfo ci) {
        Display.setTitle(PhiClient.WINDOW_TITLE);
    }

    @Inject(
            method = "startGame",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/Minecraft;ingameGUI:Lnet/minecraft/client/gui/GuiIngame;",
                    opcode = Opcodes.PUTFIELD,
                    shift = At.Shift.AFTER
            )
    )
    private void phi$startGame(CallbackInfo ci) {
        PhiClient.getInstance().start();
    }

    @Inject(method = "runTick", at = @At("TAIL"))
    private void phi$runTick(CallbackInfo ci) {
        new EventTick().call();
    }

    @Inject(method = "shutdownMinecraftApplet", at = @At("HEAD"))
    private void phi$shutdown(CallbackInfo ci) {
        PhiClient.getInstance().stop();
    }
}
