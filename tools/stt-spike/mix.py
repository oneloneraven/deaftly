import json, random, numpy as np, soundfile as sf
from pathlib import Path
random.seed(285)
m=json.load(open("clips/manifest.json"))
noise={n: sf.read(f"noise/{n}.wav")[0] for n in ["PCAFETER","STRAFFIC","PSTATION"]}
for n,a in noise.items(): print(n, len(a)/16000, "s")
for snr in (10, 5):
    for n,a in noise.items():
        d=Path(f"clips/{n.lower()}-{snr}db"); d.mkdir(exist_ok=True)
        for c in m:
            s,_=sf.read(f"clips/clean/{c['id']}.wav")
            o=random.randrange(0, len(a)-len(s)); z=a[o:o+len(s)]
            ps=np.mean(s**2); pn=np.mean(z**2)
            y=s+z*np.sqrt(ps/(pn*10**(snr/10)))
            y=y/max(1.0, np.abs(y).max()/0.99)
            sf.write(d/f"{c['id']}.wav", y.astype(np.float32), 16000, subtype="PCM_16")
print(sorted(p.name for p in Path("clips").iterdir()))
