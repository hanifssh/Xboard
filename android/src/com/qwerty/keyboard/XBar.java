package com.qwerty.keyboard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;

public class XBar extends View {

    public interface Listener {
        void onStartAuto();
        void onStopAuto();
        void onSwitchKeyboard();
        void onResizeStep(int dir);
        void onResizeDone();
        void onResizeOpen();
    }

    private static final int B_RESIZE = 0, B_SWITCH = 1, B_START = 2, B_STOP = 3, B_MINUS = 4, B_PLUS = 5, B_DONE = 6;
    private static final int STOP_RED = 0xFFE5484D;

    private final float dp, sp;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF[] rects = new RectF[7];
    private final boolean[] vis = new boolean[7];
    private Theme theme = Theme.get(null);
    private Listener listener;
    private boolean wall, resizing, running;
    private int sent, total, scale = 100;
    private int pressed = -1;

    public XBar(Context c) {
        super(c);
        dp = c.getResources().getDisplayMetrics().density;
        sp = c.getResources().getDisplayMetrics().scaledDensity;
        for (int i = 0; i < rects.length; i++) rects[i] = new RectF();
        text.setTextAlign(Paint.Align.LEFT);
    }

    public void setListener(Listener l) { listener = l; }
    public void setTheme(Theme t) { theme = t; invalidate(); }
    public void setWallpaperMode(boolean w) { wall = w; invalidate(); }
    public void setScale(int s) { scale = s; invalidate(); }
    public void setResizing(boolean r) { resizing = r; invalidate(); }
    public boolean isResizing() { return resizing; }
    public void setRunning(boolean r) { running = r; invalidate(); }
    public void setProgress(int s, int t) { sent = s; total = t; invalidate(); }

    @Override
    protected void onMeasure(int w, int h) {
        setMeasuredDimension(MeasureSpec.getSize(w), (int) (44 * dp));
    }

    private String label(int b) {
        switch (b) {
            case B_RESIZE: return "⇕";
            case B_SWITCH: return "🌐";
            case B_START: return "Start";
            case B_STOP: return "Stop";
            case B_MINUS: return "−";
            case B_PLUS: return "+";
            default: return "Done";
        }
    }

    private boolean hasIcon(int b) { return b == B_START || b == B_STOP; }

    private void setButtonText(int b) {
        text.setTextSize((b == B_RESIZE || b == B_MINUS || b == B_PLUS ? 18 : b == B_SWITCH ? 15 : 13) * sp);
        text.setTypeface(Typeface.DEFAULT_BOLD);
    }

    private float buttonWidth(int b) {
        setButtonText(b);
        float w = text.measureText(label(b)) + 22 * dp;
        if (hasIcon(b)) w += 14 * dp;
        return Math.max(w, 38 * dp);
    }

    private void layout() {
        for (int i = 0; i < vis.length; i++) vis[i] = false;
        float bh = 32 * dp;
        float top = (getHeight() - bh) / 2f;
        float right = getWidth() - 8 * dp;
        float gap = 6 * dp;
        int[] order = resizing ? new int[]{B_DONE, B_PLUS, B_MINUS} : new int[]{B_STOP, B_START, B_SWITCH, B_RESIZE};
        for (int b : order) {
            float w = buttonWidth(b);
            rects[b].set(right - w, top, right, top + bh);
            vis[b] = true;
            right -= w + gap;
        }
    }

    private float leftmostButton() {
        float m = getWidth();
        for (int i = 0; i < vis.length; i++) if (vis[i]) m = Math.min(m, rects[i].left);
        return m;
    }

    @Override
    protected void onDraw(Canvas c) {
        layout();
        if (!wall) c.drawColor(theme.bg);
        float cy = getHeight() / 2f;
        float x = 12 * dp;
        text.setTextSize(19 * sp);
        text.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC));
        text.setColor(theme.accent);
        float baseline = cy - (text.descent() + text.ascent()) / 2f;
        c.drawText("X", x, baseline, text);
        x += text.measureText("X");
        text.setColor(theme.text);
        c.drawText("board", x, baseline, text);
        x += text.measureText("board") + 10 * dp;

        String info = null;
        int infoColor = theme.hint;
        if (resizing) info = "Size " + scale + "%";
        else if (running) { info = sent + "/" + total; infoColor = theme.accent; }
        else if (total > 0 && sent > 0) info = sent + "/" + total;
        if (info != null) {
            text.setTextSize(13 * sp);
            text.setTypeface(Typeface.DEFAULT_BOLD);
            if (x + text.measureText(info) < leftmostButton() - 6 * dp) {
                text.setColor(infoColor);
                c.drawText(info, x, cy - (text.descent() + text.ascent()) / 2f, text);
            }
        }

        float r = 16 * dp;
        for (int b = 0; b < rects.length; b++) {
            if (!vis[b]) continue;
            RectF rc = rects[b];
            boolean p = pressed == b;
            int bg, fg;
            if (b == B_START) {
                bg = p ? theme.accentPressed : theme.accent;
                fg = theme.accentText;
            } else if (b == B_STOP) {
                bg = running ? (p ? 0xFFC23338 : STOP_RED) : (p ? theme.funcPressed : theme.func);
                fg = running ? 0xFFFFFFFF : theme.hint;
            } else if (b == B_DONE) {
                bg = p ? theme.accentPressed : theme.accent;
                fg = theme.accentText;
            } else {
                bg = p ? theme.funcPressed : theme.func;
                fg = theme.text;
            }
            fill.setColor(bg);
            if (b == B_START && running) fill.setAlpha(110);
            c.drawRoundRect(rc, r, r, fill);
            setButtonText(b);
            text.setColor(fg);
            String s = label(b);
            float tw = text.measureText(s);
            float content = tw + (hasIcon(b) ? 14 * dp : 0);
            float tx = rc.centerX() - content / 2f;
            float ty = rc.centerY() - (text.descent() + text.ascent()) / 2f;
            if (hasIcon(b)) {
                fill.setColor(fg);
                float ic = rc.centerY();
                if (b == B_START) {
                    path.reset();
                    path.moveTo(tx, ic - 5 * dp);
                    path.lineTo(tx + 9 * dp, ic);
                    path.lineTo(tx, ic + 5 * dp);
                    path.close();
                    c.drawPath(path, fill);
                } else {
                    c.drawRoundRect(new RectF(tx, ic - 4.5f * dp, tx + 9 * dp, ic + 4.5f * dp), 1.5f * dp, 1.5f * dp, fill);
                }
                tx += 14 * dp;
            }
            c.drawText(s, tx, ty, text);
        }
    }

    private int buttonAt(float x, float y) {
        for (int b = 0; b < rects.length; b++) {
            if (!vis[b]) continue;
            RectF rc = rects[b];
            if (x >= rc.left - 3 * dp && x <= rc.right + 3 * dp && y >= 0 && y <= getHeight()) return b;
        }
        return -1;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                layout();
                pressed = buttonAt(e.getX(), e.getY());
                invalidate();
                return true;
            case MotionEvent.ACTION_UP: {
                int b = pressed;
                pressed = -1;
                invalidate();
                if (b < 0 || listener == null) return true;
                switch (b) {
                    case B_RESIZE: listener.onResizeOpen(); break;
                    case B_SWITCH: listener.onSwitchKeyboard(); break;
                    case B_START: listener.onStartAuto(); break;
                    case B_STOP: listener.onStopAuto(); break;
                    case B_MINUS: listener.onResizeStep(-1); break;
                    case B_PLUS: listener.onResizeStep(1); break;
                    case B_DONE: listener.onResizeDone(); break;
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
