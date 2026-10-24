package org.dpdns.syscrashed.phiclient.gui.theme;

/**
 * 界面配色。
 *
 * <p>对应原 SoarClient 的 {@code management.color}（AccentColor / ColorPalette / ColorType），
 * 但只保留当前中心界面真正用到的那几组颜色，语义保持一致：
 * <ul>
 *   <li>ACCENT / ACCENT_GRADIENT —— 主色与其渐变端点；</li>
 *   <li>PANEL / PANEL_DARK —— 面板背景与侧栏深色；</li>
 *   <li>CARD —— 内容卡片；</li>
 *   <li>TEXT / TEXT_DIM —— 正文与次要文字。</li>
 * </ul>
 */
public final class PhiTheme {

    private PhiTheme() {
    }

    /** 主色（蓝紫）。 */
    public static final int ACCENT = 0xFF5B6CFF;

    /** 主色渐变端点。 */
    public static final int ACCENT_DARK = 0xFF3A46C8;

    /** 面板主体背景。 */
    public static final int PANEL = 0xE6141620;

    /** 侧栏 / 头部条背景。 */
    public static final int PANEL_DARK = 0xF00E1018;

    /** 内容卡片背景。 */
    public static final int CARD = 0xFF1B1F2B;

    /** 卡片描边。 */
    public static final int CARD_BORDER = 0xFF262B3A;

    /** 正文颜色。 */
    public static final int TEXT = 0xFFFFFFFF;

    /** 次要文字颜色。 */
    public static final int TEXT_DIM = 0xFF9AA3BF;

    /** 分隔线颜色。 */
    public static final int DIVIDER = 0xFF232838;
}
