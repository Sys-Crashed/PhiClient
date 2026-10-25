package org.dpdns.syscrashed.phiclient.gui.nanovg;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import org.dpdns.syscrashed.phiclient.PhiClient;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiSkew;
import org.dpdns.syscrashed.phiclient.gui.nanovg.font.Font;
import org.dpdns.syscrashed.phiclient.gui.nanovg.font.FontManager;
import org.dpdns.syscrashed.phiclient.logger.PhiLogger;
import org.lwjgl.BufferUtils;
import org.lwjgl.nanovg.NSVGImage;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NVGPaint;
import org.lwjgl.nanovg.NanoSVG;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.nanovg.NanoVGGL2;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

/**
 * Lifecycle, GL-state protection and common drawing helpers for the NanoVG context
 * (package {@code org.dpdns.syscrashed.phiclient.gui.nanovg}).
 *
 * <p>Two critical fixes for MC 1.8.9:</p>
 * <ol>
 *   <li><b>Lazy context creation</b> ({@link #ensureReady()}): not created during {@code Minecraft#startGame}.
 *       NanoVG's GL2 backend <b>binds its own shader program</b> in {@code nvgCreate}, while MC 1.8.9's font
 *       atlas (built in {@code FontRenderer}'s constructor) uses the fixed pipeline - if NanoVG's program
 *       is still bound then, the glyph atlas is corrupted and every font in the game is garbled. So the
 *       context must be created only at the moment the GUI actually needs it.</li>
 *   <li><b>Explicit shader-program restore</b>: {@code glPushAttrib / glPopAttrib} does <b>not</b> include the
 *       current program, so after each frame we must restore {@code glUseProgram} to its previous value,
 *       otherwise MC's subsequent fixed-pipeline rendering bleeds into NanoVG's shader.</li>
 * </ol>
 *
 * <p>Other details: the GL2 backend is used (MC 1.8.9's context is GL2 level); drawing is sized in
 * physical pixels (displayWidth / displayHeight) then scaled via {@code nvgScale}, so business code can
 * use the GuiScreen {@code width / height} coordinate space directly.</p>
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #ensureReady()} - lazily creates the context (once).</li>
 *   <li>{@link #isReady()} / {@link #getContext()} - context state accessors.</li>
 *   <li>{@link #shutdown()} - deletes the context.</li>
 *   <li>{@link #setupAndDraw(Runnable, boolean)} - wraps a frame: save GL state -&gt; beginFrame -&gt; (scale) -&gt; task -&gt; endFrame -&gt; restore.</li>
 *   <li>{@link #save()} / {@link #restore()} / {@link #translate(float, float)} / {@link #scale(float, float)} / {@link #skewX(float)} / {@link #scissor(float, float, float, float)} / {@link #resetScissor()} - state helpers.</li>
 *   <li>{@link #drawRect(...)} / {@link #drawRoundedRect(...)} / {@link #drawRoundedRectOutline(...)} / {@link #drawGradientRoundedRect(...)} - shape drawing.</li>
 *   <li>{@link #drawSkewRect(...)} / {@link #drawSkewRectGradient(...)} - the global rounded parallelogram shape of the UI.</li>
 *   <li>{@link #beginPath()} / {@link #moveTo(float, float)} / {@link #lineTo(float, float)} / {@link #closePath()} /
 *       {@link #fillPath(int)} / {@link #strokePath(int, float)} / {@link #drawLine(...)} / {@link #drawCircle(...)} - arbitrary vector drawing.</li>
 *   <li>{@link #drawText(...)} / {@link #drawTextAligned(...)} / {@link #drawTextCentered(...)} / {@link #textWidth(...)} - text drawing and measurement.</li>
 *   <li>{@link #limitText(String, float, Font, float)} - shortens a text to a given width.</li>
 * </ul>
 */
public final class NanoVGManager {

    /** {@code GL_CLIENT_ALL_ATTRIB_BITS}: LWJGL 2 does not expose this constant in GL11, so it is hard-coded from gl.h. */
    private static final int GL_CLIENT_ALL_ATTRIB_BITS = 0xFFFFFFFF;

    private final Minecraft mc = Minecraft.getMinecraft();

    private final FontManager fontManager = new FontManager();

    private long context;
    private boolean ready;
    private boolean failed;
    private boolean creating;

