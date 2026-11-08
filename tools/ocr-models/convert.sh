#!/usr/bin/env bash
# Rebuilds app/src/main/assets/ocr/ppocrv5_* from PaddlePaddle's published ONNX files.
# Usage: tools/ocr-models/convert.sh <path-to-pnnx> <output-dir>
# Needs curl, sha256sum, python3 with PyYAML. See third_party/ncnn/README.md for provenance.
set -euo pipefail

pnnx=$(realpath "$1")
out=$2
here=$(cd "$(dirname "$0")" && pwd)
hf=https://huggingface.co/PaddlePaddle

declare -A sha=(
  [det/inference.onnx]=a431985659dc921974177a95adcfbb90fd9e51989a5e04d70d0b75f597b6e61d
  [latin/inference.onnx]=7888113072263cb471b93f66dd5e2ad70548dc526fa1ace760d0d973dd121498
  [latin/inference.yml]=0bbe984570f597af3638e50bdf2e8276f3ab26a61966096538b3b0d1849f5c84
)
declare -A repo=(
  [det]=PP-OCRv5_mobile_det_onnx
  [latin]=latin_PP-OCRv5_mobile_rec_onnx
)

work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
mkdir -p "$out" "$work/det" "$work/latin"

for file in "${!sha[@]}"; do
  curl -fsSL -o "$work/$file" "$hf/${repo[${file%%/*}]}/resolve/main/${file##*/}"
  echo "${sha[$file]}  $work/$file" | sha256sum -c --quiet -
done

(cd "$work/det" && "$pnnx" inference.onnx 'inputshape=[1,3,640,640]' 'inputshape2=[1,3,960,800]' device=cpu >/dev/null 2>&1)
(cd "$work/latin" && "$pnnx" inference.onnx 'inputshape=[1,3,48,320]' 'inputshape2=[1,3,48,640]' device=cpu >/dev/null 2>&1)

cp "$work/det/inference.ncnn.param" "$out/ppocrv5_mobile_det.param"
cp "$work/det/inference.ncnn.bin" "$out/ppocrv5_mobile_det.bin"
cp "$work/latin/inference.ncnn.param" "$out/ppocrv5_latin_rec.param"
cp "$work/latin/inference.ncnn.bin" "$out/ppocrv5_latin_rec.bin"
python3 -I "$here/dictionary.py" "$work/latin/inference.yml" "$out/ppocrv5_latin_dict.txt"

(cd "$out" && sha256sum ppocrv5_mobile_det.* ppocrv5_latin_rec.* ppocrv5_latin_dict.txt)
