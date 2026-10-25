package org.dpdns.syscrashed.phiclient.gui.icon;

import org.dpdns.syscrashed.phiclient.gui.nanovg.NanoVGManager;
import org.lwjgl.nanovg.NanoVG;

/**
 * The vector icons used by the PhiClient navigation bar
 * (package {@code org.dpdns.syscrashed.phiclient.gui.icon}).
 *
 * <p>Every icon is a pure vector drawing - there is no image resource involved. The icons are built
 * from strokes and filled shapes so that they inherit the color of the current state (dim by default,
 * accent when the entry is selected).</p>
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #draw(NanoVGManager, float, float, float, int)} - renders the icon (implemented by every
 *       constant).</li>
 * </ul>
 */
public enum PhiIcons implements PhiIcon {

    /** Home - a simple house outline. */
    HOME {
        @Override
        public void draw(NanoVGManager nvg, float centerX, float centerY, float size, int color) {
            float w = size * 0.085F;

            // Roof.
            nvg.beginPath();
            nvg.moveTo(centerX - size * 0.46F, centerY - size * 0.02F);
            nvg.lineTo(centerX, centerY - size * 0.46F);
            nvg.lineTo(centerX + size * 0.46F, centerY - size * 0.02F);
            nvg.strokePath(color, w);

            // Body.
            nvg.beginPath();
            nvg.moveTo(centerX - size * 0.28F, centerY - size * 0.12F);
            nvg.lineTo(centerX - size * 0.28F, centerY + size * 0.40F);
            nvg.lineTo(centerX + size * 0.28F, centerY + size * 0.40F);
            nvg.lineTo(centerX + size * 0.28F, centerY - size * 0.12F);
            nvg.closePath();
            nvg.strokePath(color, w);

            // Door.
            nvg.beginPath();
            nvg.moveTo(centerX - size * 0.09F, centerY + size * 0.40F);
            nvg.lineTo(centerX - size * 0.09F, centerY + size * 0.14F);
            nvg.lineTo(centerX + size * 0.09F, centerY + size * 0.14F);
            nvg.lineTo(centerX + size * 0.09F, centerY + size * 0.40F);
            nvg.strokePath(color, w);
        }
    },

    /** Module - three stacked slanted bars (the shape family of the UI). */
    MODULES {
        @Override
        public void draw(NanoVGManager nvg, float centerX, float centerY, float size, int color) {
            float barHeight = size * 0.16F;
            float gap = size * 0.10F;

            for (int i = 0; i < 3; i++) {
                float top = centerY - size * 0.34F + i * (barHeight + gap);
                float width = size * 0.72F - i * size * 0.14F;
                nvg.drawSkewRect(centerX - width * 0.5F, top, width, barHeight, size * 0.10F, color);
            }
        }
    },

    /** Music - a single eighth note. */
    MUSIC {
        @Override
        public void draw(NanoVGManager nvg, float centerX, float centerY, float size, int color) {
            float w = size * 0.085F;

            // Note head.
            nvg.drawCircle(centerX - size * 0.18F, centerY + size * 0.28F, size * 0.16F, color);

            // Stem.
            nvg.drawLine(centerX - size * 0.02F, centerY + size * 0.28F,
                    centerX - size * 0.02F, centerY - size * 0.40F, color, w);

            // Flag.
            nvg.beginPath();
            nvg.moveTo(centerX - size * 0.02F, centerY - size * 0.40F);
            nvg.lineTo(centerX + size * 0.30F, centerY - size * 0.26F);
            nvg.lineTo(centerX + size * 0.30F, centerY - size * 0.02F);
            nvg.strokePath(color, w);
        }
    },

    /** Settings - three sliders with knobs. */
    SETTINGS {
        @Override
        public void draw(NanoVGManager nvg, float centerX, float centerY, float size, int color) {
            float w = size * 0.085F;
            float half = size * 0.40F;

            for (int i = 0; i < 3; i++) {
                float y = centerY - size * 0.28F + i * size * 0.28F;
                float knob = centerX + (i == 1 ? size * 0.16F : (i == 0 ? -size * 0.12F : -size * 0.04F));

                nvg.drawLine(centerX - half, y, centerX + half, y, color, w);
                nvg.drawCircle(knob, y, size * 0.11F, color);
            }
        }
    },

