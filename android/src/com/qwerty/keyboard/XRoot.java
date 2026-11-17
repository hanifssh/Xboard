package com.qwerty.keyboard;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.widget.LinearLayout;

public class XRoot extends LinearLayout {
    private Bitmap bmp;
    private int dim;
    private final Rect src = new Rect();
    private final RectF dst = new RectF();
    private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);

    public XRoot(Context c) {
        super(c);
        setOrientation(VERTICAL);
        setWillNotDraw(false);
    }

    public void setWallpaper(Bitmap b, int dimColor) {
        bmp = b;
        dim = dimColor;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        if (bmp == null || bmp.isRecycled()) return;
        float vw = getWidth(), vh = getHeight();
        if (vw <= 0 || vh <= 0) return;
        float s = Math.max(vw / bmp.getWidth(), vh / bmp.getHeight());
        float sw = vw / s, sh = vh / s;
        int l = (int) ((bmp.getWidth() - sw) / 2f), t = (int) ((bmp.getHeight() - sh) / 2f);
        src.set(l, t, l + (int) sw, t + (int) sh);
        dst.set(0, 0, vw, vh);
        c.drawBitmap(bmp, src, dst, paint);
        c.drawColor(dim);
    }
}
