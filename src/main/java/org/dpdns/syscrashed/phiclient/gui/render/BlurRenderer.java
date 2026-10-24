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
 * 背景虚化：用 Minecraft 自带的 shader 管线做一次「水平 + 垂直」两趟高斯模糊。
 *
 * <p>完全对齐原 SoarClient 的 {@code utils.render.BlurUtils}：
 * <ul>
 *   <li>shader 是 {@code assets/minecraft/shaders/post/blurArea.json}（仓库里已随 Soar 资源一起保留下来），
 *       内部两个 pass 分别做横向与纵向模糊；</li>
 *   <li>{@code BlurXY / BlurCoord} 决定模糊区域，{@code Radius} 决定强度；
 *       这几个 uniform 在 1.8.9 的 ShaderGroup 里没有公开 setter，所以通过
 *       {@link ShaderGroupAccess}（按类型反射拿到 shader 列表）逐个设置；</li>
 *   <li>模糊结果直接写回 MC 主帧缓冲，最后把主帧缓冲重新 bind 回去。</li>
 * </ul>
 *
 * <p>相比自己写 FBO + 高斯 pass 的方案，走 MC 的 ShaderGroup 有两个好处：
 * 帧缓冲链由 MC 自己管理（不会漏掉 viewport / 纹理过滤的还原），
 * 并且不会去动 MC 的 {@code GlStateManager} 缓存。
 */
public final class BlurRenderer {

    /** 模糊后处理脚本。 */
    private static final ResourceLocation BLUR_SHADER = new ResourceLocation("shaders/post/blurArea.json");

    private final Minecraft mc = Minecraft.getMinecraft();

    private ShaderGroup shaderGroup;
    private int lastWidth = -1;
    private int lastHeight = -1;
    private boolean disabled;

    /**
     * 对当前主帧缓冲做一次虚化（结果直接写回主帧缓冲）。
     * 任何一步失败都会把自己永久关掉并只报一次错，避免每帧刷屏 / 连带把游戏搞崩。
     *
     * @param partialTicks 渲染插值系数
     * @param radius       模糊半径
     */
    public void render(float partialTicks, float radius) {
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

                // 必须调用：1.8.9 里只有 createBindFramebuffers 才会
                //   1) resetProjectionMatrix() 建出投影矩阵；
                //   2) 把投影矩阵分发给每一个 shader。
                // 少了这一步，shader 里的 ProjMat uniform 会拿到 null 的 Matrix4f，
                // Shader.loadShader 里直接抛 NullPointerException（Soar 的 BlurUtils 同样有这一步）。
                this.shaderGroup.createBindFramebuffers(this.mc.displayWidth, this.mc.displayHeight);

                this.lastWidth = this.mc.displayWidth;
                this.lastHeight = this.mc.displayHeight;
            }

            applyUniforms(radius);

            this.shaderGroup.loadShaderGroup(partialTicks);

            this.mc.getFramebuffer().bindFramebuffer(true);
        } catch (Throwable t) {
            this.disabled = true;
            PhiLogger.error("Blur failed and has been disabled", t);
        }
    }

    /**
     * 设置两个 pass 的 uniform。与 Soar 的取值完全一致：
     * BlurXY 是模糊区域的起点，BlurCoord 是区域大小（都按「半分辨率」折算过），
     * 最终效果是整屏模糊。
     */
    private void applyUniforms(float radius) {
        ScaledResolution sr = new ScaledResolution(this.mc);

        float scale = sr.getScaleFactor();
        int scaledWidth = sr.getScaledWidth();
        int scaledHeight = sr.getScaledHeight();
        int x = 0;
        int y = 0;
        int width = scaledWidth;
        int height = scaledHeight;
        // 下面的算式刻意与 Soar 的 BlurUtils 保持逐项一致（即使 height 目前恒等于 scaledHeight），
        // 这样模糊区域与 Soar 的表现完全相同，将来改成局部模糊时也不用重算。

        List<Shader> shaders = ShaderGroupAccess.getShaders(this.shaderGroup);
        if (shaders == null) {
            return;
        }

        for (Shader shader : shaders) {
            ShaderManager manager = shader.getShaderManager();
            if (manager == null) {
                continue;
            }

            setUniform(manager, "BlurXY", x * (scale / 2.0F), (scaledHeight - height) * (scale / 2.0F));
            setUniform(manager, "BlurCoord", (width - x) * (scale / 2.0F), (height - y) * (scale / 2.0F));
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
     * 释放自建资源（游戏退出时调用）。
     */
    public void delete() {
        if (this.shaderGroup != null) {
            this.shaderGroup.deleteShaderGroup();
            this.shaderGroup = null;
        }
    }
}
