package com.qwerty.keyboard;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.inputmethodservice.InputMethodService;
import android.media.AudioManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.List;

public class QwertyIME extends InputMethodService implements QwertyView.Listener, SuggestionStrip.Listener, XBar.Listener {

    private XRoot root;
    private XBar bar;
    private QwertyView keyboard;
    private SuggestionStrip strip;
    private Bitmap wallBmp;
    private long wallStamp = -1;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean running;
    private int sent, total;
    private String autoMessage;
    private SuggestionEngine engine;
    private Prefs prefs;
    private AudioManager audio;
    private Vibrator vibrator;

    private int mode = Layouts.ALPHA;
    private int field = Layouts.FIELD_TEXT;
    private int shift = QwertyView.SHIFT_OFF;
    private long lastShiftTap;
    private long lastSpaceTime;
    private boolean autoSpaced;
    private boolean noLearn;
    private boolean suggestionsOn;
    private boolean autoCapAllowed;
    private int emojiPage;

    @Override
    public void onCreate() {
        super.onCreate();
        engine = new SuggestionEngine(this);
        audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        prefs = Prefs.load(this);
    }

    @Override
    public View onCreateInputView() {
        root = new XRoot(this);
        bar = new XBar(this);
        bar.setListener(this);
        strip = new SuggestionStrip(this);
        strip.setListener(this);
        keyboard = new QwertyView(this);
        keyboard.setListener(this);
        root.addView(bar, new LinearLayout.LayoutParams(-1, -2));
        root.addView(strip, new LinearLayout.LayoutParams(-1, -2));
        root.addView(keyboard, new LinearLayout.LayoutParams(-1, -2));
        wallStamp = -1;
        applyPrefs();
        return root;
    }

    @Override
    public boolean onEvaluateFullscreenMode() {
        return false;
    }

    private void applyPrefs() {
        prefs = Prefs.load(this);
        if (keyboard == null) return;
        Theme t = Theme.get(prefs.theme);
        keyboard.setTheme(t);
        strip.setTheme(t);
        bar.setTheme(t);
        keyboard.setShowPreview(prefs.preview);
        applyWallpaper(t);
        applyScale();
        bar.setScale(prefs.scale);
        bar.setRunning(running);
    }

    private void applyWallpaper(Theme t) {
        long stamp = Wallpaper.stamp(this);
        if (stamp != wallStamp) {
            wallStamp = stamp;
            Bitmap old = wallBmp;
            wallBmp = stamp == 0 ? null : Wallpaper.load(this, getResources().getDisplayMetrics().widthPixels);
            root.setWallpaper(wallBmp, 0);
            if (old != null && old != wallBmp) old.recycle();
        }
        boolean w = wallBmp != null;
        root.setWallpaper(wallBmp, (t.bg & 0x00FFFFFF) | 0x73000000);
        keyboard.setWallpaperMode(w);
        strip.setWallpaperMode(w);
        bar.setWallpaperMode(w);
    }

    private void applyScale() {
        float dp = getResources().getDisplayMetrics().density;
        boolean land = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        float base = land ? 40 : 54;
        keyboard.setRowHeight((int) (base * prefs.scale / 100f * dp));
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        applyPrefs();
        int cls = info.inputType & InputType.TYPE_MASK_CLASS;
        int var = info.inputType & InputType.TYPE_MASK_VARIATION;
        boolean password = false;
        field = Layouts.FIELD_TEXT;
        autoCapAllowed = prefs.autocap;
        if (cls == InputType.TYPE_CLASS_NUMBER || cls == InputType.TYPE_CLASS_DATETIME) {
            mode = Layouts.NUMBER;
            password = var == InputType.TYPE_NUMBER_VARIATION_PASSWORD;
        } else if (cls == InputType.TYPE_CLASS_PHONE) {
            mode = Layouts.PHONE;
        } else {
            mode = Layouts.ALPHA;
            password = var == InputType.TYPE_TEXT_VARIATION_PASSWORD
                    || var == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    || var == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD;
            if (var == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS || var == InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS) {
                field = Layouts.FIELD_EMAIL;
            } else if (var == InputType.TYPE_TEXT_VARIATION_URI) {
                field = Layouts.FIELD_URI;
            }
        }
        if (password || field != Layouts.FIELD_TEXT || cls != InputType.TYPE_CLASS_TEXT) autoCapAllowed = false;
        noLearn = password;
        suggestionsOn = prefs.suggest && cls == InputType.TYPE_CLASS_TEXT && !password && field == Layouts.FIELD_TEXT
                && (info.inputType & InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS) == 0;
        strip.setVisibility(View.VISIBLE);
        shift = QwertyView.SHIFT_OFF;
        emojiPage = 0;
        autoSpaced = false;
        rebuild();
        updateEnter();
        updateShift();
        updateSuggestions();
    }

