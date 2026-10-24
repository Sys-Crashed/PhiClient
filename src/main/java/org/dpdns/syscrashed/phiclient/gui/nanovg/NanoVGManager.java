package org.dpdns.syscrashed.phiclient.gui.nanovg;

import org.dpdns.syscrashed.phiclient.gui.nanovg.font.Font;
import org.dpdns.syscrashed.phiclient.gui.nanovg.font.FontManager;
import org.dpdns.syscrashed.phiclient.logger.PhiLogger;
import org.lwjgl.BufferUtils;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NVGPaint;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.nanovg.NanoVGGL2;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

/**
 * NanoVG 上下文的生命周期、GL 状态保护与常用绘制封装。
 *
 * <p>针对 MC 1.8.9 做了两处关键修正：
 *
 * <ol>
 *   <li><b>懒创建上下文</b>（{@link #ensureReady()}）：不再在 {@code Minecraft#startGame} 期间创建。
 *       NanoVG 的 GL2 后端在 {@code nvgCreate} 时会<b>把自己的着色器程序绑定上</b>，而 MC 1.8.9 的字体图集
 *       （{@code FontRenderer} 构造时生成）是用固定管线画的 —— 那时若 NanoVG 的 program 还绑着，
 *       字形图集会被画坏，整个游戏的字体都会花掉。所以必须等 GUI 真正要用的那一刻再建上下文。</li>
 *   <li><b>显式还原着色器程序</b>：{@code glPushAttrib / glPopAttrib} 的属性栈<b>不包含</b>当前 program，
 *       所以每帧绘制结束后都要把 {@code glUseProgram} 还原回原值，否则 MC 后续的固定管线渲染会串味。</li>
 * </ol>
 *
 * <p>其余细节：使用 GL2 后端（MC 1.8.9 的上下文是 GL2 级别），
 * 绘制尺寸按物理像素（displayWidth / displayHeight），再通过 {@code nvgScale} 缩放，
 * 业务代码就可以直接用 GuiScreen 的 {@code width / height} 坐标系。
 */
public final class NanoVGManager {

    /** {@code GL_CLIENT_ALL_ATTRIB_BITS}：LWJGL 2 没在 GL11 里暴露这个常量，这里按 gl.h 的定义写死。 */
    private static final int GL_CLIENT_ALL_ATTRIB_BITS = 0xFFFFFFFF;

    private final Minecraft mc = Minecraft.getMinecraft();

    private final FontManager fontManager = new FontManager();

    private long context;
    private boolean ready;
    private boolean failed;
    private boolean creating;

    /** 复用的颜色对象（避免每次绘制都分配 native 内存）。 */
    private NVGColor scratchColor;

    /** 渐变用的第二个颜色对象。 */
    private NVGColor scratchColorAlt;

    /** 渐变用的画笔对象（长期持有，不释放，理由见 drawGradientRoundedRect）。 */
    private NVGPaint scratchPaint;

    /** 复用的文本包围盒缓冲。 */
    private final java.nio.FloatBuffer boundsBuffer = BufferUtils.createFloatBuffer(4);

    /**
     * 确保 NanoVG 上下文可用；不可用时返回 false（并且只尝试失败一次）。
     *
     * <p>必须在 GL 上下文就绪、且在渲染线程上调用。
     */
    public boolean ensureReady() {
        if (this.ready) {
            return true;
        }
        if (this.failed || this.creating) {
            return false;
        }

        this.creating = true;

        int previousProgram = 0;
        int previousMatrixMode = GL11.GL_MODELVIEW;
        int previousActiveTexture = GL13.GL_TEXTURE0;

        try {
            previousProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
            previousMatrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
            previousActiveTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);

            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            GL11.glPushClientAttrib(GL_CLIENT_ALL_ATTRIB_BITS);

            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPushMatrix();
            GL11.glLoadIdentity();

            this.context = NanoVGGL2.nvgCreate(NanoVGGL2.NVG_ANTIALIAS);
            if (this.context == 0L) {
                this.failed = true;
                PhiLogger.error("Failed to create NanoVG context (nvgCreate returned NULL).");
                return false;
            }

            this.fontManager.init(this.context);

            this.ready = true;
            PhiLogger.info("NanoVG context created: 0x" + Long.toHexString(this.context));
        } catch (Throwable t) {
            this.failed = true;
            this.context = 0L;
            PhiLogger.error("Failed to initialize NanoVG", t);
            return false;
        } finally {
            this.creating = false;

            // NanoVG 会把自己的 shader program 留在绑定状态里 —— 必须还原，
            // 否则 MC 的固定管线渲染（字体图集、HUD、方块）会全部走 NanoVG 的 shader。
            GL20.glUseProgram(previousProgram);
            GL11.glPopMatrix();
            GL11.glMatrixMode(previousMatrixMode);
            GL13.glActiveTexture(previousActiveTexture);
            GL11.glPopClientAttrib();
            GL11.glPopAttrib();
            GL20.glUseProgram(previousProgram);
        }

