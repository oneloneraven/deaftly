import json, sys, time, re
import numpy as np, soundfile as sf, jiwer
from pathlib import Path

CHUNK = 1600  # 100 ms at 16 kHz

def norm(t):
    t = t.lower().replace("’", "'")
    t = re.sub(r"[^a-z0-9' ]+", " ", t)
    return " ".join(t.split())

def sherpa(model):
    import sherpa_ncnn
    d = Path("models") / model
    r = sherpa_ncnn.Recognizer(
        tokens=str(d / "tokens.txt"),
        encoder_param=str(d / "encoder_jit_trace-pnnx.ncnn.param"), encoder_bin=str(d / "encoder_jit_trace-pnnx.ncnn.bin"),
        decoder_param=str(d / "decoder_jit_trace-pnnx.ncnn.param"), decoder_bin=str(d / "decoder_jit_trace-pnnx.ncnn.bin"),
        joiner_param=str(d / "joiner_jit_trace-pnnx.ncnn.param"), joiner_bin=str(d / "joiner_jit_trace-pnnx.ncnn.bin"),
        num_threads=2)
    def run(a):
        s = r.create_stream() if hasattr(r, "create_stream") else None
        first = None
        for i in range(0, len(a), CHUNK):
            r.accept_waveform(16000, a[i:i + CHUNK])
            if first is None and r.text.strip(): first = (i + CHUNK) / 16000
        r.accept_waveform(16000, np.zeros(int(0.5 * 16000), dtype=np.float32))
        r.input_finished()
        t = r.text
        r.reset() if hasattr(r, "reset") else None
        return t, first
    return run

def vosk(model):
    import vosk
    vosk.SetLogLevel(-1)
    m = vosk.Model(str(Path("models") / model))
    def run(a):
        rec = vosk.KaldiRecognizer(m, 16000)
        pcm = (np.clip(a, -1, 1) * 32767).astype("<i2").tobytes()
        first, parts = None, []
        for i in range(0, len(pcm), CHUNK * 2):
            if rec.AcceptWaveform(pcm[i:i + CHUNK * 2]):
                parts.append(json.loads(rec.Result())["text"])
            elif first is None and json.loads(rec.PartialResult())["partial"]:
                first = (i // 2 + CHUNK) / 16000
        parts.append(json.loads(rec.FinalResult())["text"])
        return " ".join(p for p in parts if p), first
    return run

ENGINES = {
    "sherpa-zipformer-en": lambda: sherpa("sherpa-ncnn-streaming-zipformer-en-2023-02-13"),
    "sherpa-zipformer-20M": lambda: sherpa("sherpa-ncnn-streaming-zipformer-20M-2023-02-17"),
    "vosk-small-en-us": lambda: vosk("vosk-model-small-en-us-0.15"),
    "vosk-en-us-lgraph": lambda: vosk("vosk-model-en-us-0.22-lgraph"),
}

name = sys.argv[1]
run = ENGINES[name]()
manifest = json.load(open("clips/manifest.json"))
conds = sorted(p.name for p in Path("clips").iterdir() if p.is_dir())
rows = []
for cond in conds:
    for c in manifest:
        a, _ = sf.read(f"clips/{cond}/{c['id']}.wav", dtype="float32")
        t0 = time.perf_counter(); hyp, first = run(a); dt = time.perf_counter() - t0
        rows.append({"engine": name, "cond": cond, "id": c["id"], "accent": c["accent"], "ref": norm(c["text"]),
                     "hyp": norm(hyp), "first_partial_s": first, "audio_s": len(a) / 16000, "proc_s": dt})
    sub = [r for r in rows if r["cond"] == cond]
    print(name, cond, f"WER {jiwer.wer([r['ref'] for r in sub], [r['hyp'] for r in sub]):.3f}", flush=True)
Path("results").mkdir(exist_ok=True)
json.dump(rows, open(f"results/host-{name}.json", "w"), indent=0)
