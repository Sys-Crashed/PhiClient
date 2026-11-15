package org.dpdns.syscrashed.phiclient.gui.input;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

import org.dpdns.syscrashed.phiclient.logger.PhiLogger;

/**
 * Windows IME bridge - makes CJK input (Chinese, Japanese, Korean) work in the hub text fields
 * (package {@code org.dpdns.syscrashed.phiclient.gui.input}).
 *
 * <p><b>Why this exists</b>: Minecraft 1.8.9 ships LWJGL 2.9.4-nightly-20150209, whose Windows
 * implementation does not handle {@code WM_IME_*} messages at all (verified against its sources:
 * no IME handling in {@code WindowsDisplay}, {@code WindowsKeyboard} or {@code Keyboard}). ASCII
 * keystrokes arrive as {@code WM_CHAR} keyboard events, but characters committed by an input
 * method editor never reach {@code Keyboard.next()} - so the user can type English but no CJK
 * characters. This is the same reason the vanilla 1.8.9 chat box needs InputFix.</p>
 *
 * <p><b>How it works</b>: instead of hooking the window procedure (needs a native DLL), this
 * bridge polls the Imm32 API from the render thread every tick of the open hub screen:
 * {@code ImmGetContext} + {@code ImmGetCompositionStringW(GCS_RESULTSTR)} returns the text the
 * input method committed since the last poll; the caller feeds it into the focused text field
 * through the regular {@code keyTyped} path, so the existing focus gate applies. The composition
 * window (the IME candidate popup) is moved to the caret via {@code ImmSetCompositionWindow},
 * using the caret position reported by the focused field.</p>
 *
 * <p>All calls are no-ops when the platform is not Windows or when the JNA bridge cannot be
 * loaded; English input is unaffected either way. The bridge is driven from one thread (the MC
 * render thread) and keeps no background state beyond the poll cursor.</p>
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #pollCommittedText()} - returns the text committed by the IME since the last poll, or {@code null}.</li>
 *   <li>{@link #setCaretGuiPosition(int, int)} - the focused field reports its caret in GUI coordinates.</li>
 *   <li>{@link #setCaretScreenPosition(int, int)} - the hub reports the converted screen position.</li>
 * </ul>
 */
public final class PhiImeBridge {

    /** ImmGetCompositionStringW index for the result string of the last composition. */
    private static final int GCS_RESULTSTR = 0x0800;

    /** COMPOSITIONFORM style: position the composition window at the given point. */
    private static final int CFS_POINT = 0x0002;

    private static final int STATE_UNINITIALIZED = 0;
    private static final int STATE_READY = 1;
    private static final int STATE_UNAVAILABLE = 2;

    /**
     * Minimal view of Imm32 (only what the bridge needs). The interface extends
     * {@link StdCallLibrary} so it also works on a 32-bit JVM where Win32 uses stdcall.
     */
    public interface Imm32 extends StdCallLibrary {

        Pointer ImmGetContext(long hwnd);

        /**
         * @param himc      the input context.
         * @param dwIndex   the index of the data to read ({@code GCS_RESULTSTR}).
         * @param lpBuf     the buffer, or {@code null} to query the required size.
         * @param dwBufLen  the buffer size in bytes.
         * @return the number of bytes written (without a null terminator), or the required size
         *         when {@code lpBuf} is {@code null}.
         */
        int ImmGetCompositionStringW(Pointer himc, int dwIndex, Pointer lpBuf, int dwBufLen);

        int ImmSetCompositionWindow(Pointer himc, COMPOSITIONFORM lpCompForm);

        int ImmReleaseContext(long hwnd, Pointer himc);
    }

    /** Minimal view of User32 (only {@code GetForegroundWindow}). */
    public interface User32 extends StdCallLibrary {

        long GetForegroundWindow();
    }

    /**
     * The Win32 COMPOSITIONFORM structure. It is declared with plain {@code int} fields (instead of
     * nested POINT/RECT structures) so the layout is identical on 32/64-bit JVMs and cannot drift
     * due to structure alignment rules: {@code DWORD dwStyle; POINT ptCurrentPos; RECT rcArea}.
     */
    public static class COMPOSITIONFORM extends Structure {

        public int dwStyle;

        /** POINT ptCurrentPos (x, y). */
        public int x;
        public int y;

        /** RECT rcArea (ignored in CFS_POINT mode, kept for the native structure size). */
        public int rcLeft;
        public int rcTop;
        public int rcRight;
        public int rcBottom;
    }

    private static int state = STATE_UNINITIALIZED;

    private static Imm32 imm32;
    private static User32 user32;

    /** How many bytes of the result string were already consumed by the previous polls. */
    private static long lastResultBytes;

    /** The caret position in GUI coordinates reported by the focused field (-1 = none). */
    private static int caretGuiX = -1;
    private static int caretGuiY = -1;

    /** The caret position in screen coordinates converted by the hub (MIN_VALUE = none). */
    private static int caretScreenX = Integer.MIN_VALUE;
    private static int caretScreenY = Integer.MIN_VALUE;

    /** The screen position the composition window was last placed at. */
    private static int placedX = Integer.MIN_VALUE;
    private static int placedY = Integer.MIN_VALUE;

