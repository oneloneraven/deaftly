import fsspec, pyarrow.parquet as pq, json, random, io, time
import soundfile as sf, numpy as np
from scipy.signal import resample_poly
from pathlib import Path
random.seed(285)
base="https://huggingface.co/datasets/sanchit-gandhi/vctk/resolve/main/"
out=Path("clips/clean"); out.mkdir(parents=True, exist_ok=True)
idx=[r for r in json.load(open("work/vctk_index.json")) if r["speaker"] and r["speaker"][0]==r["speaker"][1] and r["accent"][0]==r["accent"][1] and r["gender"][0]==r["gender"][1]]
fs=fsspec.filesystem("https")
manifest=[]
for accent in ["English","Scottish","Welsh","Irish","NorthernIrish","Indian"]:
    spk={}
    for r in idx:
        if r["accent"][0]==accent: spk.setdefault((r["gender"][0], r["speaker"][0]), []).append(r)
    chosen=[]
    for g in "FM":
        c=sorted(k for k in spk if k[0]==g); random.shuffle(c); chosen+=c[:2]
    for key in chosen:
        r=random.choice(spk[key])
        t=pq.ParquetFile(fs.open(base+r["file"], block_size=2**20)).read_row_group(r["rg"]).to_pylist()
        rows=[x for x in t if len(x["text"].split())>=5]; random.shuffle(rows)
        for x in rows[:6]:
            a,sr=sf.read(io.BytesIO(x["audio"]["bytes"]))
            if a.ndim>1: a=a.mean(axis=1)
            a=resample_poly(a, 16000, sr) if sr!=16000 else a
            cid=f"{accent}-{x['speaker_id']}-{x['text_id']}"
            sf.write(out/f"{cid}.wav", a.astype(np.float32), 16000, subtype="PCM_16")
            manifest.append({"id":cid,"accent":accent,"speaker":x["speaker_id"],"gender":x["gender"],"region":x["region"],"text":x["text"].strip()})
        time.sleep(0.5)
    print(accent, chosen, flush=True)
json.dump(manifest, open("clips/manifest.json","w"), indent=1)
print(len(manifest))
