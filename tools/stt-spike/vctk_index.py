import fsspec, pyarrow.parquet as pq, json, time
base="https://huggingface.co/datasets/sanchit-gandhi/vctk/resolve/main/"
names=sorted(f["path"] for f in json.load(fsspec.open("https://huggingface.co/api/datasets/sanchit-gandhi/vctk/tree/main/data").open()) if f["path"].endswith(".parquet"))
fs=fsspec.filesystem("https")
index=[]
for n in names:
    m=pq.ParquetFile(fs.open(base+n, block_size=2**16)).metadata
    cols={m.schema.names[i]:i for i in range(len(m.schema.names))}
    for g in range(m.num_row_groups):
        rg=m.row_group(g)
        def mm(name):
            for i in range(rg.num_columns):
                c=rg.column(i)
                if c.path_in_schema==name and c.statistics and c.statistics.has_min_max: return c.statistics.min, c.statistics.max
        index.append({"file":n,"rg":g,"rows":rg.num_rows,"speaker":mm("speaker_id"),"accent":mm("accent"),"gender":mm("gender")})
    time.sleep(0.5)
json.dump(index, open("work/vctk_index.json","w"), indent=0)
from collections import Counter
print(Counter(r["accent"][0] for r in index if r["accent"] and r["accent"][0]==r["accent"][1]))
