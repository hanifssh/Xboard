package com.qwerty.keyboard;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends Activity {

    private static final int BG = 0xFF070B14, CARD = 0xFF0E1524, FIELD = 0xFF141D31, LINE = 0xFF1C2A44,
            TEXT = 0xFFEAF1FF, MUTED = 0xFF7F8DA8, ACCENT = 0xFF1E9BFF, OK = 0xFF3DDC97;
    private static final int PICK_IMAGE = 7;

    private float dp;
    private TextView status1, status2, sizeLabel;
    private Button btnSelect, btnRemove;
    private ImageView wallPreview;
    private LinearLayout themeRow;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        dp = getResources().getDisplayMetrics().density;
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = px(20);
        root.setPadding(pad, px(28), pad, px(40));
        scroll.addView(root);

        SpannableString name = new SpannableString("Xboard");
        name.setSpan(new ForegroundColorSpan(ACCENT), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        TextView title = text("", 34, TEXT, true);
        title.setText(name);
        title.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC));
        root.addView(title);
        TextView by = label("made by Hanif");
        by.setTextColor(MUTED);
        by.setPadding(0, px(2), 0, px(22));
        root.addView(by);

        LinearLayout setup = card("Setup");
        status1 = text("", 15, TEXT, false);
        status1.setPadding(0, px(10), 0, px(6));
        setup.addView(status1);
        setup.addView(button("1. Enable in system settings", true, new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS));
            }
        }));
        status2 = text("", 15, TEXT, false);
        status2.setPadding(0, px(14), 0, px(6));
        setup.addView(status2);
        btnSelect = button("2. Switch to Xboard", true, new View.OnClickListener() {
            @Override public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.showInputMethodPicker();
            }
        });
        setup.addView(btnSelect);
        root.addView(setup);

        LinearLayout tryCard = card("Try it");
        EditText et = field("Tap here and start typing…");
        et.setMinLines(3);
        et.setGravity(Gravity.TOP);
        tryCard.addView(et, fieldLp());
        root.addView(tryCard);

        final android.content.SharedPreferences sp = Prefs.sp(this);
        Prefs cur = Prefs.load(this);

        LinearLayout auto = card("Auto send");
        auto.addView(small("Message"));
        final EditText msg = field("Type the message to send…");
        msg.setMinLines(2);
        msg.setGravity(Gravity.TOP);
        msg.setText(cur.message);
        msg.addTextChangedListener(new Watcher() {
            @Override public void afterTextChanged(Editable e) {
                sp.edit().putString(Prefs.MESSAGE, e.toString()).apply();
            }
        });
        auto.addView(msg, fieldLp());
        TextView amountLabel = small("Amount (how many times)");
        amountLabel.setPadding(0, px(14), 0, 0);
        auto.addView(amountLabel);
        final EditText amount = field(String.valueOf(AutoSend.AMOUNT_DEFAULT));
        amount.setInputType(InputType.TYPE_CLASS_NUMBER);
        amount.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
        amount.setSingleLine(true);
        amount.setText(String.valueOf(cur.amount));
        amount.setTypeface(Typeface.MONOSPACE);
        amount.addTextChangedListener(new Watcher() {
            @Override public void afterTextChanged(Editable e) {
                sp.edit().putInt(Prefs.AMOUNT, AutoSend.parseAmount(e.toString())).apply();
            }
        });
        auto.addView(amount, fieldLp());
        TextView how = text("Open a chat, tap its message field, then press ▶ Start on the Xboard bar. "
                + "Each round types your message and presses Enter in under 200 ms. Press ■ Stop anytime.\n"
                + "In WhatsApp, turn on Settings › Chats › Enter is send.", 13, MUTED, false);
        how.setLineSpacing(px(3), 1f);
        how.setPadding(0, px(12), 0, 0);
        auto.addView(how);
        root.addView(auto);

        LinearLayout look = card("Look");
        look.addView(small("Theme"));
        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        themeRow = new LinearLayout(this);
        themeRow.setOrientation(LinearLayout.HORIZONTAL);
        themeRow.setPadding(0, px(10), 0, px(4));
        hs.addView(themeRow);
        look.addView(hs);
        buildThemes(cur.theme);

        TextView wl = small("Wallpaper");
        wl.setPadding(0, px(16), 0, 0);
        look.addView(wl);
        wallPreview = new ImageView(this);
        wallPreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        wallPreview.setClipToOutline(true);
        wallPreview.setBackground(rounded(FIELD, 12, LINE));
        LinearLayout.LayoutParams wlp = new LinearLayout.LayoutParams(-1, px(96));
        wlp.topMargin = px(10);
        look.addView(wallPreview, wlp);
        LinearLayout wallBtns = new LinearLayout(this);
        wallBtns.setOrientation(LinearLayout.HORIZONTAL);
        Button pick = button("Choose photo", true, new View.OnClickListener() {
            @Override public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.setType("image/*");
                i.addCategory(Intent.CATEGORY_OPENABLE);
                try {
                    startActivityForResult(Intent.createChooser(i, "Choose wallpaper"), PICK_IMAGE);
                } catch (Exception e) {
                    Toast.makeText(SettingsActivity.this, "No photo app found", Toast.LENGTH_SHORT).show();
                }
            }
        });
        btnRemove = button("Remove", false, new View.OnClickListener() {
            @Override public void onClick(View v) {
                Wallpaper.clear(SettingsActivity.this);
                refreshWallpaper();
            }
        });
        LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(0, px(46), 1f);
        p1.topMargin = px(10);
        LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(0, px(46), 1f);
        p2.topMargin = px(10);
        p2.leftMargin = px(10);
        wallBtns.addView(pick, p1);
        wallBtns.addView(btnRemove, p2);
        look.addView(wallBtns);

        sizeLabel = small("");
        sizeLabel.setPadding(0, px(18), 0, 0);
        look.addView(sizeLabel);
        SeekBar size = new SeekBar(this);
        size.setMax((Prefs.SCALE_MAX - Prefs.SCALE_MIN) / Prefs.SCALE_STEP);
        size.setProgress((cur.scale - Prefs.SCALE_MIN) / Prefs.SCALE_STEP);
        size.setProgressTintList(ColorStateList.valueOf(ACCENT));
        size.setThumbTintList(ColorStateList.valueOf(ACCENT));
        size.setProgressBackgroundTintList(ColorStateList.valueOf(LINE));
        size.setPadding(px(4), px(14), px(4), px(10));
        setSizeLabel(cur.scale);
        size.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int v, boolean user) {
                int scale = Prefs.SCALE_MIN + v * Prefs.SCALE_STEP;
                setSizeLabel(scale);
                if (user) sp.edit().putInt(Prefs.SCALE, scale).apply();
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
        look.addView(size);
        TextView sizeHint = text("You can also tap ⇕ on the Xboard bar while typing.", 13, MUTED, false);
        look.addView(sizeHint);
        root.addView(look);

        LinearLayout pref = card("Typing");
        pref.addView(toggle("Vibrate on keypress", Prefs.VIBRATE, true));
        pref.addView(toggle("Sound on keypress", Prefs.SOUND, false));
        pref.addView(toggle("Popup on keypress", Prefs.PREVIEW, true));
        pref.addView(toggle("Auto-capitalization", Prefs.AUTOCAP, true));
        pref.addView(toggle("Double-space types period", Prefs.DOUBLE_SPACE, true));
        pref.addView(toggle("Word suggestions", Prefs.SUGGEST, true));
        pref.addView(toggle("Number row", Prefs.NUMBER_ROW, false));
        root.addView(pref);

        LinearLayout tips = card("Tips");
        TextView t = text(
                "• Double-tap ⇧ for caps lock\n" +
                "• Hold a key for numbers and accents (é, ñ, ü…)\n" +
                "• Slide on the space bar to move the cursor\n" +
                "• Tap 🌐 on the Xboard bar to change keyboard\n" +
                "• Hold ⌫ to delete continuously\n" +
                "• Tap a suggestion to complete the word\n" +
                "• Hold a suggestion to forget a learned word\n" +
                "• Tap ☺ for emoji, ?123 for symbols", 14, TEXT, false);
        t.setLineSpacing(px(4), 1f);
        t.setPadding(0, px(10), 0, 0);
        tips.addView(t);
        tips.addView(button("Clear learned words", false, new View.OnClickListener() {
            @Override public void onClick(View v) {
                new AlertDialog.Builder(SettingsActivity.this)
                        .setMessage("Forget all the words the keyboard has learned?")
                        .setPositiveButton("Clear", new DialogInterface.OnClickListener() {
                            @Override public void onClick(DialogInterface d, int w) {
                                new SuggestionEngine(SettingsActivity.this).clearUser();
                                Toast.makeText(SettingsActivity.this, "Learned words cleared", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        }));
        root.addView(tips);

        TextView foot = label("Xboard v2.0 · made by Hanif");
        foot.setTextColor(MUTED);
        foot.setGravity(Gravity.CENTER);
        foot.setPadding(0, px(10), 0, 0);
        root.addView(foot);

        setContentView(scroll);
        refreshWallpaper();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) refresh();
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req != PICK_IMAGE || res != RESULT_OK || data == null || data.getData() == null) return;
        final Uri uri = data.getData();
        Toast.makeText(this, "Setting wallpaper…", Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            @Override public void run() {
                final boolean ok = Wallpaper.save(getApplicationContext(), uri);
                runOnUiThread(new Runnable() {
                    @Override public void run() {
                        refreshWallpaper();
                        Toast.makeText(SettingsActivity.this, ok ? "Wallpaper set" : "Couldn't use that photo",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();
    }

    private void refresh() {
        boolean enabled = false;
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            for (InputMethodInfo i : imm.getEnabledInputMethodList()) {
                if (i.getPackageName().equals(getPackageName())) enabled = true;
            }
        }
        String cur = Settings.Secure.getString(getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
        boolean selected = cur != null && cur.startsWith(getPackageName() + "/");
        status1.setText(enabled ? "✓ Keyboard is enabled" : "Turn on “Xboard” in the list.");
        status1.setTextColor(enabled ? OK : TEXT);
        status2.setText(selected ? "✓ Xboard is your active keyboard" : "Choose Xboard as your input method.");
        status2.setTextColor(selected ? OK : TEXT);
        btnSelect.setEnabled(enabled);
        btnSelect.setAlpha(enabled ? 1f : 0.45f);
    }

    private void refreshWallpaper() {
        Bitmap bmp = Wallpaper.load(this, px(360));
        if (bmp != null) {
            wallPreview.setImageBitmap(bmp);
        } else {
            wallPreview.setImageDrawable(null);
        }
        wallPreview.setVisibility(bmp != null ? View.VISIBLE : View.GONE);
        btnRemove.setEnabled(bmp != null);
        btnRemove.setAlpha(bmp != null ? 1f : 0.45f);
    }

    private void setSizeLabel(int scale) {
        sizeLabel.setText("Keyboard size  " + scale + "%");
    }

    private void buildThemes(String selected) {
        themeRow.removeAllViews();
        for (int i = 0; i < Theme.NAMES.length; i++) {
            final String n = Theme.NAMES[i];
            Theme t = Theme.get(n);
            boolean on = n.equals(selected);
            SpannableString s = new SpannableString("●  " + Theme.LABELS[i]);
            s.setSpan(new ForegroundColorSpan(t.accent), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            TextView chip = text("", 14, t.text, true);
            chip.setText(s);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(px(14), 0, px(16), 0);
            GradientDrawable g = rounded(t.key, 12, on ? t.accent : t.divider);
            g.setStroke(px(on ? 2 : 1), on ? t.accent : t.divider);
            chip.setBackground(g);
            chip.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    Prefs.sp(SettingsActivity.this).edit().putString(Prefs.THEME, n).apply();
                    buildThemes(n);
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, px(42));
            lp.rightMargin = px(8);
            themeRow.addView(chip, lp);
        }
    }

    private int px(float v) { return (int) (v * dp + 0.5f); }

    private TextView text(String s, float size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private TextView label(String s) {
        TextView t = text(s.toUpperCase(), 12, ACCENT, true);
        t.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        t.setLetterSpacing(0.16f);
        return t;
    }

    private TextView small(String s) {
        TextView t = text(s, 13, MUTED, true);
        t.setPadding(0, px(10), 0, 0);
        return t;
    }

    private GradientDrawable rounded(int color, float r, int strokeColor) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(r * dp);
        if (strokeColor != 0) g.setStroke(Math.max(1, px(1)), strokeColor);
        return g;
    }

    private LinearLayout card(String heading) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackground(rounded(CARD, 16, LINE));
        c.setPadding(px(18), px(16), px(18), px(18));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = px(14);
        c.setLayoutParams(lp);
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        View bar = new View(this);
        bar.setBackground(rounded(ACCENT, 2, 0));
        head.addView(bar, new LinearLayout.LayoutParams(px(3), px(14)));
        TextView h = label(heading);
        h.setPadding(px(8), 0, 0, 0);
        head.addView(h);
        c.addView(head);
        return c;
    }

    private EditText field(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setHintTextColor(MUTED);
        e.setTextColor(TEXT);
        e.setTextSize(15);
        e.setBackground(rounded(FIELD, 10, LINE));
        e.setPadding(px(12), px(12), px(12), px(12));
        return e;
    }

    private LinearLayout.LayoutParams fieldLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = px(8);
        return lp;
    }

    private Button button(String label, boolean primary, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(15);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setStateListAnimator(null);
        b.setTextColor(primary ? 0xFFFFFFFF : TEXT);
        b.setBackground(primary ? rounded(ACCENT, 10, 0) : rounded(FIELD, 10, LINE));
        b.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, px(48));
        lp.topMargin = px(8);
        b.setLayoutParams(lp);
        return b;
    }

    private View toggle(String label, final String key, boolean def) {
        Switch s = new Switch(this);
        s.setText(label);
        s.setTextSize(15);
        s.setTextColor(TEXT);
        s.setPadding(0, px(12), 0, px(12));
        s.setChecked(Prefs.sp(this).getBoolean(key, def));
        s.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton b, boolean v) {
                Prefs.sp(SettingsActivity.this).edit().putBoolean(key, v).apply();
            }
        });
        return s;
    }

    private abstract static class Watcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
        @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
    }
}