    /** Reused color object (avoids allocating native memory on every draw). */
    private NVGColor scratchColor;

    /** Second color object used for gradients. */
    private NVGColor scratchColorAlt;

    /** Reused gradient paint (kept alive and never freed; see {@link #drawGradientRoundedRect}). */
    private NVGPaint scratchPaint;

    /** Reused paint for image patterns (kept alive and never freed, see {@link #drawImageRegion}). */
    private NVGPaint scratchImagePaint;

    /** The NanoVG images that were created from OpenGL textures, keyed by their texture id. */
    private final java.util.Map<Integer, Integer> imageCache = new java.util.HashMap<Integer, Integer>();

    /** The NanoVG images that were rasterized from SVG resources. */
    private final java.util.Map<String, Integer> svgCache = new java.util.HashMap<String, Integer>();

    /** Reused text bounds buffer. */
    private final java.nio.FloatBuffer boundsBuffer = BufferUtils.createFloatBuffer(4);

    /**
     * Ensures the NanoVG context is available; returns false if it cannot be created (and only attempts
     * creation once).
     *
     * <p>Must be called on the render thread, after the GL context is ready.</p>
     *
     * @return true if the context is (now) usable.
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

            // NanoVG leaves its shader program bound - must restore it, otherwise MC's fixed-pipeline
            // rendering (font atlas, HUD, blocks) would all run through NanoVG's shader.
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

    /**
     * @return true if the NanoVG context has been created successfully.
     */
    public boolean isReady() {
        return this.ready;
    }

    /**
     * @return the native NanoVG context handle (0 if not created).
     */
    public long getContext() {
        return this.context;
    }

    /**
     * Deletes the NanoVG context (called on shutdown).
     */
    public void shutdown() {
        if (!this.ready) {
            return;
        }
        try {
            for (Integer image : this.imageCache.values()) {
                deleteImage(image);
            }
            this.imageCache.clear();

            for (Integer image : this.svgCache.values()) {
                deleteImage(image);
            }
            this.svgCache.clear();

            NanoVGGL2.nvgDelete(this.context);
            PhiLogger.info("NanoVG context destroyed.");
        } catch (Throwable t) {
            PhiLogger.error("Failed to destroy NanoVG context", t);
        }
        this.ready = false;
        this.context = 0L;
    }

    /**
     * Wraps a NanoVG frame: save GL state -&gt; beginFrame -&gt; (optional scale by GuiScreen factor) -&gt;
     * run the task -&gt; endFrame -&gt; restore GL state.
     *
     * @param task  the actual drawing logic (coordinates use the GuiScreen width/height space).
     * @param scale whether to scale by the {@link ScaledResolution} factor (GUI screens always pass true).
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

            // MC may have alpha test enabled; NanoVG's GL2 backend does not disable it, and without doing so
            // alpha-tested shapes would be discarded.
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

    // ------------------------------------------------------------------ state

    /** Saves the current NanoVG state (push). */
    public void save() {
        NanoVG.nvgSave(this.context);
    }

    /** Restores the most recently saved NanoVG state (pop). */
    public void restore() {
        NanoVG.nvgRestore(this.context);
    }

    /** Translates the current NanoVG coordinate system. */
    public void translate(float x, float y) {
        NanoVG.nvgTranslate(this.context, x, y);
    }

    /** Scales the current NanoVG coordinate system. */
    public void scale(float x, float y) {
        NanoVG.nvgScale(this.context, x, y);
    }

    /**
     * Skews the current NanoVG coordinate system along the x axis.
     *
     * <p>Used to build the global shape of the interface: a rounded rectangle that is sheared by
     * {@link PhiSkew#SHEAR_DEGREES} becomes a parallelogram with rounded corners.</p>
     *
     * @param degrees the shear angle in degrees.
     */
    public void skewX(float degrees) {
        NanoVG.nvgSkewX(this.context, (float) Math.toRadians(degrees));
    }

    /** Sets a scissor rectangle (clips subsequent drawing). */
    public void scissor(float x, float y, float width, float height) {
        NanoVG.nvgScissor(this.context, x, y, width, height);
    }

    /** Removes the scissor rectangle. */
    public void resetScissor() {
        NanoVG.nvgResetScissor(this.context);
    }

