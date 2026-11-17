package com.qwerty.keyboard;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public final class Wallpaper {
    private Wallpaper() {}

    public static File file(Context c) {
        return new File(c.getFilesDir(), "wallpaper.jpg");
    }

    public static boolean has(Context c) {
        return file(c).exists();
    }

    public static long stamp(Context c) {
        File f = file(c);
        return f.exists() ? f.lastModified() + f.length() : 0;
    }

    public static Bitmap load(Context c, int targetW) {
        File f = file(c);
        if (!f.exists()) return null;
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(f.getAbsolutePath(), o);
            int s = 1;
            while (targetW > 0 && o.outWidth / (s * 2) >= targetW) s *= 2;
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = s;
            return BitmapFactory.decodeFile(f.getAbsolutePath(), o2);
        } catch (Throwable t) {
            return null;
        }
    }

    public static boolean save(Context c, Uri uri) {
        InputStream in = null;
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            in = c.getContentResolver().openInputStream(uri);
            BitmapFactory.decodeStream(in, null, o);
            in.close();
            int s = 1;
            while (Math.max(o.outWidth, o.outHeight) / (s * 2) >= 1440) s *= 2;
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = s;
            in = c.getContentResolver().openInputStream(uri);
            Bitmap b = BitmapFactory.decodeStream(in, null, o2);
            in.close();
            in = null;
            if (b == null) return false;
            File tmp = new File(c.getFilesDir(), "wallpaper.tmp");
            FileOutputStream out = new FileOutputStream(tmp);
            b.compress(Bitmap.CompressFormat.JPEG, 88, out);
            out.close();
            b.recycle();
            File dst = file(c);
            if (dst.exists()) dst.delete();
            return tmp.renameTo(dst);
        } catch (Throwable t) {
            return false;
        } finally {
            if (in != null) try { in.close(); } catch (Exception ignored) {}
        }
    }

    public static void clear(Context c) {
        File f = file(c);
        if (f.exists()) f.delete();
    }
}
