import sys

import yaml

source, target = sys.argv[1:3]
with open(source, encoding="utf-8") as handle:
    characters = [str(c) for c in yaml.safe_load(handle)["PostProcess"]["character_dict"]]
characters.append(" ")
assert all("\n" not in c for c in characters)
with open(target, "w", encoding="utf-8") as handle:
    handle.write("\n".join(characters) + "\n")