    // ------------------------------------------------------------------ drawing

    /** Draws a filled axis-aligned rectangle. */
    public void drawRect(float x, float y, float width, float height, int argb) {
        NanoVG.nvgBeginPath(this.context);
        NanoVG.nvgRect(this.context, x, y, width, height);
        NanoVG.nvgFillColor(this.context, color(argb));
        NanoVG.nvgFill(this.context);
    }

    /** Draws a filled rounded rectangle. */
    public void drawRoundedRect(float x, float y, float width, float height, float radius, int argb) {
        NanoVG.nvgBeginPath(this.context);
        NanoVG.nvgRoundedRect(this.context, x, y, width, height, radius);
        NanoVG.nvgFillColor(this.context, color(argb));
        NanoVG.nvgFill(this.context);
    }

    /** Draws the outline of a rounded rectangle. */
    public void drawRoundedRectOutline(float x, float y, float width, float height, float radius, float lineWidth, int argb) {
        NanoVG.nvgBeginPath(this.context);
        NanoVG.nvgRoundedRect(this.context, x, y, width, height, radius);
        NanoVG.nvgStrokeWidth(this.context, lineWidth);
        NanoVG.nvgStrokeColor(this.context, color(argb));
        NanoVG.nvgStroke(this.context);
    }

    /** Draws a filled rounded rectangle with an individual radius for every corner. */
    public void drawRoundedRectVarying(float x, float y, float width, float height, float topLeft, float topRight, float bottomRight, float bottomLeft, int argb) {
        NanoVG.nvgBeginPath(this.context);
        NanoVG.nvgRoundedRectVarying(this.context, x, y, width, height, topLeft, topRight, bottomRight, bottomLeft);
        NanoVG.nvgFillColor(this.context, color(argb));
        NanoVG.nvgFill(this.context);
    }

    /**
     * Draws a vertically-linear-gradient rounded rectangle (accent blocks / sidebar highlights).
     *
     * <p>Note: a <b>long-lived</b> {@link NVGPaint} is reused here and {@code free()} is <b>never</b> called.
     * After {@code NVGPaint.create()} it is never freed. Reason: in LWJGL 3's Struct system the native
     * memory and its mirror direct buffer share a lifetime, so a manual {@code free()} would release the
     * memory twice -&gt; Windows heap corruption ({@code 0xC0000374 / STATUS_HEAP_CORRUPTION}),
     * i.e. "the GUI crashes the game on open".</p>
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

    // ------------------------------------------------------------------ paths

    /** Begins a new (empty) path. */
    public void beginPath() {
        NanoVG.nvgBeginPath(this.context);
    }

    /** Appends a move-to command to the current path. */
    public void moveTo(float x, float y) {
        NanoVG.nvgMoveTo(this.context, x, y);
    }

    /** Appends a line-to command to the current path. */
    public void lineTo(float x, float y) {
        NanoVG.nvgLineTo(this.context, x, y);
    }

    /** Closes the current path. */
    public void closePath() {
        NanoVG.nvgClosePath(this.context);
    }

    /**
     * Appends a rounded rectangle to the current path. The rectangle is <b>not</b> filled - use it
     * together with {@link #fillPath(int)} or {@link #fillPathRadialGradient(float, float, float, int, int)}
     * to draw it.
     *
     * @param x      the left x of the rectangle.
     * @param y      the top y of the rectangle.
     * @param width  the width of the rectangle.
     * @param height the height of the rectangle.
     * @param radius the corner radius of the rectangle.
     */
    public void addRoundedRect(float x, float y, float width, float height, float radius) {
        NanoVG.nvgRoundedRect(this.context, x, y, width, height, radius);
    }

    /** Appends an arc (circle segment) to the current path. */
    public void arc(float centerX, float centerY, float radius, float startAngle, float endAngle, int direction) {
        NanoVG.nvgArc(this.context, centerX, centerY, radius, startAngle, endAngle, direction);
    }

    /** Fills the current path with a solid color. */
    public void fillPath(int argb) {
        NanoVG.nvgFillColor(this.context, color(argb));
        NanoVG.nvgFill(this.context);
    }

