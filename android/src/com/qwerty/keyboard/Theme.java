package com.qwerty.keyboard;

public final class Theme {
    public static final String[] NAMES = {"xboard", "neon", "ocean", "sunset", "rose", "dark", "light"};
    public static final String[] LABELS = {"Xboard", "Neon", "Ocean", "Sunset", "Rose", "Graphite", "Light"};

    public int bg, key, keyPressed, func, funcPressed, text, hint, accent, accentPressed, accentText, shadow,
            previewBg, stripText, divider;

    private Theme set(int bg, int key, int keyPressed, int func, int funcPressed, int text, int hint, int accent,
                      int accentPressed, int accentText, int shadow, int previewBg, int stripText, int divider) {
        this.bg = bg; this.key = key; this.keyPressed = keyPressed; this.func = func; this.funcPressed = funcPressed;
        this.text = text; this.hint = hint; this.accent = accent; this.accentPressed = accentPressed;
        this.accentText = accentText; this.shadow = shadow; this.previewBg = previewBg; this.stripText = stripText;
        this.divider = divider;
        return this;
    }

    public static Theme get(String name) {
        Theme t = new Theme();
        if ("light".equals(name)) {
            return t.set(0xFFE3E6EA, 0xFFFFFFFF, 0xFFD3D8DF, 0xFFC8CDD5, 0xFFADB4BE, 0xFF1C1E22, 0xFF6B717B,
                    0xFF1E7BFF, 0xFF1462D1, 0xFFFFFFFF, 0xFFA6ADB7, 0xFFFFFFFF, 0xFF2A2D33, 0xFFC2C7CF);
        }
        if ("dark".equals(name)) {
            return t.set(0xFF16181D, 0xFF2E3139, 0xFF4A4F5B, 0xFF22252B, 0xFF3A3E47, 0xFFF2F3F5, 0xFF8D939D,
                    0xFFF2A33A, 0xFFD1872A, 0xFF1A1A1A, 0xFF0B0C0F, 0xFF41454F, 0xFFE6E8EB, 0xFF33363E);
        }
        if ("neon".equals(name)) {
            return t.set(0xFF07090D, 0xFF141A1F, 0xFF22303A, 0xFF0F1418, 0xFF1C262E, 0xFFE9FFF6, 0xFF6E8C80,
                    0xFF19F59A, 0xFF12C47B, 0xFF04140D, 0xFF000000, 0xFF22303A, 0xFFCFF7E7, 0xFF1E2A30);
        }
        if ("ocean".equals(name)) {
            return t.set(0xFF0A1E26, 0xFF12333F, 0xFF1C4A5A, 0xFF0E2731, 0xFF184050, 0xFFE6FBFF, 0xFF7BA7B5,
                    0xFF2EC4D6, 0xFF1FA0B0, 0xFF02181D, 0xFF041016, 0xFF1C4A5A, 0xFFD3F3F8, 0xFF163A47);
        }
        if ("sunset".equals(name)) {
            return t.set(0xFF1E1220, 0xFF3A2238, 0xFF55304F, 0xFF2B1A2C, 0xFF452843, 0xFFFFEFF2, 0xFFB892A6,
                    0xFFFF6B5A, 0xFFE04E3E, 0xFFFFFFFF, 0xFF0E070F, 0xFF55304F, 0xFFFFE3E8, 0xFF3E2640);
        }
        if ("rose".equals(name)) {
            return t.set(0xFFF7E4EA, 0xFFFFFFFF, 0xFFF2CCD8, 0xFFEDC7D3, 0xFFE0AFBF, 0xFF3A1D27, 0xFF9A6F7D,
                    0xFFE0507A, 0xFFC23B63, 0xFFFFFFFF, 0xFFD7AFBC, 0xFFFFFFFF, 0xFF4A2632, 0xFFE3BCC8);
        }
        return t.set(0xFF0B0F1A, 0xFF1A2233, 0xFF2A3650, 0xFF131A28, 0xFF22304A, 0xFFEFF4FF, 0xFF7F8DA8,
                0xFF1E9BFF, 0xFF1478CC, 0xFFFFFFFF, 0xFF05070C, 0xFF2A3650, 0xFFDCE6F7, 0xFF223049);
    }
}
