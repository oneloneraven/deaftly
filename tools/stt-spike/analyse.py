import json, glob, re, sys, statistics as st
import numpy as np, soundfile as sf, jiwer
from collections import defaultdict

CONTRACTIONS = {"there'll": "there will", "i'm": "i am", "it's": "it is", "don't": "do not", "we're": "we are", "they're": "they are",
                "can't": "cannot", "won't": "will not", "isn't": "is not", "that's": "that is", "we'll": "we will", "i'll": "i will",
                "you're": "you are", "didn't": "did not", "doesn't": "does not", "wasn't": "was not", "i've": "i have", "we've": "we have",
                "couldn't": "could not", "wouldn't": "would not", "aren't": "are not", "there's": "there is", "let's": "let us",
                "he's": "he is", "she's": "she is", "what's": "what is", "haven't": "have not", "shouldn't": "should not"}
def norm(t):
    t = re.sub(r"<error \d+>", "", t.replace("<cut-off>", "")).lower().replace("’", "'")
    t = re.sub(r"[^a-z0-9' ]+", " ", t)
    return " ".join(CONTRACTIONS.get(w, w) for w in t.split()).replace("can not", "cannot")

manifest = {c["id"]: c for c in json.load(open("clips/manifest.json"))}
onset = {}
for cid in manifest:
    a, _ = sf.read(f"clips/clean/{cid}.wav")
    fr = a[: len(a) // 480 * 480].reshape(-1, 480)
    e = 10 * np.log10((fr ** 2).mean(axis=1) + 1e-12)
    onset[cid] = int(np.argmax(e > e.max() - 30)) * 0.03

def wer(rows):
    return jiwer.wer([norm(manifest[r["id"]]["text"]) for r in rows], [norm(r["hyp"]) or "<empty>" for r in rows])

def load(pattern, device):
    out = {}
    for f in sorted(glob.glob(pattern)):
        d = json.load(open(f)); rows = d["rows"] if device else d
        out[rows[0]["engine"]] = (d.get("load_ms") if device else None, rows)
    return out

CONDS = ["clean", "pcafeter-10db", "straffic-10db", "pstation-10db", "pcafeter-5db", "straffic-5db", "pstation-5db"]
ACCENTS = ["English", "Scottish", "Welsh", "Irish", "NorthernIrish", "Indian"]
pct = lambda x: f"{100 * x:.1f}%"

def tables(results, title):
    print(f"\n### {title}: word error rate by condition\n")
    print("| Engine | " + " | ".join(CONDS) + " | All noisy |"); print("|---" * (len(CONDS) + 2) + "|")
    for e, (_, rows) in results.items():
        by = defaultdict(list)
        for r in rows: by[r["cond"]].append(r)
        noisy = [r for r in rows if r["cond"] != "clean"]
        print(f"| {e} | " + " | ".join(pct(wer(by[c])) if by[c] else "n/a" for c in CONDS) + f" | {pct(wer(noisy)) if noisy else 'n/a'} |")
    print(f"\n### {title}: word error rate by accent (clean / all noisy)\n")
    print("| Engine | " + " | ".join(ACCENTS) + " |"); print("|---" * (len(ACCENTS) + 1) + "|")
    for e, (_, rows) in results.items():
        cells = []
        for a in ACCENTS:
            c = [r for r in rows if manifest[r["id"]]["accent"] == a and r["cond"] == "clean"]
            n = [r for r in rows if manifest[r["id"]]["accent"] == a and r["cond"] != "clean"]
            cells.append(f"{pct(wer(c))} / {pct(wer(n)) if n else 'n/a'}")
        print(f"| {e} | " + " | ".join(cells) + " |")

host = load("results/host-*.json", False)
tables(host, "Host (Linux, x86_64)")
print("\n### Host: speed\n\n| Engine | Real-time factor (median) | First words shown, after speech starts (median / p95) |\n|---|---|---|")
for e, (_, rows) in host.items():
    rtf = st.median(r["proc_s"] / r["audio_s"] for r in rows)
    lat = sorted(r["first_partial_s"] - onset[r["id"]] for r in rows if r["first_partial_s"] is not None)
    print(f"| {e} | {rtf:.3f} | {st.median(lat):.2f} s / {lat[int(0.95 * len(lat))]:.2f} s |")

label, pattern = (sys.argv[1], sys.argv[2]) if len(sys.argv) > 2 else ("Emulator", "results/device-*.json")
dev = load(pattern, True)
if dev:
    tables(dev, label)
    print(f"\n### {label}: speed and memory\n\n| Engine | Load | Per 100 ms chunk (median / p95) | Real-time factor | First words shown after speech starts (median / p95) | Final text after audio ends (median) | App memory, PSS (max) |\n|---|---|---|---|---|---|---|")
    for e, (load_ms, rows) in dev.items():
        ch = sorted(x for r in rows for x in r["chunk_ms"])
        android = e.startswith("android")
        rtf = "n/a (fed in real time)" if android else f"{st.median(sum(r['chunk_ms']) / 1000 / r['audio_s'] for r in rows):.2f}"
        chunk = "n/a" if android else f"{st.median(ch):.0f} / {ch[int(0.95 * len(ch))]:.0f} ms"
        lat = sorted((r["first_partial_chunk"] + 1) * 0.1 - onset[r["id"]] for r in rows if r["first_partial_chunk"] is not None)
        fin = st.median(r["finalise_ms"] for r in rows if r["finalise_ms"] >= 0)
        print(f"| {e} | {load_ms / 1000:.1f} s | {chunk} | {rtf} | {st.median(lat):.2f} s / {lat[int(0.95 * len(lat))]:.2f} s | {fin:.0f} ms | {max(r['pss_kb'] for r in rows) / 1024:.0f} MB |")
    for e, (_, rows) in dev.items():
        cut = sum("<cut-off>" in r["hyp"] for r in rows); errs = sum(r["hyp"].startswith("<error") for r in rows)
        if cut or errs: print(f"\n{e}: stopped listening before the speaker finished on {cut} of {len(rows)} clips; errors on {errs}.")