    /**
     * Fills the current path with a radial gradient. The gradient is centered on the given point: the
     * inner color is at its center, the outer color at the given radius. It is used for the hover
     * highlight, which spreads from the position of the pointer.
     *
     * @param centerX the horizontal center of the gradient.
     * @param centerY the vertical center of the gradient.
     * @param radius  the radius of the gradient.
     * @param inner   the color at the center of the gradient.
     * @param outer   the color at the radius of the gradient.
     */
    public void fillPathRadialGradient(float centerX, float centerY, float radius, int inner, int outer) {
        if (this.scratchPaint == null) {
            this.scratchPaint = NVGPaint.create();
        }

        // The trimmed NanoVG build expects the inner and the outer radius of the gradient.
        NanoVG.nvgRadialGradient(this.context, centerX, centerY, radius, radius,
                color(inner), colorAlt(outer), this.scratchPaint);
        NanoVG.nvgFillPaint(this.context, this.scratchPaint);
        NanoVG.nvgFill(this.context);
    }

    /** Fills the current path with a linear gradient (the paint is reused and never freed). */
    public void fillPathGradient(int from, int to, float x1, float y1, float x2, float y2) {
        if (this.scratchPaint == null) {
            this.scratchPaint = NVGPaint.create();
        }

        NanoVG.nvgLinearGradient(this.context, x1, y1, x2, y2, color(from), colorAlt(to), this.scratchPaint);
        NanoVG.nvgFillPaint(this.context, this.scratchPaint);
        NanoVG.nvgFill(this.context);
    }

    /** Strokes the current path with the given line width. */
    public void strokePath(int argb, float lineWidth) {
        NanoVG.nvgStrokeWidth(this.context, lineWidth);
        NanoVG.nvgStrokeColor(this.context, color(argb));
        NanoVG.nvgStroke(this.context);
    }

    /** Draws a filled circle. */
    public void drawCircle(float centerX, float centerY, float radius, int argb) {
        beginPath();
        arc(centerX, centerY, radius, 0.0F, (float) (Math.PI * 2.0D), NanoVG.NVG_CW);
        fillPath(argb);
    }

    /** Draws a straight line. */
    public void drawLine(float x1, float y1, float x2, float y2, int argb, float lineWidth) {
        beginPath();
        moveTo(x1, y1);
        lineTo(x2, y2);
        strokePath(argb, lineWidth);
    }

    // ------------------------------------------------------------------ shapes

    /**
     * Draws the global shape of the interface: a parallelogram whose adjacent angles are 75 / 105
     * degrees and whose top edge is shifted to the right by {@code height / tan(75 degrees)}.
     *
     * <p>The shape is built by shearing a rounded rectangle, so the corners are rounded as well. The
     * radius is measured in the un-sheared coordinate system of the shape.</p>
     *
     * @param x      the left x of the bounding box.
     * @param y      the top y of the bounding box.
     * @param width  the width of the bounding box.
     * @param height the height of the bounding box.
     * @param radius the corner radius of the shape.
     * @param argb   the fill color.
     */
    public void drawSkewRect(float x, float y, float width, float height, float radius, int argb) {
        float offset = PhiSkew.offset(height);

        save();
        translate(x + offset, y);
        skewX(PhiSkew.SHEAR_DEGREES);
        drawRoundedRect(0.0F, 0.0F, width, height, radius, argb);
        restore();
    }

    /**
     * Draws the global shape of the interface filled with a vertical linear gradient.
     *
     * @param x      the left x of the bounding box.
     * @param y      the top y of the bounding box.
     * @param width  the width of the bounding box.
     * @param height the height of the bounding box.
     * @param radius the corner radius of the shape.
     * @param from   the color at the top.
     * @param to     the color at the bottom.
     */
    public void drawSkewRectGradient(float x, float y, float width, float height, float radius, int from, int to) {
        float offset = PhiSkew.offset(height);

        save();
        translate(x + offset, y);
        skewX(PhiSkew.SHEAR_DEGREES);
        beginPath();
        NanoVG.nvgRoundedRect(this.context, 0.0F, 0.0F, width, height, radius);
        fillPathGradient(from, to, 0.0F, 0.0F, 0.0F, height);
        restore();
    }

