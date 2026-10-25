package org.dpdns.syscrashed.phiclient.gui.render;

import org.dpdns.syscrashed.phiclient.gui.nanovg.NanoVGManager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.util.ResourceLocation;

/**
 * Renders the head of the player (package {@code org.dpdns.syscrashed.phiclient.gui.render}).
 *
 * <p>The head is not copied into a bitmap: the skin texture of Minecraft is referenced directly by a
 * NanoVG image, so the current skin of the player is always shown. Only the front of the head (the
 * 8x8 pixels at {@code (8, 8)} of the 64x64 skin) is used.</p>
 *
 * <p>The head is drawn into a <b>square</b> that is not sheared: the portrait of the player is the one
 * element of the interface that keeps the upright shape of the skin, so the head of the skin is not
 * distorted by the parallelogram of the global shape.</p>
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #getSkinTextureId()} - the OpenGL texture of the skin of the player (0 when it is not
 *       available yet).</li>
 *   <li>{@link #drawHead(NanoVGManager, float, float, float, float)} - draws the head.</li>
 * </ul>
 */
public final class PlayerHeadRenderer {

    /** The size of a Minecraft skin. */
    public static final int SKIN_SIZE = 64;

    /** The position of the front of the head inside the skin. */
    public static final int FACE_X = 8;

    /** The position of the front of the head inside the skin. */
    public static final int FACE_Y = 8;

    /** The size of the front of the head inside the skin. */
    public static final int FACE_SIZE = 8;

    private final Minecraft mc = Minecraft.getMinecraft();

    /**
     * @return the OpenGL texture id of the skin of the player (0 when the skin is not loaded yet).
     */
    public int getSkinTextureId() {
        if (this.mc.thePlayer == null) {
            return 0;
        }

        ResourceLocation skin = this.mc.thePlayer.getLocationSkin();
        if (skin == null) {
            return 0;
        }

        ITextureObject texture = this.mc.getTextureManager().getTexture(skin);
        if (texture == null) {
            return 0;
        }

        return texture.getGlTextureId();
    }

    /**
     * Draws the front of the head of the player into the global shape of the interface.
     *
     * @param nvg    the NanoVG drawing context.
     * @param x      the left x of the bounding box.
     * @param y      the top y of the bounding box.
     * @param size   the edge length of the (square) head.
     * @param radius the corner radius of the head.
     * @return true if the head was drawn, false when the skin is not available.
     */
    public boolean drawHead(NanoVGManager nvg, float x, float y, float size, float radius) {
        int glTexture = getSkinTextureId();
        if (glTexture == 0) {
            return false;
        }

        // The negative height flips the texture, because OpenGL stores the images bottom up.
        int image = nvg.getImageFromHandle(glTexture, SKIN_SIZE, -SKIN_SIZE);
        if (image == 0) {
            return false;
        }

        // The portrait is a plain square - the skin is not sheared like the rest of the interface.
        nvg.drawImageRegion(image, x, y, size, size, radius,
                FACE_X, FACE_Y, FACE_SIZE, FACE_SIZE, SKIN_SIZE, SKIN_SIZE, 1.0F);

        return true;
    }
}
