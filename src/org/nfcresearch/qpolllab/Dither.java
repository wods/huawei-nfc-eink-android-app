package org.nfcresearch.qpolllab;

/**
 * Image dithering algorithms for 552x768 4-color / 1-bit E-ink display.
 * Includes classic error diffusion, retro ordered Bayer halftoning, and noise dithering.
 */
public enum Dither {
    NONE("无抖动（最省电 / 纯色阶）"),
    FLOYD_STEINBERG("Floyd-Steinberg 扩散（经典均衡）"),
    ATKINSON("Atkinson 麦金塔扩散（高对比 / 墨水屏首选）"),
    BURKES("Burkes 快速扩散（平滑渐变）"),
    SIERRA3("Sierra-3 完整扩散（细腻真实）"),
    SIERRA2("Sierra-2 简易扩散（自然柔和）"),
    SIERRA_LITE("Sierra Lite 极速扩散"),
    STUCKI("Stucki 扩散（清晰少噪）"),
    JJN("Jarvis-Judice-Ninke 扩散（大卷积核）"),
    BAYER_4X4("Bayer 4×4 有序网点（复古点阵）"),
    BAYER_8X8("Bayer 8×8 有序网点（细腻网纹）"),
    BAYER_2X2("Bayer 2×2 有序网点（粗网格点）"),
    BLUE_NOISE("蓝噪声 (Blue Noise) 抖动（自然颗粒感）"),
    RANDOM("随机噪点 (Random) 抖动");

    public final String label;

    Dither(String label) {
        this.label = label;
    }

    public static String[] labels() {
        Dither[] values = values();
        String[] labels = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            labels[i] = values[i].label;
        }
        return labels;
    }
}
