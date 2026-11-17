package com.qwerty.keyboard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.text.TextPaint;
import android.view.MotionEvent;
import android.view.View;

public class SuggestionStrip extends View {

    public interface Listener {
        void onSuggestion(String word, boolean literal);
        void onSuggestionLongPress(String word);
        void onHide();
        void onSettings();
    }

    private final float dp, sp;
    private final TextPaint text = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Paint line = new Paint();
    private Theme theme = Theme.get("dark");
    private Listener listener;
    private String[] slots = new String[3];
    private int literalSlot = -1;
    private int pressed = -1;
    private long downTime;
    private boolean wall;

    public SuggestionStrip(Context c) {
        super(c);
        dp = c.getResources().getDisplayMetrics().density;
        sp = c.getResources().getDisplayMetrics().scaledDensity;
        text.setTextAlign(Paint.Align.CENTER);
    }

    public void setListener(Listener l) { listener = l; }
    public void setTheme(Theme t) { theme = t; invalidate(); }
    public void setWallpaperMode(boolean w) { wall = w; invalidate(); }

    public void setSlots(String[] s, int literal) {
        slots = s;
        literalSlot = literal;
        invalidate();
    }

    private boolean empty() {
        for (String s : slots) if (s != null) return false;
        return true;
    }

    @Override
    protected void onMeasure(int w, int h) {
        setMeasuredDimension(MeasureSpec.getSize(w), (int) (42 * dp));
    }

    @Override
    protected void onDraw(Canvas c) {
        if (!wall) c.drawColor(theme.bg);
        int w = getWidth(), h = getHeight();
        float cy = h / 2f;
        if (empty()) {
            text.setTextSize(18 * sp);
            text.setTypeface(Typeface.DEFAULT);
            text.setColor(pressed == 0 ? theme.accent : theme.hint);
            draw(c, "⌄", 28 * dp, cy);
            text.setColor(pressed == 2 ? theme.accent : theme.hint);
            draw(c, "⚙", w - 28 * dp, cy);
            return;
        }
        float cell = w / 3f;
        line.setColor(theme.divider);
        line.setStrokeWidth(dp);
        c.drawLine(cell, h * 0.28f, cell, h * 0.72f, line);
        c.drawLine(cell * 2, h * 0.28f, cell * 2, h * 0.72f, line);
        for (int i = 0; i < 3; i++) {
            if (slots[i] == null) continue;
            text.setTextSize(16 * sp);
            text.setTypeface(i == 1 ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            text.setColor(i == pressed ? theme.accent : theme.stripText);
            String s = i == literalSlot ? "“" + slots[i] + "”" : slots[i];
            CharSequence t = TextUtils.ellipsize(s, text, cell - 12 * dp, TextUtils.TruncateAt.END);
            draw(c, t.toString(), cell * i + cell / 2f, cy);
        }
    }

    private void draw(Canvas c, String s, float cx, float cy) {
        c.drawText(s, cx, cy - (text.descent() + text.ascent()) / 2f, text);
    }

    private int slotAt(float x) {
        int w = getWidth();
        if (empty()) {
            if (x < 64 * dp) return 0;
            if (x > w - 64 * dp) return 2;
            return -1;
        }
        return Math.max(0, Math.min(2, (int) (x / (w / 3f))));
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                pressed = slotAt(e.getX());
                downTime = e.getEventTime();
                invalidate();
                return true;
            case MotionEvent.ACTION_UP: {
                int s = pressed;
                pressed = -1;
                invalidate();
                if (s < 0 || listener == null) return true;
                if (empty()) {
                    if (s == 0) listener.onHide();
                    else listener.onSettings();
                } else if (slots[s] != null) {
                    if (e.getEventTime() - downTime > 600 && s != literalSlot) listener.onSuggestionLongPress(slots[s]);
                    else listener.onSuggestion(slots[s], s == literalSlot);
                }
                return true;
            }
            case MotionEvent.ACTION_CANCEL:
                pressed = -1;
                invalidate();
                return true;
        }
        return true;
    }
}
