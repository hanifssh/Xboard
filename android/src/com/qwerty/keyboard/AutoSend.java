package com.qwerty.keyboard;

public final class AutoSend {
    public static final int CYCLE_MS = 150;
    public static final int ENTER_DELAY_MS = 40;
    public static final int AMOUNT_DEFAULT = 10;
    public static final int AMOUNT_MAX = 100000;

    private AutoSend() {}

    public static int clampAmount(int v) {
        return Math.max(1, Math.min(AMOUNT_MAX, v));
    }

    public static int parseAmount(String s) {
        if (s == null) return AMOUNT_DEFAULT;
        String t = s.trim();
        if (t.isEmpty()) return AMOUNT_DEFAULT;
        try {
            long v = Long.parseLong(t);
            if (v > AMOUNT_MAX) return AMOUNT_MAX;
            return clampAmount((int) v);
        } catch (NumberFormatException e) {
            return AMOUNT_DEFAULT;
        }
    }
}
