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

    /**
     * More settings - a gear wheel. It marks the modules whose parameters can be adjusted: the wheel
     * is drawn as eight teeth around a ring with a hole in the middle.
     */
    MORE_SETTINGS {
        @Override
        public void draw(NanoVGManager nvg, float centerX, float centerY, float size, int color) {
            float w = size * 0.075F;
            float outer = size * 0.34F;
            float toothLength = size * 0.10F;

            // Teeth: short spokes that stick out of the ring.
            for (int i = 0; i < 8; i++) {
                float angle = (float) (i * Math.PI / 4.0D);
                float cos = (float) Math.cos(angle);
                float sin = (float) Math.sin(angle);

                nvg.drawLine(centerX + cos * outer, centerY + sin * outer,
                        centerX + cos * (outer + toothLength), centerY + sin * (outer + toothLength), color, w);
            }

            // Ring.
            nvg.beginPath();
            nvg.arc(centerX, centerY, outer, 0.0F, (float) (Math.PI * 2.0D), NanoVG.NVG_CW);
            nvg.strokePath(color, w);

            // Hole in the middle.
            nvg.beginPath();
            nvg.arc(centerX, centerY, size * 0.13F, 0.0F, (float) (Math.PI * 2.0D), NanoVG.NVG_CW);
            nvg.strokePath(color, w);
        }
    },

    /** Reset - a circular arrow that restores the default values of a module. */
    RESET {
        @Override
        public void draw(NanoVGManager nvg, float centerX, float centerY, float size, int color) {
            float w = size * 0.085F;
            float radius = size * 0.30F;

            // The open ring (most of a circle).
            nvg.beginPath();
            nvg.arc(centerX, centerY, radius, (float) (Math.PI * 0.2D), (float) (Math.PI * 1.7D), NanoVG.NVG_CW);
            nvg.strokePath(color, w);

            // The arrow head at the end of the ring.
            nvg.beginPath();
            nvg.moveTo(centerX + radius * 0.98F, centerY - radius * 0.20F);
            nvg.lineTo(centerX + radius * 0.62F, centerY - radius * 0.40F);
            nvg.lineTo(centerX + radius * 0.84F, centerY + radius * 0.14F);
            nvg.closePath();
            nvg.fillPath(color);
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
    },

    /** Favorite - a five pointed star (filled). */
    FAVORITE {
        @Override
        public void draw(NanoVGManager nvg, float centerX, float centerY, float size, int color) {
            float outer = size * 0.46F;
            float inner = outer * 0.42F;

            nvg.beginPath();
            for (int i = 0; i < 10; i++) {
                double angle = -Math.PI / 2.0D + i * Math.PI / 5.0D;
                float radius = (i % 2 == 0) ? outer : inner;
                float px = centerX + (float) Math.cos(angle) * radius;
                float py = centerY + (float) Math.sin(angle) * radius;
                if (i == 0) {
                    nvg.moveTo(px, py);
                } else {
                    nvg.lineTo(px, py);
                }
            }
            nvg.closePath();
            nvg.fillPath(color);
        }
    },

    /** Shuffle - two crossing paths with arrow heads (pseudo random order). */
    SHUFFLE {
        @Override
        public void draw(NanoVGManager nvg, float centerX, float centerY, float size, int color) {
            float w = size * 0.085F;

            // Upper path: left low to right high, then bends down.
            nvg.beginPath();
            nvg.moveTo(centerX - size * 0.42F, centerY - size * 0.22F);
            nvg.lineTo(centerX - size * 0.12F, centerY - size * 0.22F);
            nvg.lineTo(centerX + size * 0.12F, centerY + size * 0.22F);
            nvg.lineTo(centerX + size * 0.30F, centerY + size * 0.22F);
            nvg.strokePath(color, w);

            // Lower path: left high to right low, then bends up.
            nvg.beginPath();
            nvg.moveTo(centerX - size * 0.42F, centerY + size * 0.22F);
            nvg.lineTo(centerX - size * 0.12F, centerY + size * 0.22F);
            nvg.lineTo(centerX + size * 0.12F, centerY - size * 0.22F);
            nvg.lineTo(centerX + size * 0.30F, centerY - size * 0.22F);
            nvg.strokePath(color, w);

            // Arrow heads at the two right ends.
            nvg.beginPath();
            nvg.moveTo(centerX + size * 0.16F, centerY - size * 0.36F);
            nvg.lineTo(centerX + size * 0.42F, centerY - size * 0.22F);
            nvg.lineTo(centerX + size * 0.16F, centerY - size * 0.08F);
            nvg.strokePath(color, w);

            nvg.beginPath();
            nvg.moveTo(centerX + size * 0.16F, centerY + size * 0.08F);
            nvg.lineTo(centerX + size * 0.42F, centerY + size * 0.22F);
            nvg.lineTo(centerX + size * 0.16F, centerY + size * 0.36F);
            nvg.strokePath(color, w);
        }
    },

    /** Sequential loop - a rectangular cycle with an arrow head. */
    REPEAT {
        @Override
        public void draw(NanoVGManager nvg, float centerX, float centerY, float size, int color) {
            float w = size * 0.085F;
            float halfW = size * 0.38F;
            float halfH = size * 0.26F;

            // The open rectangle (three sides).
            nvg.beginPath();
            nvg.moveTo(centerX + halfW, centerY - halfH);
            nvg.lineTo(centerX - halfW, centerY - halfH);
            nvg.lineTo(centerX - halfW, centerY + halfH);
            nvg.lineTo(centerX + halfW, centerY + halfH);
            nvg.strokePath(color, w);

            // Right side up to the arrow.
            nvg.drawLine(centerX + halfW, centerY + halfH, centerX + halfW, centerY - size * 0.06F, color, w);

            // Arrow head pointing up.
            nvg.beginPath();
            nvg.moveTo(centerX + halfW - size * 0.14F, centerY - size * 0.06F);
            nvg.lineTo(centerX + halfW, centerY - halfH - size * 0.06F);
            nvg.lineTo(centerX + halfW + size * 0.14F, centerY - size * 0.06F);
            nvg.strokePath(color, w);
        }
    },

    /** Single loop - the cycle of {@link #REPEAT} with a "1" bar in the middle. */
    REPEAT_ONE {
        @Override
        public void draw(NanoVGManager nvg, float centerX, float centerY, float size, int color) {
            PhiIcons.REPEAT.draw(nvg, centerX, centerY, size, color);

            // The small "1": a stem with a foot and a flag.
            nvg.drawLine(centerX, centerY - size * 0.10F, centerX, centerY + size * 0.16F, color, size * 0.075F);
            nvg.drawLine(centerX - size * 0.08F, centerY - size * 0.02F, centerX, centerY - size * 0.10F,
                    color, size * 0.075F);
        }
    },

    /** Volume - a speaker with two sound waves. */
    VOLUME {
        @Override
        public void draw(NanoVGManager nvg, float centerX, float centerY, float size, int color) {
            float w = size * 0.085F;

            // Speaker body (filled trapezoid with the box at the left).
            nvg.beginPath();
            nvg.moveTo(centerX - size * 0.42F, centerY - size * 0.14F);
            nvg.lineTo(centerX - size * 0.22F, centerY - size * 0.14F);
            nvg.lineTo(centerX - size * 0.02F, centerY - size * 0.34F);
            nvg.lineTo(centerX - size * 0.02F, centerY + size * 0.34F);
            nvg.lineTo(centerX - size * 0.22F, centerY + size * 0.14F);
            nvg.lineTo(centerX - size * 0.42F, centerY + size * 0.14F);
            nvg.closePath();
            nvg.fillPath(color);

            // Sound waves.
            nvg.beginPath();
            nvg.arc(centerX + size * 0.06F, centerY, size * 0.18F,
                    (float) (-Math.PI * 0.35D), (float) (Math.PI * 0.35D), NanoVG.NVG_CW);
            nvg.strokePath(color, w);

            nvg.beginPath();
            nvg.arc(centerX + size * 0.06F, centerY, size * 0.32F,
                    (float) (-Math.PI * 0.35D), (float) (Math.PI * 0.35D), NanoVG.NVG_CW);
            nvg.strokePath(color, w);
        }
    },

    /** Plus - a cross (add a song / create a playlist). */
    PLUS {
        @Override
        public void draw(NanoVGManager nvg, float centerX, float centerY, float size, int color) {
            float w = size * 0.10F;
            float half = size * 0.34F;

            nvg.drawLine(centerX - half, centerY, centerX + half, centerY, color, w);
            nvg.drawLine(centerX, centerY - half, centerX, centerY + half, color, w);
        }
    },

    /** Search - a magnifying glass. */
    SEARCH {
        @Override
        public void draw(NanoVGManager nvg, float centerX, float centerY, float size, int color) {
            float w = size * 0.09F;

            nvg.beginPath();
            nvg.arc(centerX - size * 0.08F, centerY - size * 0.08F, size * 0.28F,
                    0.0F, (float) (Math.PI * 2.0D), NanoVG.NVG_CW);
            nvg.strokePath(color, w);

            nvg.drawLine(centerX + size * 0.12F, centerY + size * 0.12F,
                    centerX + size * 0.40F, centerY + size * 0.40F, color, w);
        }
    },

    /** Folder - opens the music folder in the system file manager. */
    FOLDER {
        @Override
        public void draw(NanoVGManager nvg, float centerX, float centerY, float size, int color) {
            float w = size * 0.085F;

            nvg.beginPath();
            nvg.moveTo(centerX - size * 0.42F, centerY + size * 0.32F);
            nvg.lineTo(centerX - size * 0.42F, centerY - size * 0.30F);
            nvg.lineTo(centerX - size * 0.08F, centerY - size * 0.30F);
            nvg.lineTo(centerX + size * 0.04F, centerY - size * 0.16F);
            nvg.lineTo(centerX + size * 0.42F, centerY - size * 0.16F);
            nvg.lineTo(centerX + size * 0.42F, centerY + size * 0.32F);
            nvg.closePath();
            nvg.strokePath(color, w);
        }
    },

    /** Playlist - three lines with a small note on the last one. */
    PLAYLIST {
        @Override
        public void draw(NanoVGManager nvg, float centerX, float centerY, float size, int color) {
            float w = size * 0.085F;
            float left = centerX - size * 0.40F;
            float right = centerX + size * 0.40F;

            nvg.drawLine(left, centerY - size * 0.26F, right, centerY - size * 0.26F, color, w);
            nvg.drawLine(left, centerY, right, centerY, color, w);
            nvg.drawLine(left, centerY + size * 0.26F, centerX + size * 0.10F, centerY + size * 0.26F, color, w);

            // Small note head on the last line.
            nvg.drawCircle(centerX + size * 0.28F, centerY + size * 0.26F, size * 0.10F, color);
        }
    };

    /** The official logo of GitHub (octicons, MIT license). */
    String GITHUB_MARK = "/assets/phiclient/images/github-mark.svg";
}
