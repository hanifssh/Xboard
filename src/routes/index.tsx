import { createFileRoute } from "@tanstack/react-router";
import appIcon from "@/assets/app-icon.png";

export const Route = createFileRoute("/")({
  head: () => ({
    meta: [
      { title: "Xboard — Android keyboard by Hanif" },
      {
        name: "description",
        content:
          "Xboard is a QWERTY keyboard for Android with auto send, themes, photo wallpaper, live resizing and word suggestions. Download the APK.",
      },
      { property: "og:title", content: "Xboard — Android keyboard by Hanif" },
      {
        property: "og:description",
        content: "Auto send, themes, photo wallpaper, live resizing and word suggestions. Download the APK.",
      },
      { property: "og:type", content: "website" },
      { name: "twitter:card", content: "summary_large_image" },
    ],
  }),
  component: Index,
});

const APK_URL = "/download/QwertyKeyboard.apk";

const ROWS = [
  ["q", "w", "e", "r", "t", "y", "u", "i", "o", "p"],
  ["a", "s", "d", "f", "g", "h", "j", "k", "l"],
];

const FEATURES = [
  { title: "Auto send", text: "Set a message and an amount, press Start, and Xboard types and sends it on repeat." },
  { title: "Seven themes", text: "Xboard, Neon, Ocean, Sunset, Rose, Graphite and Light." },
  { title: "Photo wallpaper", text: "Put any photo from your gallery behind the keys." },
  { title: "Resize live", text: "Tap ⇕ on the Xboard bar to make the keyboard bigger or smaller while typing." },
  { title: "Word suggestions", text: "10,000-word dictionary that learns the words you use." },
  { title: "Emoji, symbols, accents", text: "Emoji pages, two symbol layouts and hold-for-more keys." },
];

const STEPS = [
  "Download the APK on your Android phone.",
  "Open it and allow installing from this source if asked.",
  "Open Xboard and tap “Enable in system settings”.",
  "Tap “Switch to Xboard” — you're ready to type.",
];

function KeyboardMock() {
  return (
    <div className="w-full max-w-md rounded-2xl border border-border bg-board p-3 shadow-glow">
      <div className="mb-2 flex items-center gap-2 border-b border-border pb-2">
        <span className="font-display text-lg font-bold italic">
          <span className="text-primary">X</span>board
        </span>
        <span className="font-mono text-xs font-bold text-primary">12/50</span>
        <span className="ml-auto flex gap-1.5 text-xs font-bold">
          <span className="rounded-full bg-keycap-func px-2.5 py-1.5">⇕</span>
          <span className="rounded-full bg-keycap-func px-2.5 py-1.5">🌐</span>
          <span className="rounded-full bg-primary px-3 py-1.5 text-primary-foreground opacity-50">▶ Start</span>
          <span className="rounded-full bg-destructive px-3 py-1.5 text-destructive-foreground">■ Stop</span>
        </span>
      </div>
      <div className="mb-2 grid grid-cols-3 text-center text-sm text-muted-foreground">
        <span>“helo”</span>
        <span className="font-bold text-foreground">hello</span>
        <span>help</span>
      </div>
      <div className="flex flex-col gap-2">
        {ROWS.map((row, i) => (
          <div key={i} className={`flex gap-1.5 ${i === 1 ? "px-[5%]" : ""}`}>
            {row.map((k, j) => (
              <div
                key={k}
                className={`keycap flex-1 ${(i * 10 + j) % 7 === 3 ? "keycap-press" : ""}`}
                style={{ animationDelay: `${(i * 10 + j) * 0.21}s` }}
              >
                {k}
              </div>
            ))}
          </div>
        ))}
        <div className="flex gap-1.5">
          <div className="keycap keycap-func flex-[1.5]">⇧</div>
          {["z", "x", "c", "v", "b", "n", "m"].map((k) => (
            <div key={k} className="keycap flex-1">
              {k}
            </div>
          ))}
          <div className="keycap keycap-func flex-[1.5]">⌫</div>
        </div>
        <div className="flex gap-1.5">
          <div className="keycap keycap-func flex-[1.5]">?123</div>
          <div className="keycap keycap-func flex-1">☺</div>
          <div className="keycap flex-1">,</div>
          <div className="keycap flex-[4] text-xs text-muted-foreground">Xboard</div>
          <div className="keycap flex-1">.</div>
          <div className="keycap keycap-accent flex-[1.5]">↵</div>
        </div>
      </div>
    </div>
  );
}

function Index() {
  return (
    <main className="min-h-screen">
      <section className="mx-auto grid max-w-6xl items-center gap-12 px-6 pb-20 pt-16 md:grid-cols-2 md:pt-24">
        <div>
          <div className="flex items-center gap-3">
            <img src={appIcon} alt="Xboard icon" width={48} height={48} className="rounded-xl" />
            <div>
              <span className="block font-display text-lg font-bold">Xboard</span>
              <span className="block font-mono text-xs uppercase tracking-[0.16em] text-primary">made by Hanif</span>
            </div>
          </div>
          <h1 className="mt-6 font-display text-5xl font-bold leading-[1.05] tracking-tight md:text-6xl">
            Type it once. <span className="text-primary">Send it on repeat.</span>
          </h1>
          <p className="mt-5 max-w-md text-lg text-muted-foreground">
            A fast QWERTY keyboard for Android with auto send, themes, your own photo wallpaper and live resizing.
          </p>
          <div className="mt-8 flex flex-wrap items-center gap-4">
            <a
              href={APK_URL}
              download="Xboard.apk"
              className="inline-flex items-center gap-2 rounded-xl bg-primary px-6 py-3.5 font-display text-base font-bold text-primary-foreground transition-transform hover:-translate-y-0.5"
            >
              ↓ Download APK
            </a>
            <span className="font-mono text-sm text-muted-foreground">v2.0 · 177 KB · Android 6.0+</span>
          </div>
        </div>
        <div className="flex justify-center md:justify-end">
          <KeyboardMock />
        </div>
      </section>

      <section className="mx-auto max-w-6xl px-6 pb-20">
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {FEATURES.map((f) => (
            <div key={f.title} className="rounded-2xl border border-border bg-card p-6">
              <h2 className="font-display text-lg font-bold">{f.title}</h2>
              <p className="mt-2 text-muted-foreground">{f.text}</p>
            </div>
          ))}
        </div>
      </section>

      <section className="mx-auto max-w-6xl px-6 pb-24">
        <h2 className="font-display text-3xl font-bold tracking-tight">Install in a minute</h2>
        <ol className="mt-8 grid gap-4 md:grid-cols-4">
          {STEPS.map((s, i) => (
            <li key={s} className="rounded-2xl border border-border bg-muted p-6">
              <span className="font-mono text-3xl font-bold text-primary">{i + 1}</span>
              <p className="mt-3">{s}</p>
            </li>
          ))}
        </ol>
      </section>

      <footer className="border-t border-border py-8 text-center font-mono text-xs uppercase tracking-[0.16em] text-muted-foreground">
        Xboard · made by Hanif
      </footer>
    </main>
  );
}