    /** HUD - a layout box with a widget inside. */
    HUD {
        @Override
        public void draw(NanoVGManager nvg, float centerX, float centerY, float size, int color) {
            float w = size * 0.085F;
            float halfWidth = size * 0.42F;
            float halfHeight = size * 0.32F;

            // Frame.
            nvg.beginPath();
            nvg.moveTo(centerX - halfWidth, centerY - halfHeight);
            nvg.lineTo(centerX + halfWidth, centerY - halfHeight);
            nvg.lineTo(centerX + halfWidth, centerY + halfHeight);
            nvg.lineTo(centerX - halfWidth, centerY + halfHeight);
            nvg.closePath();
            nvg.strokePath(color, w);

            // Divider.
            nvg.drawLine(centerX - halfWidth, centerY - size * 0.04F, centerX + halfWidth, centerY - size * 0.04F, color, w);

            // Widget.
            nvg.drawSkewRect(centerX - halfWidth + size * 0.08F, centerY + size * 0.04F,
                    size * 0.30F, size * 0.20F, size * 0.08F, color);
        }
    },

    /** Previous track - a triangle pointing left with a bar. */
    PREVIOUS {
        @Override
        public void draw(NanoVGManager nvg, float centerX, float centerY, float size, int color) {
            float w = size * 0.10F;

            nvg.beginPath();
            nvg.moveTo(centerX - size * 0.06F, centerY - size * 0.30F);
            nvg.lineTo(centerX - size * 0.06F, centerY + size * 0.30F);
            nvg.lineTo(centerX + size * 0.36F, centerY);
            nvg.closePath();
            nvg.fillPath(color);

            nvg.drawRect(centerX - size * 0.40F, centerY - size * 0.30F, size * 0.12F, size * 0.60F, color);
        }
    },

    /** Play - a triangle pointing right. */
    PLAY {
        @Override
        public void draw(NanoVGManager nvg, float centerX, float centerY, float size, int color) {
            nvg.beginPath();
            nvg.moveTo(centerX - size * 0.30F, centerY - size * 0.36F);
            nvg.lineTo(centerX - size * 0.30F, centerY + size * 0.36F);
            nvg.lineTo(centerX + size * 0.36F, centerY);
            nvg.closePath();
            nvg.fillPath(color);
        }
    },

    /** Pause - two vertical bars. */
    PAUSE {
        @Override
        public void draw(NanoVGManager nvg, float centerX, float centerY, float size, int color) {
            nvg.drawRect(centerX - size * 0.28F, centerY - size * 0.34F, size * 0.20F, size * 0.68F, color);
            nvg.drawRect(centerX + size * 0.08F, centerY - size * 0.34F, size * 0.20F, size * 0.68F, color);
        }
    },

    /** Next track - a triangle pointing right with a bar. */
    NEXT {
        @Override
        public void draw(NanoVGManager nvg, float centerX, float centerY, float size, int color) {
            float w = size * 0.10F;

            nvg.beginPath();
            nvg.moveTo(centerX + size * 0.06F, centerY - size * 0.30F);
            nvg.lineTo(centerX + size * 0.06F, centerY + size * 0.30F);
            nvg.lineTo(centerX - size * 0.36F, centerY);
            nvg.closePath();
            nvg.fillPath(color);

            nvg.drawRect(centerX + size * 0.28F, centerY - size * 0.30F, size * 0.12F, size * 0.60F, color);
        }
    },

    /** Contrast - a circle with a diagonal bar (the symbol of the theme switch). */
    CONTRAST {
        @Override
        public void draw(NanoVGManager nvg, float centerX, float centerY, float size, int color) {
            float radius = size * 0.38F;
            float thickness = Math.max(1.0F, size * 0.09F);

            nvg.beginPath();
            nvg.arc(centerX, centerY, radius, 0.0F, (float) (Math.PI * 2.0D), NanoVG.NVG_CW);
            nvg.strokePath(color, thickness);

            nvg.drawLine(centerX - radius * 0.72F, centerY + radius * 0.72F,
                    centerX + radius * 0.72F, centerY - radius * 0.72F, color, thickness);
        }
    },

    /**
     * GitHub - the official mark of the repository (octicons, MIT license). In contrast to the other
     * icons it is a resource: the SVG is rasterized once and tinted with the color of the state.
     */
    GITHUB {
        @Override
        public void draw(NanoVGManager nvg, float centerX, float centerY, float size, int color) {
            int image = nvg.getSvgImage(GITHUB_MARK, size * 2.0F, color);

            if (image == 0) {
                return;
            }

            nvg.drawImage(image, centerX - size * 0.5F, centerY - size * 0.5F, size, size, 1.0F);
        }
    };

    /** The official logo of GitHub (octicons, MIT license). */
    String GITHUB_MARK = "/assets/phiclient/images/github-mark.svg";
}