    /**
     * Draws the global shape of the interface filled with a diagonal linear gradient.
     *
     * <p>The gradient runs from the upper left corner to the lower right corner of the shape. It is
     * built in the sheared coordinate system of the shape, so the two ends of the gradient follow the
     * slant of the interface instead of the axes of the screen.</p>
     *
     * @param x      the left x of the bounding box.
     * @param y      the top y of the bounding box.
     * @param width  the width of the bounding box.
     * @param height the height of the bounding box.
     * @param radius the corner radius of the shape.
     * @param from   the color at the upper left corner.
     * @param to     the color at the lower right corner.
     */
    public void drawSkewRectGradientDiagonal(float x, float y, float width, float height, float radius,
                                             int from, int to) {
        float offset = PhiSkew.offset(height);

        save();
        translate(x + offset, y);
        skewX(PhiSkew.SHEAR_DEGREES);
        beginPath();
        NanoVG.nvgRoundedRect(this.context, 0.0F, 0.0F, width, height, radius);
        fillPathGradient(from, to, 0.0F, 0.0F, width, height);
        restore();
    }

    /**
     * Sets the global alpha that is applied to every subsequent draw call.
     *
     * @param alpha the alpha value in the range {@code [0, 1]}.
     */
    public void setGlobalAlpha(float alpha) {
        NanoVG.nvgGlobalAlpha(this.context, Math.max(0.0F, Math.min(1.0F, alpha)));
    }

    /** Resets the global alpha back to 1. */
    public void resetGlobalAlpha() {
        NanoVG.nvgGlobalAlpha(this.context, 1.0F);
    }

    // ------------------------------------------------------------------ images

    /**
     * Creates a NanoVG image that wraps an existing OpenGL texture - this is how the skin of the
     * player is displayed (the texture of the skin is never copied, it is only referenced).
     *
     * @param glTexture the OpenGL texture id.
     * @param width     the width of the texture.
     * @param height    the height of the texture (a negative value flips the image vertically, because
     *                  OpenGL stores the textures bottom up).
     * @return the NanoVG image handle (0 on failure).
     */
    public int createImageFromHandle(int glTexture, int width, int height) {
        if (glTexture == 0) {
            return 0;
        }

        return NanoVGGL2.nvglCreateImageFromHandle(this.context, glTexture, width, height, 0);
    }

    /**
     * Returns the NanoVG image of an OpenGL texture and creates it on the first request. The images
     * are cached until the NanoVG context is destroyed.
     *
     * @param glTexture the OpenGL texture id.
     * @param width     the width of the texture.
     * @param height    the height of the texture (negative values flip the image).
     * @return the NanoVG image handle (0 on failure).
     */
    public int getImageFromHandle(int glTexture, int width, int height) {
        if (glTexture == 0) {
            return 0;
        }

        Integer cached = this.imageCache.get(glTexture);
        if (cached != null) {
            return cached;
        }

        int image = createImageFromHandle(glTexture, width, height);
        if (image != 0) {
            this.imageCache.put(glTexture, image);
        }

        return image;
    }

    /**
     * Deletes a NanoVG image that was created by {@link #createImageFromHandle(int, int, int)}.
     *
     * @param image the image handle.
     */
    public void deleteImage(int image) {
        if (image != 0) {
            NanoVG.nvgDeleteImage(this.context, image);
        }
    }

    /**
     * Draws an image into a rectangle.
     *
     * @param image  the NanoVG image handle.
     * @param x      the left x of the target rectangle.
     * @param y      the top y of the target rectangle.
     * @param width  the width of the target rectangle.
     * @param height the height of the target rectangle.
     * @param alpha  the alpha of the image.
     */
    public void drawImage(int image, float x, float y, float width, float height, float alpha) {
        if (image == 0 || width <= 0.0F || height <= 0.0F) {
            return;
        }

        if (this.scratchImagePaint == null) {
            this.scratchImagePaint = NVGPaint.create();
        }

        NanoVG.nvgBeginPath(this.context);
        NanoVG.nvgRect(this.context, x, y, width, height);
        NanoVG.nvgImagePattern(this.context, x, y, width, height, 0, image, alpha, this.scratchImagePaint);
        NanoVG.nvgFillPaint(this.context, this.scratchImagePaint);
        NanoVG.nvgFill(this.context);
    }

