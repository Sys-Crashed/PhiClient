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
 * Mixin onto vanilla {@link Minecraft}; the second entry layer for PhiClient
 * (package {@code org.dpdns.syscrashed.phiclient.injection.mixin.mixins}).
 *
 * <ul>
 *   <li>{@code createDisplay}: rewrites the window title after the display is created (vanilla hard-codes "Minecraft 1.8.9").</li>
 *   <li>{@code startGame}: triggers {@link PhiClient#start()} after the {@code ingameGUI} field is assigned (the real business init point).</li>
 *   <li>{@code runTick}: dispatches {@link EventTick} at the end of every frame, closing the event-bus loop.</li>
 *   <li>{@code shutdownMinecraftApplet}: triggers {@link PhiClient#stop()}.</li>
 * </ul>
 *
 * <p>Extend by "Mixin hooks a lifecycle -&gt; dispatches an event -&gt; a module listens", rather than
 * piling business logic directly into this class.</p>
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
