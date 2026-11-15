package org.dpdns.syscrashed.phiclient.gui.render;

import java.util.List;

import org.dpdns.syscrashed.phiclient.logger.PhiLogger;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.client.shader.Shader;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.client.shader.ShaderManager;
import net.minecraft.client.shader.ShaderUniform;
import net.minecraft.util.ResourceLocation;

/**
 * Background blur: a two-pass (horizontal + vertical) Gaussian blur using Minecraft's own shader pipeline
 * (package {@code org.dpdns.syscrashed.phiclient.gui.render}).
 *
 * <p>Approach:</p>
 * <ul>
 *   <li>the shader is {@code assets/minecraft/shaders/post/blurArea.json} (kept in the repo resources);
 *       its two internal passes do horizontal and vertical blur;</li>
 *   <li>{@code BlurXY / BlurCoord} define the blurred region and {@code Radius} defines the strength;
 *       these uniforms have no public setter in 1.8.9, so they are set per-shader via
 *       {@link ShaderGroupAccess} (which finds the shader list by type reflection);</li>
 *   <li>the blurred result is written back to MC's main framebuffer, which is then re-bound.</li>
 * </ul>
 *
 * <p>Compared to hand-rolling an FBO + Gaussian pass, going through MC's {@link ShaderGroup} has two
 * advantages: the framebuffer chain is managed by MC (no missed viewport / texture-filter restores) and
 * it does not touch MC's {@code GlStateManager} cache.</p>
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #render(float, float)} - blurs the current main framebuffer (writes the result back).</li>
 *   <li>{@link #delete()} - releases the self-managed resources (called on game exit).</li>
 * </ul>
 */
public final class BlurRenderer {

    /** The blur post-processing script. */
    private static final ResourceLocation BLUR_SHADER = new ResourceLocation("shaders/post/blurArea.json");

    private final Minecraft mc = Minecraft.getMinecraft();

    private ShaderGroup shaderGroup;
    private int lastWidth = -1;
    private int lastHeight = -1;
    private boolean disabled;

    /**
     * Blurs the whole main framebuffer once (result is written back to the main framebuffer). If any
     * step fails, the renderer permanently disables itself and logs only once, to avoid spamming every
     * frame / crashing the game.
     *
     * @param partialTicks the render interpolation factor.
     * @param radius       the blur radius.
     */
    public void render(float partialTicks, float radius) {
        ScaledResolution sr = new ScaledResolution(this.mc);
        render(partialTicks, radius, 0.0F, 0.0F, sr.getScaledWidth(), sr.getScaledHeight());
    }

    /**
     * Blurs only the given rectangle of the main framebuffer (result is written back). The rectangle
     * is given in the scaled GuiScreen coordinate space; it is widened and clamped inside the shader
     * via the {@code BlurXY / BlurCoord} region uniforms, so only the area behind a modal overlay is
     * blurred and the rest of the frame stays untouched.
     *
     * @param partialTicks the render interpolation factor.
     * @param radius       the blur radius.
     * @param x            the left x of the region.
     * @param y            the top y of the region.
     * @param width        the width of the region.
     * @param height       the height of the region.
     */
    public void render(float partialTicks, float radius, float x, float y, float width, float height) {
        if (this.disabled) {
            return;
        }

        try {
            if (!OpenGlHelper.shadersSupported) {
                this.disabled = true;
                PhiLogger.warn("Blur disabled: shaders are not supported.");
                return;
            }

            if (this.shaderGroup == null || this.lastWidth != this.mc.displayWidth || this.lastHeight != this.mc.displayHeight) {
                if (this.shaderGroup != null) {
                    this.shaderGroup.deleteShaderGroup();
                    this.shaderGroup = null;
                }

                Framebuffer main = this.mc.getFramebuffer();
                this.shaderGroup = new ShaderGroup(this.mc.getTextureManager(), this.mc.getResourceManager(), main, BLUR_SHADER);

                // Must call this: in 1.8.9 only createBindFramebuffers will
                //   1) resetProjectionMatrix() build the projection matrix;
                //   2) distribute the projection matrix to every shader.
                // Without it, the ProjMat uniform receives a null Matrix4f and
                // Shader.loadShader throws NullPointerException (this step is required here too).
                this.shaderGroup.createBindFramebuffers(this.mc.displayWidth, this.mc.displayHeight);

                this.lastWidth = this.mc.displayWidth;
                this.lastHeight = this.mc.displayHeight;
            }

            applyUniforms(radius, x, y, width, height);

            this.shaderGroup.loadShaderGroup(partialTicks);

            this.mc.getFramebuffer().bindFramebuffer(true);
        } catch (Throwable t) {
            this.disabled = true;
            PhiLogger.error("Blur failed and has been disabled", t);
        }
    }

    /**
     * Sets the uniforms for both passes: {@code BlurXY} is the region origin, {@code BlurCoord} is the
     * region size (both halved to match the half-resolution blur). The region is given in scaled
     * GuiScreen coordinates; the {@code y} axis is flipped because the shader reads the framebuffer
     * texture with its origin at the bottom left, while GuiScreen coordinates grow downwards.
     */
    private void applyUniforms(float radius, float x, float y, float width, float height) {
        ScaledResolution sr = new ScaledResolution(this.mc);

        float scale = sr.getScaleFactor();
        int scaledWidth = sr.getScaledWidth();
        int scaledHeight = sr.getScaledHeight();

        List<Shader> shaders = ShaderGroupAccess.getShaders(this.shaderGroup);
        if (shaders == null) {
            return;
        }

        for (Shader shader : shaders) {
            ShaderManager manager = shader.getShaderManager();
            if (manager == null) {
                continue;
            }

            setUniform(manager, "BlurXY", x * (scale / 2.0F),
                    (scaledHeight - y - height) * (scale / 2.0F));
            setUniform(manager, "BlurCoord", width * (scale / 2.0F), height * (scale / 2.0F));
            setUniform(manager, "Radius", radius);
        }
    }

    private static void setUniform(ShaderManager manager, String name, float a, float b) {
        ShaderUniform uniform = manager.getShaderUniform(name);
        if (uniform != null) {
            uniform.set(a, b);
        }
    }

    private static void setUniform(ShaderManager manager, String name, float a) {
        ShaderUniform uniform = manager.getShaderUniform(name);
        if (uniform != null) {
            uniform.set(a);
        }
    }

    /**
     * Releases the self-managed resources (called when the game exits).
     */
    public void delete() {
        if (this.shaderGroup != null) {
            this.shaderGroup.deleteShaderGroup();
            this.shaderGroup = null;
        }
    }
}