    /**
     * Rasterizes an SVG resource of the class path into a NanoVG image and returns its handle. The
     * shapes of the SVG are painted with the given color; the image is cached until the NanoVG
     * context is destroyed.
     *
     * @param resourcePath the absolute path of the resource (for example {@code /assets/x/y.svg}).
     * @param size         the edge length the SVG is rasterized with.
     * @param tint         the ARGB color the shapes of the SVG are painted with.
     * @return the NanoVG image handle (0 on failure).
     */
    public int getSvgImage(String resourcePath, float size, int tint) {
        String key = resourcePath + "-" + (int) size + "-" + tint;

        Integer cached = this.svgCache.get(key);
        if (cached != null) {
            return cached;
        }

        String svg = readResource(resourcePath);
        if (svg == null) {
            return 0;
        }

        NSVGImage parsed = NanoSVG.nsvgParse(svg, "px", 96.0F);
        if (parsed == null) {
            return 0;
        }

        float scale = size / Math.max(1.0F, Math.max(parsed.width(), parsed.height()));
        int width = Math.max(1, (int) (parsed.width() * scale));
        int height = Math.max(1, (int) (parsed.height() * scale));

        ByteBuffer pixels = MemoryUtil.memAlloc(width * height * 4);
        int image = 0;

        try {
            long rasterizer = NanoSVG.nsvgCreateRasterizer();
            NanoSVG.nsvgRasterize(rasterizer, parsed, 0.0F, 0.0F, scale, pixels, width, height, width * 4);

            tint(pixels, width, height, tint);

            image = NanoVG.nvgCreateImageRGBA(this.context, width, height, 0, pixels);
        } catch (Throwable t) {
            PhiLogger.error("Failed to rasterize the SVG " + resourcePath, t);
            image = 0;
        } finally {
            MemoryUtil.memFree(pixels);
            NanoSVG.nsvgDelete(parsed);
        }

        if (image != 0) {
            this.svgCache.put(key, image);
        }

        return image;
    }

