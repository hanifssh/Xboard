package com.qwerty.keyboard;

import android.graphics.RectF;

public class Key {
    public static final int CHAR = 0;
    public static final int SHIFT = -1;
    public static final int MODE_SYM = -2;
    public static final int MODE_ALPHA = -3;
    public static final int MODE_SYM2 = -4;
    public static final int DELETE = -5;
    public static final int EMOJI = -6;
    public static final int EMOJI_PREV = -7;
    public static final int EMOJI_NEXT = -8;
    public static final int ENTER = 10;
    public static final int SPACE = 32;

    public String label;
    public int code = CHAR;
    public String text;
    public String hint;
    public String[] alts;
    public float w = 1f;
    public boolean func;
    public boolean repeat;
    public boolean longAction;
    public boolean emoji;
    public final RectF rect = new RectF();
    public final RectF hit = new RectF();

    public boolean isLetter() {
        return code == CHAR && text != null && text.length() == 1 && Character.isLetter(text.charAt(0));
    }
}
