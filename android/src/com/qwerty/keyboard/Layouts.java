package com.qwerty.keyboard;

import java.util.ArrayList;
import java.util.List;

public final class Layouts {
    public static final int ALPHA = 0, SYM1 = 1, SYM2 = 2, EMOJI = 3, NUMBER = 4, PHONE = 5;
    public static final int FIELD_TEXT = 0, FIELD_EMAIL = 1, FIELD_URI = 2;

    static final String[][] EMOJI_PAGES = {
        {"😀","😃","😄","😁","😆","😅","😂","🤣","😊","😇","🙂","😉","😌","😍","🥰","😘",
         "😗","😋","😛","😜","🤪","😎","🤩","🥳","😏","😒","😞","😔","😟","😕","🙁","😢"},
        {"😭","😤","😠","😡","🤯","😳","🥺","😱","😨","😰","🤗","🤔","🤭","🤫","😶","😐",
         "😑","😬","🙄","😴","🤤","😷","🤒","🤕","🤢","🤮","🥵","🥶","😵","🤠","😈","💀"},
        {"👍","👎","👌","✌️","🤞","🤟","🤘","👏","🙌","👐","🙏","💪","👋","🤝","❤️","🧡",
         "💛","💚","💙","💜","🖤","💔","💯","🔥","✨","🎉","🎂","🌹","☀️","🌙","⭐","⚡"}
    };

    private Layouts() {}

    static Key ch(String s, String... alts) {
        Key k = new Key();
        k.label = s;
        k.text = s;
        if (alts.length > 0) {
            k.alts = alts;
            if (Character.isDigit(alts[0].charAt(0))) k.hint = alts[0];
        }
        return k;
    }

    static Key fn(String label, int code, float w) {
        Key k = new Key();
        k.label = label;
        k.code = code;
        k.w = w;
        k.func = true;
        if (code == Key.DELETE) k.repeat = true;
        return k;
    }

    static Key w(Key k, float w) { k.w = w; return k; }

    static List<Key> row(Key... keys) {
        List<Key> r = new ArrayList<>();
        for (Key k : keys) r.add(k);
        return r;
    }

    static List<Key> chars(String s) {
        List<Key> r = new ArrayList<>();
        for (String c : s.split(" ")) r.add(ch(c));
        return r;
    }

    static Key space(float w) {
        Key k = fn("Xboard", Key.SPACE, w);
        k.text = " ";
        k.longAction = true;
        return k;
    }

    static List<Key> bottom(String modeLabel, int modeCode, int field) {
        String mid = field == FIELD_EMAIL ? "@" : field == FIELD_URI ? "/" : ",";
        Key period = ch(".", "?", "!", "'", "\"", ":", ";");
        Key c = ch(mid);
        if (field == FIELD_TEXT) c.alts = new String[]{";", ":", "-"};
        return row(fn(modeLabel, modeCode, 1.5f), fn("☺", Key.EMOJI, 1f), c, space(4f), period,
                fn("↵", Key.ENTER, 1.5f));
    }

    public static List<List<Key>> build(int mode, int field, boolean numberRow, int emojiPage) {
        List<List<Key>> rows = new ArrayList<>();
        switch (mode) {
            case SYM1: {
                rows.add(chars("1 2 3 4 5 6 7 8 9 0"));
                rows.add(row(ch("@"), ch("#"), ch("$", "€", "£", "¥", "¢", "₹"), ch("_"), ch("&"),
                        ch("-", "–", "—", "·"), ch("+", "±"), ch("(", "<", "{", "["), ch(")", ">", "}", "]"), ch("/", "\\")));
                rows.add(row(fn("=\\<", Key.MODE_SYM2, 1.5f), ch("*", "†", "★"), ch("\"", "“", "”", "„", "«", "»"),
                        ch("'", "‘", "’", "‚", "‹", "›"), ch(":"), ch(";"), ch("!", "¡"), ch("?", "¿"),
                        fn("⌫", Key.DELETE, 1.5f)));
                rows.add(bottom("ABC", Key.MODE_ALPHA, field));
                break;
            }
            case SYM2: {
                rows.add(chars("~ ` | • √ π ÷ × ¶ ∆"));
                rows.add(chars("£ € ¥ ^ ° = { } [ ]"));
                List<Key> r3 = new ArrayList<>();
                r3.add(fn("?123", Key.MODE_SYM, 1.5f));
                r3.addAll(chars("% © ® ✓ \\ < >"));
                r3.add(fn("⌫", Key.DELETE, 1.5f));
                rows.add(r3);
                rows.add(bottom("ABC", Key.MODE_ALPHA, field));
                break;
            }
            case EMOJI: {
                String[] page = EMOJI_PAGES[Math.max(0, Math.min(emojiPage, EMOJI_PAGES.length - 1))];
                for (int r = 0; r < 4; r++) {
                    List<Key> row = new ArrayList<>();
                    for (int c = 0; c < 8; c++) {
                        Key k = w(ch(page[r * 8 + c]), 1.25f);
                        k.emoji = true;
                        row.add(k);
                    }
                    rows.add(row);
                }
                rows.add(row(fn("ABC", Key.MODE_ALPHA, 1.5f), fn("◀", Key.EMOJI_PREV, 1.25f), space(3.75f),
                        fn("▶", Key.EMOJI_NEXT, 1.25f), fn("⌫", Key.DELETE, 2.25f)));
                break;
            }
            case NUMBER:
            case PHONE: {
                boolean ph = mode == PHONE;
                rows.add(row(w(ch("1"), 2.5f), w(ch("2"), 2.5f), w(ch("3"), 2.5f), w(ch(ph ? "+" : "-"), 2.5f)));
                rows.add(row(w(ch("4"), 2.5f), w(ch("5"), 2.5f), w(ch("6"), 2.5f), space(2.5f)));
                rows.add(row(w(ch("7"), 2.5f), w(ch("8"), 2.5f), w(ch("9"), 2.5f), fn("⌫", Key.DELETE, 2.5f)));
                rows.add(row(fn("ABC", Key.MODE_ALPHA, 2f), w(ch(ph ? "*" : ","), 2f), w(ch("0"), 2f),
                        w(ch(ph ? "#" : "."), 2f), fn("↵", Key.ENTER, 2f)));
                for (List<Key> r : rows) for (Key k : r) if (k.code == Key.SPACE) k.label = "␣";
                break;
            }
            default: {
                if (numberRow) rows.add(chars("1 2 3 4 5 6 7 8 9 0"));
                rows.add(row(ch("q", "1"), ch("w", "2"), ch("e", "3", "è", "é", "ê", "ë", "ē"), ch("r", "4"),
                        ch("t", "5"), ch("y", "6", "ý", "ÿ"), ch("u", "7", "ù", "ú", "û", "ü"),
                        ch("i", "8", "ì", "í", "î", "ï"), ch("o", "9", "ò", "ó", "ô", "ö", "õ", "ø", "œ"), ch("p", "0")));
                rows.add(row(ch("a", "à", "á", "â", "ä", "ã", "å", "æ"), ch("s", "ß", "ś", "š"), ch("d", "ð"),
                        ch("f"), ch("g"), ch("h"), ch("j"), ch("k"), ch("l", "ł")));
                rows.add(row(fn("⇧", Key.SHIFT, 1.5f), ch("z", "ž", "ź", "ż"), ch("x"), ch("c", "ç", "ć", "č"),
                        ch("v"), ch("b"), ch("n", "ñ", "ń"), ch("m"), fn("⌫", Key.DELETE, 1.5f)));
                rows.add(bottom("?123", Key.MODE_SYM, field));
            }
        }
        return rows;
    }
}