        return this.ready;
    }

    public boolean isReady() {
        return this.ready;
    }

    public long getContext() {
        return this.context;
    }

    public void shutdown() {
        if (!this.ready) {
            return;
        }
        try {
            NanoVGGL2.nvgDelete(this.context);
            PhiLogger.info("NanoVG context destroyed.");
        } catch (Throwable t) {
            PhiLogger.error("Failed to destroy NanoVG context", t);
        }
        this.ready = false;
        this.context = 0L;
    }

    /**
     * 包一层 NanoVG 帧：保存 GL 状态 → beginFrame →（按 GUI 缩放）→ 执行任务 → endFrame → 恢复状态。
     *
     * @param task  真正的绘制逻辑（坐标用 GuiScreen 的 width/height）
     * @param scale 是否按 {@code ScaledResolution} 的缩放系数缩放（GUI 界面一律传 true）
     */
    public void setupAndDraw(Runnable task, boolean scale) {
        if (task == null || !ensureReady()) {
            return;
        }

        ScaledResolution sr = new ScaledResolution(this.mc);

        int previousProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int previousActiveTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushClientAttrib(GL_CLIENT_ALL_ATTRIB_BITS);

        try {
            NanoVG.nvgBeginFrame(this.context, this.mc.displayWidth, this.mc.displayHeight, 1.0F);

            if (scale) {
                NanoVG.nvgScale(this.context, sr.getScaleFactor(), sr.getScaleFactor());
            }

            task.run();

            // MC 可能开着 alpha test；NanoVG 的 GL2 后端不会去关它，
            // 不关的话带 alpha 的形状会被 alpha test 直接丢弃掉。
            GL11.glDisable(GL11.GL_ALPHA_TEST);

            NanoVG.nvgEndFrame(this.context);
        } catch (Throwable t) {
            PhiLogger.error("Failed to draw NanoVG frame", t);
        } finally {
            GL20.glUseProgram(previousProgram);
            GL13.glActiveTexture(previousActiveTexture);
            GL11.glPopClientAttrib();
            GL11.glPopAttrib();
        }
    }

    // ------------------------------------------------------------------ 状态

    public void save() {
        NanoVG.nvgSave(this.context);
    }

    public void restore() {
        NanoVG.nvgRestore(this.context);
    }

    public void translate(float x, float y) {
        NanoVG.nvgTranslate(this.context, x, y);
    }

    public void scale(float x, float y) {
        NanoVG.nvgScale(this.context, x, y);
    }

    public void scissor(float x, float y, float width, float height) {
        NanoVG.nvgScissor(this.context, x, y, width, height);
    }

    public void resetScissor() {
        NanoVG.nvgResetScissor(this.context);
    }

    // ------------------------------------------------------------------ 绘制

    public void drawRect(float x, float y, float width, float height, int argb) {
        NanoVG.nvgBeginPath(this.context);
        NanoVG.nvgRect(this.context, x, y, width, height);
        NanoVG.nvgFillColor(this.context, color(argb));
        NanoVG.nvgFill(this.context);
    }

    public void drawRoundedRect(float x, float y, float width, float height, float radius, int argb) {
        NanoVG.nvgBeginPath(this.context);
        NanoVG.nvgRoundedRect(this.context, x, y, width, height, radius);
        NanoVG.nvgFillColor(this.context, color(argb));
        NanoVG.nvgFill(this.context);
    }

    public void drawRoundedRectOutline(float x, float y, float width, float height, float radius, float lineWidth, int argb) {
        NanoVG.nvgBeginPath(this.context);
        NanoVG.nvgRoundedRect(this.context, x, y, width, height, radius);
        NanoVG.nvgStrokeWidth(this.context, lineWidth);
        NanoVG.nvgStrokeColor(this.context, color(argb));
        NanoVG.nvgStroke(this.context);
    }

    /**
     * 竖直线性渐变的圆角矩形（主色块 / 侧栏高亮用）。
     *
     * <p>注意：这里复用一个<b>长期持有</b>的 NVGPaint，<b>不</b>调用 {@code free()}。
     * {@code NVGPaint.create()} 之后从不释放。
     * 原因：LWJGL 3 的 Struct 体系里 native 内存与它镜像用的 direct buffer 生命周期是绑定的，
     * 手动 {@code free()} 之后这块内存会被释放两次 → Windows 上直接变成堆损坏
     * （{@code 0xC0000374 / STATUS_HEAP_CORRUPTION}），表现为「一开界面就崩」。
     */
    public void drawGradientRoundedRect(float x, float y, float width, float height, float radius, int from, int to) {
        if (this.scratchPaint == null) {
            this.scratchPaint = NVGPaint.create();
        }

        NanoVG.nvgBeginPath(this.context);
        NanoVG.nvgRoundedRect(this.context, x, y, width, height, radius);

        NanoVG.nvgLinearGradient(this.context, x, y, x, y + height, color(from), colorAlt(to), this.scratchPaint);
        NanoVG.nvgFillPaint(this.context, this.scratchPaint);
        NanoVG.nvgFill(this.context);
    }

    /**
     * 绘制一行文本。坐标是文本左上角（{@code NVG_ALIGN_LEFT | NVG_ALIGN_TOP}）。
     */
    public void drawText(String text, float x, float y, int argb, float size, Font font) {
        if (text == null || text.isEmpty() || font == null || !font.isLoaded()) {
            return;
        }

        NanoVG.nvgFontFace(this.context, font.getName());
        NanoVG.nvgFontSize(this.context, size);
        NanoVG.nvgTextAlign(this.context, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_TOP);
        NanoVG.nvgFillColor(this.context, color(argb));
        NanoVG.nvgText(this.context, x, y, text);
    }

    /**
     * 文本宽度（像素）。
     */
    public float textWidth(String text, float size, Font font) {
        if (text == null || text.isEmpty() || font == null || !font.isLoaded()) {
            return 0.0F;
        }

        NanoVG.nvgFontFace(this.context, font.getName());
        NanoVG.nvgFontSize(this.context, size);
        NanoVG.nvgTextBounds(this.context, 0.0F, 0.0F, text, this.boundsBuffer);
        return this.boundsBuffer.get(2) - this.boundsBuffer.get(0);
    }

    /**
     * ARGB（0xAARRGGBB）→ NanoVG 颜色。
     */
    private NVGColor color(int argb) {
        if (this.scratchColor == null) {
            this.scratchColor = NVGColor.create();
        }
        return fill(this.scratchColor, argb);
    }

    /**
     * 渐变的第二个端点色（与 {@link #color(int)} 用不同的缓冲，避免互相覆盖）。
     */
    private NVGColor colorAlt(int argb) {
        if (this.scratchColorAlt == null) {
            this.scratchColorAlt = NVGColor.create();
        }
        return fill(this.scratchColorAlt, argb);
    }

    private static NVGColor fill(NVGColor target, int argb) {
        NanoVG.nvgRGBA(
                (byte) ((argb >> 16) & 0xFF),
                (byte) ((argb >> 8) & 0xFF),
                (byte) (argb & 0xFF),
                (byte) ((argb >> 24) & 0xFF),
                target);
        return target;
    }
}