    @Override
    public void onFinishInputView(boolean finishing) {
        super.onFinishInputView(finishing);
        stopAuto();
        if (bar != null && bar.isResizing()) bar.setResizing(false);
        if (keyboard != null) keyboard.cancelAll();
        engine.save();
    }

    @Override
    public void onDestroy() {
        stopAuto();
        engine.save();
        super.onDestroy();
    }

    @Override
    public void onUpdateSelection(int os, int oe, int ns, int ne, int cs, int ce) {
        super.onUpdateSelection(os, oe, ns, ne, cs, ce);
        updateShift();
        updateSuggestions();
    }

    private void rebuild() {
        if (keyboard != null) keyboard.setRows(Layouts.build(mode, field, prefs.numberRow, emojiPage));
    }

    private void updateEnter() {
        EditorInfo info = getCurrentInputEditorInfo();
        String label = "↵";
        if (info != null && (info.imeOptions & EditorInfo.IME_FLAG_NO_ENTER_ACTION) == 0) {
            switch (info.imeOptions & EditorInfo.IME_MASK_ACTION) {
                case EditorInfo.IME_ACTION_GO: label = "Go"; break;
                case EditorInfo.IME_ACTION_SEARCH: label = "⌕"; break;
                case EditorInfo.IME_ACTION_SEND: label = "Send"; break;
                case EditorInfo.IME_ACTION_NEXT: label = "Next"; break;
                case EditorInfo.IME_ACTION_DONE: label = "Done"; break;
                case EditorInfo.IME_ACTION_PREVIOUS: label = "Prev"; break;
            }
        }
        if (keyboard != null) keyboard.setEnterLabel(label);
    }

    private void updateShift() {
        if (keyboard == null) return;
        if (mode != Layouts.ALPHA) { keyboard.setShift(QwertyView.SHIFT_OFF); return; }
        if (shift == QwertyView.SHIFT_LOCKED) { keyboard.setShift(shift); return; }
        InputConnection ic = getCurrentInputConnection();
        EditorInfo info = getCurrentInputEditorInfo();
        if (autoCapAllowed && ic != null && info != null) {
            int caps = ic.getCursorCapsMode(info.inputType | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            shift = caps != 0 ? QwertyView.SHIFT_ON : QwertyView.SHIFT_OFF;
        } else if (!keyboard.isShiftHeld() && shift == QwertyView.SHIFT_ON && autoCapAllowed) {
            shift = QwertyView.SHIFT_OFF;
        }
        keyboard.setShift(shift);
    }


    private static boolean isWordChar(char c) {
        return Character.isLetterOrDigit(c) || c == '\'' || c == '’';
    }

    private String currentWord() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return "";
        CharSequence sel = ic.getSelectedText(0);
        if (sel != null && sel.length() > 0) return "";
        CharSequence before = ic.getTextBeforeCursor(48, 0);
        if (before == null) return "";
        int i = before.length();
        while (i > 0 && isWordChar(before.charAt(i - 1))) i--;
        String w = before.subSequence(i, before.length()).toString();
        while (w.startsWith("'") || w.startsWith("’")) w = w.substring(1);
        return w;
    }