    /**
     * Reads a text resource of the class path.
     *
     * @param resourcePath the absolute path of the resource.
     * @return the content of the resource (null when it does not exist).
     */
    private String readResource(String resourcePath) {
        InputStream stream = PhiClient.class.getResourceAsStream(resourcePath);
        if (stream == null) {
            PhiLogger.error("Missing resource: " + resourcePath);
            return null;
        }

        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;

            while ((read = stream.read(buffer)) > 0) {
                output.write(buffer, 0, read);
            }

            return new String(output.toByteArray(), "UTF-8");
        } catch (Throwable t) {
            PhiLogger.error("Failed to read the resource " + resourcePath, t);
            return null;
        } finally {
            try {
                stream.close();
            } catch (IOException ignored) {
                // Nothing to do here.
            }
        }
    }

    /**
     * Paints every visible pixel of an image with a single color.
     *
     * @param pixels the RGBA pixels of the image.
     * @param width  the width of the image.
     * @param height the height of the image.
     * @param color  the ARGB color.
     */
    private static void tint(ByteBuffer pixels, int width, int height, int color) {
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;

        for (int i = 0; i < width * height; i++) {
            int index = i * 4;
            int alpha = pixels.get(index + 3) & 0xFF;

            if (alpha == 0) {
                continue;
            }

            pixels.put(index, (byte) red);
            pixels.put(index + 1, (byte) green);
            pixels.put(index + 2, (byte) blue);
        }
    }

    /**
     * Draws a region of an image into a rounded rectangle. The region is given in texture pixels, so
     * a single texture can be used to show several parts of it (for example the face of a skin).
     *
     * @param image         the NanoVG image handle.
     * @param x            the left x of the target rectangle.
     * @param y            the top y of the target rectangle.
     * @param width        the width of the target rectangle.
     * @param height       the height of the target rectangle.
     * @param radius       the corner radius of the target rectangle.
     * @param regionX      the left x of the region inside the texture.
     * @param regionY      the top y of the region inside the texture.
     * @param regionWidth  the width of the region inside the texture.
     * @param regionHeight the height of the region inside the texture.
     * @param textureWidth the width of the whole texture.
     * @param textureHeight the height of the whole texture.
     * @param alpha        the alpha of the image.
     */
    public void drawImageRegion(int image, float x, float y, float width, float height, float radius,
                                float regionX, float regionY, float regionWidth, float regionHeight,
                                float textureWidth, float textureHeight, float alpha) {
        if (image == 0 || width <= 0.0F || height <= 0.0F || regionWidth <= 0.0F || regionHeight <= 0.0F) {
            return;
        }

        if (this.scratchImagePaint == null) {
            this.scratchImagePaint = NVGPaint.create();
        }

        // The pattern covers the whole texture; the region is placed inside it.
        float scaleX = width / regionWidth;
        float scaleY = height / regionHeight;
        float patternX = x - regionX * scaleX;
        float patternY = y - regionY * scaleY;
        float patternWidth = textureWidth * scaleX;
        float patternHeight = textureHeight * scaleY;

        NanoVG.nvgBeginPath(this.context);
        NanoVG.nvgRoundedRect(this.context, x, y, width, height, radius);
        NanoVG.nvgImagePattern(this.context, patternX, patternY, patternWidth, patternHeight, 0, image, alpha,
                this.scratchImagePaint);
        NanoVG.nvgFillPaint(this.context, this.scratchImagePaint);
        NanoVG.nvgFill(this.context);
    }

    /**
     * Draws a line of text. The coordinate is the text's top-left corner
     * ({@code NVG_ALIGN_LEFT | NVG_ALIGN_TOP}).
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
     * Draws a line of text with an explicit alignment.
     *
     * @param text  the text to draw.
     * @param x     the anchor x.
     * @param y     the anchor y.
     * @param align the NanoVG alignment flags (for example {@code NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE}).
     * @param argb  the text color.
     * @param size  the font size.
     * @param font  the font to use.
     */
    public void drawTextAligned(String text, float x, float y, int align, int argb, float size, Font font) {
        if (text == null || text.isEmpty() || font == null || !font.isLoaded()) {
            return;
        }

        NanoVG.nvgFontFace(this.context, font.getName());
        NanoVG.nvgFontSize(this.context, size);
        NanoVG.nvgTextAlign(this.context, align);
        NanoVG.nvgFillColor(this.context, color(argb));
        NanoVG.nvgText(this.context, x, y, text);
    }

    /**
     * Draws a line of text centered on the given x position.
     *
     * @param text     the text to draw.
     * @param centerX  the horizontal center of the text.
     * @param y        the top y of the text.
     * @param argb     the text color.
     * @param size     the font size.
     * @param font     the font to use.
     */
    public void drawTextCentered(String text, float centerX, float y, int argb, float size, Font font) {
        drawTextAligned(text, centerX, y, NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_TOP, argb, size, font);
    }

    /**
     * @return the rendered pixel width of {@code text} at the given size.
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
     * Shortens a text so that it fits into the given width.
     *
     * <p>Every text of the interface is limited to the width of its content column, so a long title can
     * never overflow the card it belongs to. A text that does not fit is cut and an ellipsis is
     * appended - the same behaviour the Soar client uses.</p>
     *
     * @param text     the text to shorten.
     * @param size     the font size.
     * @param font     the font to measure the text with.
     * @param maxWidth the maximum width of the result.
     * @return the shortened text (empty when nothing fits).
     */
    public String limitText(String text, float size, Font font, float maxWidth) {
        if (text == null || text.isEmpty() || maxWidth <= 0.0F) {
            return "";
        }

        if (textWidth(text, size, font) <= maxWidth) {
            return text;
        }

        String suffix = "...";
        float suffixWidth = textWidth(suffix, size, font);

        if (suffixWidth > maxWidth) {
            return "";
        }

        // Binary search for the longest prefix that still fits together with the ellipsis.
        int low = 0;
        int high = text.length();

        while (low < high) {
            int middle = (low + high + 1) >>> 1;

            if (textWidth(text.substring(0, middle), size, font) + suffixWidth <= maxWidth) {
                low = middle;
            } else {
                high = middle - 1;
            }
        }

        return text.substring(0, low) + suffix;
    }

    /**
     * ARGB (0xAARRGGBB) -&gt; NanoVG color.
     */
    private NVGColor color(int argb) {
        if (this.scratchColor == null) {
            this.scratchColor = NVGColor.create();
        }
        return fill(this.scratchColor, argb);
    }

    /**
     * The gradient's second end-point color (uses a separate buffer from {@link #color(int)} to avoid clobbering).
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
