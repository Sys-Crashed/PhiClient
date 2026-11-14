package org.dpdns.syscrashed.phiclient.gui.util;

import org.dpdns.syscrashed.phiclient.gui.icon.PhiCheckIcon;
import org.dpdns.syscrashed.phiclient.gui.icon.PhiIcon;
import org.dpdns.syscrashed.phiclient.gui.nanovg.NanoVGManager;
import org.dpdns.syscrashed.phiclient.gui.theme.PhiTheme;

/**
 * The icon transition of a control block, using the Cupertino replace preset
 * (package {@code org.dpdns.syscrashed.phiclient.gui.util}).
 *
 * <p>A confirmation replaces the resting icon with the feedback icon: the resting icon shrinks and
 * moves out, the feedback icon enters with a damped spring and (for a check mark) a draw-on stroke.
 * After a short hold the resting icon springs back. A state driven transition instead rotates and
 * scales between two persistent icons, for example the sun and the moon of the theme block.</p>
 *
 * <p>All animation is derived from absolute timestamps, so it is frame rate independent and does not
 * allocate temporary objects in the draw path.</p>
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #trigger()} - starts a confirmation.</li>
 *   <li>{@link #setStateTarget(float)} - selects the icon of a state driven transition.</li>
 *   <li>{@link #draw(NanoVGManager, float, float, float, int)} - advances and draws the transition.</li>
 * </ul>
 */
public final class PhiIconFeedback {

    private static final long PRIMARY_OUT_MS = 140L;
    private static final long FEEDBACK_DELAY_MS = 90L;
    private static final long ENTER_END_MS = 360L;
    private static final long HOLD_END_MS = 1010L;
    private static final long EXIT_END_MS = 1230L;

    /** Angular frequency and damping ratio of the replace spring (zeta 0.72 = a small overshoot). */
    private static final float SPRING_OMEGA = 20.0F;
    private static final float SPRING_ZETA = 0.72F;

    private final PhiIcon primary;
    private final PhiIcon feedback;
    private final boolean stateDriven;

    /** The moment the current confirmation started (0 = resting). */
    private long feedbackStart;

    private float stateFrom;
    private float stateTarget;
    private long stateStart;

    /**
     * @param primary    the icon of the resting state.
     * @param feedback   the icon of the feedback or second state (may be null).
     * @param stateDriven whether the transition follows {@link #setStateTarget(float)} instead of
     *                    the confirmation timeline.
     */
    public PhiIconFeedback(PhiIcon primary, PhiIcon feedback, boolean stateDriven) {
        this.primary = primary;
        this.feedback = feedback;
        this.stateDriven = stateDriven;
    }

    /** Starts the confirmation timeline. */
    public void trigger() {
        if (!this.stateDriven) {
            this.feedbackStart = System.currentTimeMillis();
        }
    }

    /**
     * Selects the target icon of a state driven transition.
     *
     * @param target 0 = the primary icon, 1 = the feedback icon.
     */
    public void setStateTarget(float target) {
        if (!this.stateDriven || target == this.stateTarget) {
            return;
        }

        long now = System.currentTimeMillis();
        this.stateFrom = getState(now);
        this.stateTarget = target;
        this.stateStart = now;
    }

    /**
     * Advances the transition and draws the active icons.
     *
     * @param nvg     the NanoVG drawing context.
     * @param centerX the horizontal center of the icon.
     * @param centerY the vertical center of the icon.
     * @param size    the edge length of the icon box.
     * @param color   the color of the resting icon (a confirmation check uses the accent color).
     */
    public void draw(NanoVGManager nvg, float centerX, float centerY, float size, int color) {
        if (this.primary == null) {
            return;
        }

        if (this.feedback == null) {
            this.primary.draw(nvg, centerX, centerY, size, color);
            return;
        }

        if (this.stateDriven) {
            drawState(nvg, centerX, centerY, size, color);
        } else {
            drawConfirmation(nvg, centerX, centerY, size, color);
        }
    }

