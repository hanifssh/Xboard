package com.qwerty.keyboard;

import android.content.Context;
import android.content.SharedPreferences;

public final class Prefs {
    public static final String FILE = "keyboard_prefs";
    public static final String VIBRATE = "vibrate", SOUND = "sound", PREVIEW = "preview", AUTOCAP = "autocap",
            DOUBLE_SPACE = "double_space", SUGGEST = "suggest", NUMBER_ROW = "number_row", THEME = "theme_v2",
            HEIGHT = "height", SCALE = "scale", MESSAGE = "auto_message", AMOUNT = "auto_amount";

    public static final int SCALE_MIN = 70, SCALE_MAX = 150, SCALE_STEP = 5;

    public boolean vibrate, sound, preview, autocap, doubleSpace, suggest, numberRow;
    public String theme, message;
    public int scale, amount;

    public static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static int legacyScale(SharedPreferences s) {
        String h = s.getString(HEIGHT, "normal");
        if ("short".equals(h)) return 90;
        if ("tall".equals(h)) return 110;
        return 100;
    }

    public static int clampScale(int v) {
        return Math.max(SCALE_MIN, Math.min(SCALE_MAX, v));
    }

    public static Prefs load(Context c) {
        SharedPreferences s = sp(c);
        Prefs p = new Prefs();
        p.vibrate = s.getBoolean(VIBRATE, true);
        p.sound = s.getBoolean(SOUND, false);
        p.preview = s.getBoolean(PREVIEW, true);
        p.autocap = s.getBoolean(AUTOCAP, true);
        p.doubleSpace = s.getBoolean(DOUBLE_SPACE, true);
        p.suggest = s.getBoolean(SUGGEST, true);
        p.numberRow = s.getBoolean(NUMBER_ROW, false);
        p.theme = s.getString(THEME, "xboard");
        p.scale = clampScale(s.getInt(SCALE, legacyScale(s)));
        p.message = s.getString(MESSAGE, "");
        p.amount = AutoSend.clampAmount(s.getInt(AMOUNT, AutoSend.AMOUNT_DEFAULT));
        return p;
    }
}