    private PhiImeBridge() {
    }

    /**
     * Loads the JNA bindings once. Failures disable the bridge permanently (English input is
     * unaffected); the log entry is written only once.
     */
    private static synchronized void ensureInitialized() {
        if (state != STATE_UNINITIALIZED) {
            return;
        }
        String os = System.getProperty("os.name", "");
        if (!os.toLowerCase(Locale.ROOT).contains("win")) {
            state = STATE_UNAVAILABLE;
            return;
        }
        try {
            imm32 = (Imm32) Native.loadLibrary("imm32", Imm32.class, W32APIOptions.UNICODE_OPTIONS);
            user32 = (User32) Native.loadLibrary("user32", User32.class, W32APIOptions.UNICODE_OPTIONS);
            state = STATE_READY;
            PhiLogger.info("PhiImeBridge - Windows IME bridge ready (CJK input enabled)");
        } catch (Throwable t) {
            state = STATE_UNAVAILABLE;
            PhiLogger.error("PhiImeBridge - cannot load the IME bridge, CJK input stays disabled", t);
        }
    }

    /**
     * @return true when the IME bridge is usable on this machine.
     */
    public static boolean isAvailable() {
        ensureInitialized();
        return state == STATE_READY;
    }

    /**
     * Polls the IME of the foreground window for text committed since the last poll.
     *
     * <p>The foreground window is queried on every call: while the game is not focused, the user
     * cannot be typing into the game anyway, and the committed text then belongs to another
     * window, so nothing is returned. The result string is kept by the IME until the next
     * composition starts, so the bridge keeps a byte cursor and returns only the new part.</p>
     *
     * @return the newly committed text, or {@code null} when nothing new was committed.
     */
    public static String pollCommittedText() {
        ensureInitialized();
        if (state != STATE_READY) {
            return null;
        }

        long hwnd;
        Pointer himc;
        try {
            hwnd = user32.GetForegroundWindow();
            if (hwnd == 0L) {
                return null;
            }
            himc = imm32.ImmGetContext(hwnd);
        } catch (Throwable t) {
            return null;
        }
        if (himc == null) {
            return null;
        }

        try {
            placeCompositionWindow(himc);

            int total = imm32.ImmGetCompositionStringW(himc, GCS_RESULTSTR, null, 0);
            if (total <= 0 || total <= lastResultBytes) {
                // The result string was reset (a new composition began) or nothing is new.
                if (total < lastResultBytes) {
                    lastResultBytes = Math.max(0, total);
                }
                return null;
            }

            Memory buffer = new Memory(Math.max(total, 2));
            int got = imm32.ImmGetCompositionStringW(himc, GCS_RESULTSTR, buffer, total);
            if (got <= lastResultBytes) {
                lastResultBytes = Math.max(0, got);
                return null;
            }

            byte[] raw = buffer.getByteArray(lastResultBytes, got - (int) lastResultBytes);
            lastResultBytes = got;
            return new String(raw, StandardCharsets.UTF_16LE);
        } catch (Throwable t) {
            PhiLogger.error("PhiImeBridge - IME poll failed", t);
            return null;
        } finally {
            try {
                imm32.ImmReleaseContext(hwnd, himc);
            } catch (Throwable ignored) {
                // already gone
            }
        }
    }

    /**
     * The focused text field reports its caret position in GUI coordinates; a position of
     * (-1, -1) clears the report (the field lost the focus).
     *
     * @param x the caret x in GUI coordinates.
     * @param y the caret y in GUI coordinates.
     */
    public static void setCaretGuiPosition(int x, int y) {
        caretGuiX = x;
        caretGuiY = y;
    }

    /**
     * @return the last reported caret x in GUI coordinates, or -1.
     */
    public static int getCaretGuiX() {
        return caretGuiX;
    }

    /**
     * @return the last reported caret y in GUI coordinates, or -1.
     */
    public static int getCaretGuiY() {
        return caretGuiY;
    }

    /**
     * The hub reports the caret converted to screen coordinates (GUI coordinate scaled by the
     * window scale plus the window origin).
     *
     * @param x the caret x in screen coordinates.
     * @param y the caret y in screen coordinates.
     */
    public static void setCaretScreenPosition(int x, int y) {
        caretScreenX = x;
        caretScreenY = y;
    }

    /**
     * Moves the IME composition window (the candidate popup) to the reported caret, but only when
     * the position actually changed.
     */
    private static void placeCompositionWindow(Pointer himc) {
        if (caretScreenX == Integer.MIN_VALUE || caretScreenY == Integer.MIN_VALUE) {
            return;
        }
        if (caretScreenX == placedX && caretScreenY == placedY) {
            return;
        }
        COMPOSITIONFORM form = new COMPOSITIONFORM();
        form.dwStyle = CFS_POINT;
        form.x = caretScreenX;
        form.y = caretScreenY;
        form.rcLeft = 0;
        form.rcTop = 0;
        form.rcRight = 0;
        form.rcBottom = 0;
        if (imm32.ImmSetCompositionWindow(himc, form) != 0) {
            placedX = caretScreenX;
            placedY = caretScreenY;
        }
    }
}