    /** Draws the replace / hold / restore timeline of a confirmation. */
    private void drawConfirmation(NanoVGManager nvg, float centerX, float centerY, float size, int color) {
        if (this.feedbackStart == 0L) {
            this.primary.draw(nvg, centerX, centerY, size, color);
            return;
        }

        long elapsed = System.currentTimeMillis() - this.feedbackStart;

        if (elapsed >= EXIT_END_MS) {
            this.feedbackStart = 0L;
            this.primary.draw(nvg, centerX, centerY, size, color);
            return;
        }

        if (elapsed < HOLD_END_MS) {
            float out = clamp01(elapsed / (float) PRIMARY_OUT_MS);
            drawIcon(nvg, this.primary, centerX, centerY, size, color, 1.0F - out,
                    1.0F - 0.18F * out, size * 0.06F * out, 0.0F, 1.0F);

            float in = clamp01((elapsed - FEEDBACK_DELAY_MS) / (float) (ENTER_END_MS - FEEDBACK_DELAY_MS));
            float scale = 0.78F + 0.22F * springResponse(elapsed - FEEDBACK_DELAY_MS);

            drawIcon(nvg, this.feedback, centerX, centerY, size, PhiTheme.accent(), in, scale,
                    0.0F, 0.0F, easeOutCubic(in));
            return;
        }

        float t = clamp01((elapsed - HOLD_END_MS) / (float) (EXIT_END_MS - HOLD_END_MS));

        drawIcon(nvg, this.feedback, centerX, centerY, size, PhiTheme.accent(), 1.0F - t,
                1.0F + 0.12F * t, -size * 0.06F * t, 0.0F, 1.0F);
        drawIcon(nvg, this.primary, centerX, centerY, size, color, t,
                0.82F + 0.18F * easeOutBack(t), 0.0F, 0.0F, 1.0F);
    }

    /** Draws the rotating replace between two persistent state icons. */
    private void drawState(NanoVGManager nvg, float centerX, float centerY, float size, int color) {
        float mix = getState(System.currentTimeMillis());
        float primaryMix = clamp01(mix);
        float feedbackMix = primaryMix;

        drawIcon(nvg, this.primary, centerX, centerY, size, color, 1.0F - primaryMix,
                1.0F - 0.30F * primaryMix, 0.0F, 60.0F * mix, 1.0F);
        drawIcon(nvg, this.feedback, centerX, centerY, size, color, feedbackMix,
                0.75F + 0.25F * mix, 0.0F, -30.0F * (1.0F - mix), 1.0F);
    }

    /**
     * @return the current state mix (0 = primary, 1 = feedback). The spring is allowed to overshoot;
     *         callers clamp the value before using it as an opacity.
     */
    private float getState(long now) {
        if (this.stateStart == 0L) {
            return this.stateTarget;
        }

        float response = springResponse(now - this.stateStart);
        return this.stateFrom + (this.stateTarget - this.stateFrom) * response;
    }

    /** Draws one icon with its replace transform and opacity. */
    private static void drawIcon(NanoVGManager nvg, PhiIcon icon, float centerX, float centerY,
                                 float size, int color, float alpha, float scale, float offsetY,
                                 float rotation, float drawProgress) {
        if (icon == null || alpha <= 0.001F || scale <= 0.001F) {
            return;
        }

        nvg.save();
        nvg.translate(centerX, centerY + offsetY);

        if (Math.abs(rotation) > 0.01F) {
            nvg.rotate(rotation);
        }

        nvg.scale(scale, scale);
        nvg.translate(-centerX, -centerY);
        nvg.setGlobalAlpha(alpha);

        if (icon instanceof PhiCheckIcon) {
            ((PhiCheckIcon) icon).draw(nvg, centerX, centerY, size, color, drawProgress);
        } else {
            icon.draw(nvg, centerX, centerY, size, color);
        }

        nvg.resetGlobalAlpha();
        nvg.restore();
    }

    /**
     * Unit step response of a damped harmonic oscillator. The value starts at 0, settles at 1 and
     * overshoots slightly, which gives the replace its soft spring feel.
     */
    private static float springResponse(float elapsedMs) {
        if (elapsedMs <= 0.0F) {
            return 0.0F;
        }

        float t = elapsedMs / 1000.0F;
        float damping = SPRING_ZETA * SPRING_OMEGA;
        float dampedOmega = SPRING_OMEGA * (float) Math.sqrt(1.0F - SPRING_ZETA * SPRING_ZETA);
        float decay = (float) Math.exp(-damping * t);

        return 1.0F - decay * ((float) Math.cos(dampedOmega * t)
                + (damping / dampedOmega) * (float) Math.sin(dampedOmega * t));
    }

    private static float easeOutCubic(float t) {
        return 1.0F - (float) Math.pow(1.0F - t, 3.0D);
    }

    /** Out-back easing with a small overshoot; unlike the spring it ends exactly at 1. */
    private static float easeOutBack(float t) {
        float c = 1.2F;
        float u = t - 1.0F;
        return 1.0F + (c + 1.0F) * u * u * u + c * u * u;
    }

    private static float clamp01(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }
}
