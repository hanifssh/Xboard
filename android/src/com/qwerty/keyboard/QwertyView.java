package com.qwerty.keyboard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class QwertyView extends View {

    public interface Listener {
        void onKeyDown(Key k);
        void onKey(Key k);
        void onText(String s);
        void onLongAction(Key k);
        void onCursorMove(int steps);
        void onShiftChordEnd();
        boolean isUpper();
    }

    public static final int SHIFT_OFF = 0, SHIFT_ON = 1, SHIFT_LOCKED = 2;

    private static final int LONG_PRESS_MS = 380;
    private static final int REPEAT_START_MS = 400;
    private static final int REPEAT_MS = 45;

    private final float dp;
    private final float sp;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF tmp = new RectF();
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean wall;

    private List<List<Key>> rows = new ArrayList<>();
    private Theme theme = Theme.get(null);
    private Listener listener;
    private int rowH;
    private boolean showPreview = true;
    private int shiftState = SHIFT_OFF;
    private String enterLabel = "↵";

    private final SparseArray<Ptr> ptrs = new SparseArray<>();

    private Key panelKey;
    private String[] panelAlts;
    private final RectF panelRect = new RectF();
    private float panelCell;
    private int panelSel;
    private int panelPtr = -1;

    private static class Ptr {
        int id;
        Key key;
        float downX, downY, lastX;
        boolean committed, longFired, moved, isShift, chorded;
        Runnable pending;
    }

    public QwertyView(Context c) {
        super(c);
        dp = c.getResources().getDisplayMetrics().density;
        sp = c.getResources().getDisplayMetrics().scaledDensity;
        rowH = (int) (54 * dp);
        textPaint.setTextAlign(Paint.Align.CENTER);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(dp);
        setHapticFeedbackEnabled(false);
    }

    public void setListener(Listener l) { listener = l; }
    public void setTheme(Theme t) { theme = t; invalidate(); }
    public void setShowPreview(boolean b) { showPreview = b; }
    public void setWallpaperMode(boolean w) { wall = w; invalidate(); }
    public void setRowHeight(int px) { rowH = px; requestLayout(); }
    public void setEnterLabel(String s) { enterLabel = s; invalidate(); }
    public void setShift(int s) { if (shiftState != s) { shiftState = s; invalidate(); } }

    public void setRows(List<List<Key>> r) {
        cancelAll();
        rows = r;
        requestLayout();
        layoutKeys(getWidth());
        invalidate();
    }

    public boolean isShiftHeld() {
        for (int i = 0; i < ptrs.size(); i++) if (ptrs.valueAt(i).isShift) return true;
        return false;
    }

    public void cancelAll() {
        for (int i = 0; i < ptrs.size(); i++) {
            Ptr p = ptrs.valueAt(i);
            if (p.pending != null) handler.removeCallbacks(p.pending);
        }
        ptrs.clear();
        closePanel();
        invalidate();
    }

    private int padTop() { return (int) (6 * dp); }
    private int padBottom() { return (int) (8 * dp); }
    private int padH() { return (int) (3 * dp); }

    @Override
    protected void onMeasure(int wSpec, int hSpec) {
        int w = MeasureSpec.getSize(wSpec);
        int h = padTop() + rows.size() * rowH + padBottom();
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        layoutKeys(w);
    }

    private void layoutKeys(int width) {
        if (width <= 0) return;
        float unit = (width - 2f * padH()) / 10f;
        float gapH = 3 * dp, gapV = 5 * dp;
        float y = padTop();
        for (List<Key> row : rows) {
            float total = 0;
            for (Key k : row) total += k.w;
            float x = padH() + (10f - total) * unit / 2f;
            for (Key k : row) {
                float kw = k.w * unit;
                k.hit.set(x, y, x + kw, y + rowH);
                k.rect.set(x + gapH, y + gapV / 2f, x + kw - gapH, y + rowH - gapV / 2f);
                x += kw;
            }
            y += rowH;
        }
    }

    private Key findKey(float x, float y) {
        Key best = null;
        float bestD = Float.MAX_VALUE;
        for (List<Key> row : rows) {
            for (Key k : row) {
                if (k.hit.contains(x, y)) return k;
                float dx = x < k.hit.left ? k.hit.left - x : (x > k.hit.right ? x - k.hit.right : 0);
                float dy = y < k.hit.top ? k.hit.top - y : (y > k.hit.bottom ? y - k.hit.bottom : 0);
                float d = dx * dx + dy * dy * 4;
                if (d < bestD) { bestD = d; best = k; }
            }
        }
        return bestD < (rowH * rowH) ? best : null;
    }

    private boolean isPressed(Key k) {
        for (int i = 0; i < ptrs.size(); i++) if (ptrs.valueAt(i).key == k) return true;
        return false;
    }


    private int tone(int color, int alpha) {
        return wall ? (color & 0x00FFFFFF) | (alpha << 24) : color;
    }

    @Override
    protected void onDraw(Canvas c) {
        if (!wall) c.drawColor(theme.bg);
        stroke.setColor((theme.accent & 0x00FFFFFF) | 0x24000000);
        float r = 7 * dp;
        boolean upper = listener != null && listener.isUpper();
        for (List<Key> row : rows) {
            for (Key k : row) {
                boolean pressed = isPressed(k);
                int bg, fg = theme.text;
                if (k.code == Key.ENTER) {
                    bg = pressed ? theme.accentPressed : theme.accent;
                    fg = theme.accentText;
                } else if (k.code == Key.SHIFT && shiftState != SHIFT_OFF) {
                    bg = pressed ? theme.funcPressed : theme.keyPressed;
                    fg = shiftState == SHIFT_LOCKED ? theme.accent : theme.text;
                } else if (k.func && k.code != Key.SPACE) {
                    bg = pressed ? theme.funcPressed : theme.func;
                } else {
                    bg = pressed ? theme.keyPressed : theme.key;
                }
                tmp.set(k.rect);
                tmp.offset(0, 1.5f * dp);
                fill.setColor(tone(theme.shadow, 0x60));
                c.drawRoundRect(tmp, r, r, fill);
                fill.setColor(k.code == Key.ENTER || pressed ? bg : tone(bg, 0xB4));
                c.drawRoundRect(k.rect, r, r, fill);
                if (k.code != Key.ENTER) c.drawRoundRect(k.rect, r, r, stroke);

                String label = labelFor(k, upper);
                float size;
                if (k.emoji) size = 24 * sp;
                else if (k.code == Key.SPACE) size = 12 * sp;
                else if (k.code == Key.SHIFT || k.code == Key.DELETE) size = 22 * sp;
                else if (label.length() == 1 && !k.func) size = 23 * sp;
                else size = 15 * sp;
                textPaint.setTextSize(size);
                textPaint.setColor(k.code == Key.SPACE ? theme.hint : fg);
                textPaint.setTypeface(k.func ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
                drawCentered(c, label, k.rect.centerX(), k.rect.centerY());

                if (k.hint != null) {
                    textPaint.setTextSize(10 * sp);
                    textPaint.setColor(theme.hint);
                    textPaint.setTypeface(Typeface.DEFAULT);
                    c.drawText(k.hint, k.rect.right - 7 * dp, k.rect.top + 13 * dp, textPaint);
                }
            }
        }

        if (showPreview && panelKey == null) {
            for (int i = 0; i < ptrs.size(); i++) {
                Ptr p = ptrs.valueAt(i);
                if (p.committed || p.longFired || p.key.func) continue;
                drawPreview(c, p.key, labelFor(p.key, upper));
            }
        }
        if (panelKey != null) drawPanel(c, upper);
    }

    private String labelFor(Key k, boolean upper) {
        if (k.code == Key.ENTER) return enterLabel;
        if (k.code == Key.SHIFT) return shiftState == SHIFT_LOCKED ? "⇪" : "⇧";
        if (k.isLetter() && upper) return k.label.toUpperCase();
        return k.label;
    }

    private void drawCentered(Canvas c, String s, float cx, float cy) {
        c.drawText(s, cx, cy - (textPaint.descent() + textPaint.ascent()) / 2f, textPaint);
    }

    private void drawPreview(Canvas c, Key k, String label) {
        float w = Math.max(k.rect.width() * 1.25f, 44 * dp);
        float h = rowH * 1.15f;
        float left = k.rect.centerX() - w / 2f;
        left = Math.max(2 * dp, Math.min(left, getWidth() - w - 2 * dp));
        float top = k.rect.top - h - 4 * dp;
        if (top < 0) top = 0;
        tmp.set(left, top, left + w, top + h);
        float r = 10 * dp;
        RectF sh = new RectF(tmp);
        sh.offset(0, 2 * dp);
        fill.setColor(theme.shadow);
        c.drawRoundRect(sh, r, r, fill);
        fill.setColor(theme.previewBg);
        c.drawRoundRect(tmp, r, r, fill);
        textPaint.setTextSize((k.emoji ? 30 : 30) * sp);
        textPaint.setColor(theme.text);
        textPaint.setTypeface(Typeface.DEFAULT);
        drawCentered(c, label, tmp.centerX(), tmp.centerY());
    }

    private void drawPanel(Canvas c, boolean upper) {
        float r = 10 * dp;
        RectF sh = new RectF(panelRect);
        sh.offset(0, 2 * dp);
        fill.setColor(theme.shadow);
        c.drawRoundRect(sh, r, r, fill);
        fill.setColor(theme.previewBg);
        c.drawRoundRect(panelRect, r, r, fill);
        for (int i = 0; i < panelAlts.length; i++) {
            float l = panelRect.left + i * panelCell;
            tmp.set(l + 3 * dp, panelRect.top + 4 * dp, l + panelCell - 3 * dp, panelRect.bottom - 4 * dp);
            if (i == panelSel) {
                fill.setColor(theme.accent);
                c.drawRoundRect(tmp, 7 * dp, 7 * dp, fill);
            }
            textPaint.setTextSize(22 * sp);
            textPaint.setTypeface(Typeface.DEFAULT);
            textPaint.setColor(i == panelSel ? theme.accentText : theme.text);
            drawCentered(c, caseAlt(panelAlts[i], upper), tmp.centerX(), tmp.centerY());
        }
    }

    private String caseAlt(String s, boolean upper) {
        return upper ? s.toUpperCase() : s;
    }


    @Override
    public boolean onTouchEvent(MotionEvent e) {
        int action = e.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {
                int idx = e.getActionIndex();
                down(e.getPointerId(idx), e.getX(idx), e.getY(idx));
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                for (int i = 0; i < e.getPointerCount(); i++) move(e.getPointerId(i), e.getX(i), e.getY(i));
                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP: {
                int idx = e.getActionIndex();
                up(e.getPointerId(idx));
                break;
            }
            case MotionEvent.ACTION_CANCEL:
                cancelAll();
                break;
        }
        return true;
    }

    private void down(int id, float x, float y) {
        if (listener == null || panelKey != null) return;
        final Key k = findKey(x, y);
        if (k == null) return;

        for (int i = 0; i < ptrs.size(); i++) {
            Ptr o = ptrs.valueAt(i);
            if (!o.committed && !o.longFired && !o.moved && !o.isShift && !o.key.repeat) {
                if (o.pending != null) handler.removeCallbacks(o.pending);
                o.committed = true;
                commit(o.key);
            }
        }

        final Ptr p = new Ptr();
        p.id = id;
        p.key = k;
        p.downX = p.lastX = x;
        p.downY = y;
        ptrs.put(id, p);
        listener.onKeyDown(k);

        if (k.code == Key.SHIFT) {
            p.isShift = true;
            p.committed = true;
            listener.onKey(k);
        } else if (k.repeat) {
            p.committed = true;
            commit(k);
            p.pending = new Runnable() {
                @Override public void run() {
                    if (ptrs.get(p.id) != p) return;
                    listener.onKey(k);
                    handler.postDelayed(this, REPEAT_MS);
                }
            };
            handler.postDelayed(p.pending, REPEAT_START_MS);
        } else {
            scheduleLong(p);
        }
        invalidate();
    }

    private void scheduleLong(final Ptr p) {
        if (p.pending != null) handler.removeCallbacks(p.pending);
        p.pending = null;
        final Key k = p.key;
        if (k.alts == null && !k.longAction) return;
        p.pending = new Runnable() {
            @Override public void run() {
                if (ptrs.get(p.id) != p || p.committed || p.moved) return;
                p.longFired = true;
                if (k.alts != null && k.alts.length > 1) {
                    openPanel(p, k);
                } else if (k.alts != null) {
                    listener.onText(caseAlt(k.alts[0], listener.isUpper()));
                    markChord();
                } else {
                    listener.onLongAction(k);
                }
                invalidate();
            }
        };
        handler.postDelayed(p.pending, LONG_PRESS_MS);
    }

    private void move(int id, float x, float y) {
        Ptr p = ptrs.get(id);
        if (p == null) return;
        if (id == panelPtr) {
            int sel = (int) ((x - panelRect.left) / panelCell);
            sel = Math.max(0, Math.min(panelAlts.length - 1, sel));
            if (sel != panelSel) { panelSel = sel; invalidate(); }
            return;
        }
        if (p.committed || p.longFired) return;
        if (p.key.code == Key.SPACE) {
            if (!p.moved && Math.abs(x - p.downX) > 14 * dp) {
                p.moved = true;
                p.lastX = x;
                if (p.pending != null) handler.removeCallbacks(p.pending);
            }
            if (p.moved) {
                float step = 9 * dp;
                int steps = (int) ((x - p.lastX) / step);
                if (steps != 0) {
                    listener.onCursorMove(steps);
                    p.lastX += steps * step;
                }
            }
            return;
        }
        Key k = findKey(x, y);
        if (k != null && k != p.key && !p.key.func && !k.func) {
            p.key = k;
            scheduleLong(p);
            invalidate();
        }
    }

    private void up(int id) {
        Ptr p = ptrs.get(id);
        if (p == null) return;
        ptrs.remove(id);
        if (p.pending != null) handler.removeCallbacks(p.pending);
        if (id == panelPtr) {
            String s = caseAlt(panelAlts[panelSel], listener.isUpper());
            closePanel();
            listener.onText(s);
            markChord();
        } else if (p.isShift) {
            if (p.chorded) listener.onShiftChordEnd();
        } else if (!p.committed && !p.longFired && !p.moved) {
            commit(p.key);
        }
        invalidate();
    }

    private void commit(Key k) {
        listener.onKey(k);
        if (k.code != Key.SHIFT) markChord();
    }

    private void markChord() {
        for (int i = 0; i < ptrs.size(); i++) {
            Ptr o = ptrs.valueAt(i);
            if (o.isShift) o.chorded = true;
        }
    }

    private void openPanel(Ptr p, Key k) {
        panelKey = k;
        panelAlts = k.alts;
        panelPtr = p.id;
        int n = panelAlts.length;
        float maxW = getWidth() - 8 * dp;
        panelCell = Math.min(Math.max(k.rect.width(), 36 * dp), maxW / n);
        float w = panelCell * n;
        float left = k.rect.left - 2 * dp;
        left = Math.max(4 * dp, Math.min(left, getWidth() - w - 4 * dp));
        float h = rowH;
        float top = k.rect.top - h - 4 * dp;
        if (top < 0) top = 0;
        panelRect.set(left, top, left + w, top + h);
        int sel = (int) ((p.downX - left) / panelCell);
        panelSel = Math.max(0, Math.min(n - 1, sel));
    }

    private void closePanel() {
        panelKey = null;
        panelAlts = null;
        panelPtr = -1;
    }
}
