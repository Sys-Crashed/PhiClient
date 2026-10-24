package org.dpdns.syscrashed.phiclient.gui.nanovg.font;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 界面字体表。
 *
 * <p>只使用 PhiClient 自己的字体：
 * 「Source Han Sans &amp; Saira Hybrid」（思源黑体 + Saira 的混合字体）。
 */
public final class Fonts {

    public static final Font REGULAR = new Font("regular", "/assets/phiclient/fonts/SourceHanSansSaira-Regular.ttf");

    private Fonts() {
    }

    public static List<Font> getFonts() {
        return new ArrayList<Font>(Arrays.asList(REGULAR));
    }
}