    private int wordAfterLength() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return 0;
        CharSequence after = ic.getTextAfterCursor(48, 0);
        if (after == null) return 0;
        int i = 0;
        while (i < after.length() && isWordChar(after.charAt(i))) i++;
        return i;
    }

    private void updateSuggestions() {
        if (strip == null) return;
        if (!suggestionsOn) { strip.setSlots(new String[3], -1); return; }
        String typed = currentWord();
        if (typed.isEmpty() || !Character.isLetter(typed.charAt(0))) { strip.setSlots(new String[3], -1); return; }
        List<String> s = engine.suggest(typed, 3);
        String[] slots = new String[3];
        int literal = -1;
        String top = s.isEmpty() ? typed : s.get(0);
        slots[1] = top;
        int next = s.isEmpty() ? 0 : 1;
        if (!typed.equalsIgnoreCase(top)) {
            slots[0] = typed;
            literal = 0;
        } else if (next < s.size()) {
            slots[0] = s.get(next++);
        }
        while (next < s.size() && (s.get(next).equalsIgnoreCase(typed) || s.get(next).equals(slots[0]))) next++;
        if (next < s.size()) slots[2] = s.get(next);
        strip.setSlots(slots, literal);
    }

    private void learnCurrent() {
        if (noLearn || !suggestionsOn) return;
        String w = currentWord();
        if (w.length() >= 2 && Character.isLetter(w.charAt(0))) engine.learn(w);
    }


    @Override
    public void onKeyDown(Key k) {
        if (prefs.vibrate && vibrator != null && vibrator.hasVibrator()) {
            try {
                if (Build.VERSION.SDK_INT >= 26) {
                    vibrator.vibrate(VibrationEffect.createOneShot(14, 90));
                } else {
                    vibrator.vibrate(14);
                }
            } catch (Exception ignored) {}
        }
        if (prefs.sound && audio != null) {
            int fx = AudioManager.FX_KEYPRESS_STANDARD;
            if (k.code == Key.DELETE) fx = AudioManager.FX_KEYPRESS_DELETE;
            else if (k.code == Key.SPACE) fx = AudioManager.FX_KEYPRESS_SPACEBAR;
            else if (k.code == Key.ENTER) fx = AudioManager.FX_KEYPRESS_RETURN;
            audio.playSoundEffect(fx, -1f);
        }
    }

    @Override
    public boolean isUpper() {
        return mode == Layouts.ALPHA && (shift != QwertyView.SHIFT_OFF || (keyboard != null && keyboard.isShiftHeld()));
    }

    @Override
    public void onKey(Key k) {
        InputConnection ic = getCurrentInputConnection();
        switch (k.code) {
            case Key.SHIFT: {
                long now = SystemClock.uptimeMillis();
                if (shift == QwertyView.SHIFT_LOCKED) shift = QwertyView.SHIFT_OFF;
                else if (now - lastShiftTap < 320) shift = QwertyView.SHIFT_LOCKED;
                else shift = shift == QwertyView.SHIFT_ON ? QwertyView.SHIFT_OFF : QwertyView.SHIFT_ON;
                lastShiftTap = now;
                keyboard.setShift(shift);
                keyboard.invalidate();
                return;
            }
            case Key.DELETE: handleDelete(ic); return;
            case Key.MODE_SYM: setMode(Layouts.SYM1); return;
            case Key.MODE_SYM2: setMode(Layouts.SYM2); return;
            case Key.MODE_ALPHA: setMode(Layouts.ALPHA); return;
            case Key.EMOJI: emojiPage = 0; setMode(Layouts.EMOJI); return;
            case Key.EMOJI_PREV:
                emojiPage = (emojiPage + Layouts.EMOJI_PAGES.length - 1) % Layouts.EMOJI_PAGES.length;
                rebuild();
                return;
            case Key.EMOJI_NEXT:
                emojiPage = (emojiPage + 1) % Layouts.EMOJI_PAGES.length;
                rebuild();
                return;
            case Key.ENTER: handleEnter(ic); return;
            case Key.SPACE: handleSpace(ic); return;
            default: onText(k.text != null ? k.text : k.label);
        }
    }

    private void setMode(int m) {
        mode = m;
        if (m != Layouts.ALPHA) shift = QwertyView.SHIFT_OFF;
        rebuild();
        updateShift();
    }

    @Override
    public void onText(String s) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null || s == null) return;
        if (mode == Layouts.ALPHA && isUpper() && s.length() == 1) s = s.toUpperCase();
        boolean punct = s.length() == 1 && ".,!?;:)".indexOf(s.charAt(0)) >= 0;
        if (punct) learnCurrent();
        if (punct && autoSpaced) {
            CharSequence b = ic.getTextBeforeCursor(1, 0);
            if (b != null && b.length() == 1 && b.charAt(0) == ' ') {
                ic.beginBatchEdit();
                ic.deleteSurroundingText(1, 0);
                ic.commitText(s + (s.equals(")") ? "" : " "), 1);
                ic.endBatchEdit();
                autoSpaced = !s.equals(")");
                afterCommit();
                return;
            }
        }
        ic.commitText(s, 1);
        autoSpaced = false;
        afterCommit();
    }

    private void afterCommit() {
        if (shift == QwertyView.SHIFT_ON && !keyboard.isShiftHeld()) {
            shift = QwertyView.SHIFT_OFF;
            keyboard.setShift(shift);
        }
        lastSpaceTime = 0;
        keyboard.invalidate();
    }

    private void handleSpace(InputConnection ic) {
        if (ic == null) return;
        long now = SystemClock.uptimeMillis();
        learnCurrent();
        if (prefs.doubleSpace && mode == Layouts.ALPHA && now - lastSpaceTime < 700) {
            CharSequence b = ic.getTextBeforeCursor(2, 0);
            if (b != null && b.length() == 2 && b.charAt(1) == ' ' && Character.isLetterOrDigit(b.charAt(0))) {
                ic.beginBatchEdit();
                ic.deleteSurroundingText(1, 0);
                ic.commitText(". ", 1);
                ic.endBatchEdit();
                lastSpaceTime = 0;
                autoSpaced = false;
                return;
            }
        }
        ic.commitText(" ", 1);
        lastSpaceTime = now;
        autoSpaced = false;
        if (mode == Layouts.SYM1 || mode == Layouts.SYM2) setMode(Layouts.ALPHA);
        if (shift == QwertyView.SHIFT_ON && !keyboard.isShiftHeld()) shift = QwertyView.SHIFT_OFF;
    }

    private void handleDelete(InputConnection ic) {
        if (ic == null) return;
        autoSpaced = false;
        lastSpaceTime = 0;
        CharSequence sel = ic.getSelectedText(0);
        if (sel != null && sel.length() > 0) {
            ic.commitText("", 1);
            return;
        }
        CharSequence b = ic.getTextBeforeCursor(2, 0);
        if (b == null || b.length() == 0) {
            sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL);
            return;
        }
        if (b.length() == 2 && Character.isSurrogatePair(b.charAt(0), b.charAt(1))) {
            ic.deleteSurroundingText(2, 0);
        } else if (b.charAt(b.length() - 1) == '\uFE0F' && b.length() == 2) {
            ic.deleteSurroundingText(2, 0);
        } else {
            ic.deleteSurroundingText(1, 0);
        }
    }

    private void handleEnter(InputConnection ic) {
        if (ic == null) return;
        learnCurrent();
        autoSpaced = false;
        pressEnter(ic);
    }

    private void pressEnter(InputConnection ic) {
        EditorInfo info = getCurrentInputEditorInfo();
        if (info != null && (info.imeOptions & EditorInfo.IME_FLAG_NO_ENTER_ACTION) == 0) {
            int action = info.imeOptions & EditorInfo.IME_MASK_ACTION;
            if (action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
                ic.performEditorAction(action);
                return;
            }
        }
        sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER);
    }

    @Override
    public void onLongAction(Key k) {
        if (k.code == Key.SPACE) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showInputMethodPicker();
        }
    }

    @Override
    public void onCursorMove(int steps) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        ExtractedText et = ic.getExtractedText(new ExtractedTextRequest(), 0);
        if (et != null && et.text != null) {
            int pos = et.startOffset + et.selectionEnd + steps;
            int len = et.startOffset + et.text.length();
            pos = Math.max(0, Math.min(len, pos));
            ic.setSelection(pos, pos);
        } else {
            int code = steps < 0 ? KeyEvent.KEYCODE_DPAD_LEFT : KeyEvent.KEYCODE_DPAD_RIGHT;
            for (int i = 0; i < Math.abs(steps); i++) sendDownUpKeyEvents(code);
        }
    }

    @Override
    public void onShiftChordEnd() {
        if (shift == QwertyView.SHIFT_ON) {
            shift = QwertyView.SHIFT_OFF;
            keyboard.setShift(shift);
        }
        keyboard.invalidate();
    }


    @Override
    public void onSuggestion(String word, boolean literal) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        String cur = currentWord();
        int after = wordAfterLength();
        ic.beginBatchEdit();
        if (after > 0) ic.deleteSurroundingText(0, after);
        ic.deleteSurroundingText(cur.length(), 0);
        ic.commitText(word + " ", 1);
        ic.endBatchEdit();
        if (!noLearn) {
            engine.learn(word);
            if (literal) engine.learn(word);
        }
        autoSpaced = true;
        lastSpaceTime = 0;
        if (shift == QwertyView.SHIFT_ON) shift = QwertyView.SHIFT_OFF;
    }

    @Override
    public void onSuggestionLongPress(String word) {
        engine.forget(word);
        updateSuggestions();
    }

    @Override
    public void onHide() {
        requestHideSelf(0);
    }

    @Override
    public void onSettings() {
        Intent i = new Intent(this, SettingsActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }

    private final Runnable typeStep = new Runnable() {
        @Override public void run() {
            if (!running) return;
            InputConnection ic = getCurrentInputConnection();
            if (ic == null) { stopAuto(); return; }
            ic.commitText(autoMessage, 1);
            handler.postDelayed(enterStep, AutoSend.ENTER_DELAY_MS);
        }
    };

    private final Runnable enterStep = new Runnable() {
        @Override public void run() {
            if (!running) return;
            InputConnection ic = getCurrentInputConnection();
            if (ic == null) { stopAuto(); return; }
            pressEnter(ic);
            sent++;
            if (bar != null) bar.setProgress(sent, total);
            if (sent >= total) { stopAuto(); return; }
            handler.postDelayed(typeStep, AutoSend.CYCLE_MS - AutoSend.ENTER_DELAY_MS);
        }
    };

    @Override
    public void onStartAuto() {
        if (running) return;
        prefs = Prefs.load(this);
        if (prefs.message == null || prefs.message.isEmpty()) {
            Toast.makeText(this, "Set your message in the Xboard app first", Toast.LENGTH_SHORT).show();
            return;
        }
        if (getCurrentInputConnection() == null) return;
        autoMessage = prefs.message;
        total = prefs.amount;
        sent = 0;
        running = true;
        bar.setRunning(true);
        bar.setProgress(0, total);
        handler.post(typeStep);
    }

    @Override
    public void onStopAuto() {
        stopAuto();
    }

    private void stopAuto() {
        handler.removeCallbacks(typeStep);
        handler.removeCallbacks(enterStep);
        if (!running) return;
        running = false;
        if (bar != null) bar.setRunning(false);
    }

    @Override
    public void onSwitchKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.showInputMethodPicker();
    }

    @Override
    public void onResizeOpen() {
        bar.setScale(prefs.scale);
        bar.setResizing(true);
    }

    @Override
    public void onResizeStep(int dir) {
        int v = Prefs.clampScale(prefs.scale + dir * Prefs.SCALE_STEP);
        if (v == prefs.scale) return;
        prefs.scale = v;
        Prefs.sp(this).edit().putInt(Prefs.SCALE, v).apply();
        applyScale();
        bar.setScale(v);
    }

    @Override
    public void onResizeDone() {
        bar.setResizing(false);
    }
}
