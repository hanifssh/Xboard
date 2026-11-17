package com.qwerty.keyboard;

import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SuggestionEngine {

    private static final String[] EXTRA = {
        "I", "I'm", "I'll", "I've", "I'd", "don't", "can't", "won't", "it's", "didn't", "doesn't", "isn't",
        "that's", "you're", "we're", "they're", "let's", "wasn't", "couldn't", "wouldn't", "shouldn't",
        "there's", "what's", "he's", "she's", "aren't", "haven't", "you'll", "we'll", "okay", "ok", "hey",
        "yeah", "lol", "thanks", "thx", "pls", "please", "tomorrow", "tonight", "awesome", "haha", "omg"
    };

    private static class Entry {
        final String word, key;
        final int base;
        Entry(String w, int b) { word = w; key = norm(w); base = b; }
    }

    private final Context ctx;
    private final List<Entry> entries = new ArrayList<>();
    private final Map<String, Entry> byKey = new HashMap<>();
    private final Map<String, Integer> user = new HashMap<>();
    private boolean dirty;

    public SuggestionEngine(Context c) {
        ctx = c.getApplicationContext();
        load();
    }

    static String norm(String w) {
        StringBuilder b = new StringBuilder(w.length());
        for (int i = 0; i < w.length(); i++) {
            char ch = w.charAt(i);
            if (ch != '\'' && ch != '’') b.append(Character.toLowerCase(ch));
        }
        return b.toString();
    }

    private void add(String w, int base) {
        String k = norm(w);
        if (k.isEmpty()) return;
        Entry old = byKey.get(k);
        if (old != null) {
            if (base > old.base || !old.word.equals(w)) {
                entries.remove(old);
            } else return;
        }
        Entry e = new Entry(w, Math.max(base, old != null ? old.base : 0));
        entries.add(e);
        byKey.put(k, e);
    }

    private void load() {
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(ctx.getAssets().open("words.txt"), "UTF-8"));
            String line;
            int rank = 0;
            List<String> words = new ArrayList<>();
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) words.add(line);
            }
            r.close();
            int n = words.size();
            for (String w : words) {
                add(w, n - rank);
                rank++;
            }
            for (String w : EXTRA) {
                Entry old = byKey.get(norm(w));
                add(w, old != null ? old.base : n / 2);
            }
        } catch (Exception ignored) {}
        Collections.sort(entries, new Comparator<Entry>() {
            @Override public int compare(Entry a, Entry b) { return a.key.compareTo(b.key); }
        });
        try {
            File f = new File(ctx.getFilesDir(), "user_words.txt");
            if (f.exists()) {
                BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));
                String line;
                while ((line = r.readLine()) != null) {
                    int t = line.lastIndexOf('\t');
                    if (t > 0) {
                        try { user.put(line.substring(0, t), Integer.parseInt(line.substring(t + 1))); }
                        catch (NumberFormatException ignored) {}
                    }
                }
                r.close();
            }
        } catch (Exception ignored) {}
    }

    public void learn(String word) {
        if (word == null || word.length() < 2) return;
        String w = word;
        if (!byKey.containsKey(norm(w)) && Character.isUpperCase(w.charAt(0)) && w.length() > 1
                && w.substring(1).equals(w.substring(1).toLowerCase())) {
            w = word;
        } else if (byKey.containsKey(norm(w))) {
            w = byKey.get(norm(w)).word;
        }
        Integer c = user.get(w);
        user.put(w, c == null ? 1 : Math.min(c + 1, 999));
        dirty = true;
    }

    public void forget(String word) {
        if (user.remove(word) != null) dirty = true;
    }

    public void save() {
        if (!dirty) return;
        dirty = false;
        try {
            File f = new File(ctx.getFilesDir(), "user_words.txt");
            Writer w = new OutputStreamWriter(new FileOutputStream(f), "UTF-8");
            for (Map.Entry<String, Integer> e : user.entrySet()) w.write(e.getKey() + "\t" + e.getValue() + "\n");
            w.close();
        } catch (Exception ignored) {}
    }

    public void clearUser() {
        user.clear();
        dirty = true;
        save();
    }

    public List<String> suggest(String typed, int max) {
        final Map<String, Integer> scores = new HashMap<>();
        String key = norm(typed);
        if (key.isEmpty()) return new ArrayList<>();

        int lo = 0, hi = entries.size();
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (entries.get(mid).key.compareTo(key) < 0) lo = mid + 1; else hi = mid;
        }
        int scanned = 0;
        for (int i = lo; i < entries.size() && scanned < 4000; i++, scanned++) {
            Entry e = entries.get(i);
            if (!e.key.startsWith(key)) break;
            int s = e.base;
            if (e.key.equals(key)) s += 4000;
            s -= (e.key.length() - key.length()) * 120;
            Integer u = user.get(e.word);
            if (u != null) s += u * 1500;
            put(scores, e.word, s);
        }
        for (Map.Entry<String, Integer> u : user.entrySet()) {
            String w = u.getKey();
            if (byKey.containsKey(norm(w))) continue;
            if (u.getValue() >= 2 && norm(w).startsWith(key)) put(scores, w, 3000 + u.getValue() * 1500);
        }
        if (scores.size() < max && key.length() >= 2) {
            for (Entry e : entries) {
                if (Math.abs(e.key.length() - key.length()) > 1) continue;
                if (!e.key.isEmpty() && e.key.charAt(0) != key.charAt(0) && key.length() < 4) continue;
                if (editDistance1(key, e.key)) put(scores, e.word, e.base / 3);
            }
        }
        List<String> out = new ArrayList<>(scores.keySet());
        Collections.sort(out, new Comparator<String>() {
            @Override public int compare(String a, String b) { return scores.get(b) - scores.get(a); }
        });
        if (out.size() > max) out = new ArrayList<>(out.subList(0, max));
        for (int i = 0; i < out.size(); i++) out.set(i, matchCase(typed, out.get(i)));
        return out;
    }

    private static void put(Map<String, Integer> m, String w, int s) {
        Integer o = m.get(w);
        if (o == null || o < s) m.put(w, s);
    }

    private static String matchCase(String typed, String w) {
        if (typed.length() > 1 && typed.equals(typed.toUpperCase()) && !typed.equals(typed.toLowerCase()))
            return w.toUpperCase();
        if (Character.isUpperCase(typed.charAt(0)) && w.length() > 0)
            return Character.toUpperCase(w.charAt(0)) + w.substring(1);
        return w;
    }

    static boolean editDistance1(String a, String b) {
        if (a.equals(b)) return false;
        int la = a.length(), lb = b.length();
        if (la == lb) {
            int first = -1, diffs = 0;
            for (int i = 0; i < la; i++) {
                if (a.charAt(i) != b.charAt(i)) {
                    if (first < 0) first = i;
                    diffs++;
                }
            }
            if (diffs == 1) return true;
            return diffs == 2 && first + 1 < la && a.charAt(first) == b.charAt(first + 1)
                    && a.charAt(first + 1) == b.charAt(first);
        }
        if (Math.abs(la - lb) != 1) return false;
        String s = la < lb ? a : b, l = la < lb ? b : a;
        int i = 0, j = 0;
        boolean skipped = false;
        while (i < s.length() && j < l.length()) {
            if (s.charAt(i) == l.charAt(j)) { i++; j++; }
            else { if (skipped) return false; skipped = true; j++; }
        }
        return true;
    }
}
