#!/usr/bin/env bash
# Exports a standard (mobile-friendly) YOLOv8 license plate TFLite model into the app.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$ROOT/app/src/main/assets/plate_detector.tflite"
PT="/tmp/yolov8_plate_best.pt"

echo "=== Step 1: Python packages (install all at once) ==="
python3 -m pip install --upgrade pip
python3 -m pip install \
  ultralytics \
  "tensorflow>=2.16" \
  tf_keras \
  onnx \
  onnx2tf \
  onnx_graphsurgeon \
  sng4onnx \
  onnxslim

echo ""
echo "=== Step 2: Download plate model weights ==="
curl -L "https://huggingface.co/Koushim/yolov8-license-plate-detection/resolve/main/best.pt" -o "$PT"

echo ""
echo "=== Step 3: Export TFLite (1–3 min) ==="
python3 - <<'PY'
from ultralytics import YOLO
m = YOLO("/tmp/yolov8_plate_best.pt")
m.export(format="tflite", imgsz=640, int8=False)
print("Export complete.")
PY

# Ultralytics names the file after the weights stem, e.g. yolov8_plate_best_float32.tflite
SAVED_MODEL_DIR="/tmp/yolov8_plate_best_saved_model"
TFLITE=""
if [ -d "$SAVED_MODEL_DIR" ]; then
  TFLITE="$(find "$SAVED_MODEL_DIR" -maxdepth 1 -name '*_float32.tflite' 2>/dev/null | head -1)"
fi
if [ -z "$TFLITE" ]; then
  TFLITE="$(find /tmp /private/tmp "$ROOT" -name '*_float32.tflite' 2>/dev/null | head -1)"
fi
if [ -z "$TFLITE" ]; then
  echo ""
  echo "FAILED: *_float32.tflite not found under $SAVED_MODEL_DIR"
  echo "Export may have succeeded — look for a *_saved_model folder and copy its *_float32.tflite manually."
  exit 1
fi

cp "$TFLITE" "$OUT"
echo ""
echo "SUCCESS: $OUT ($(du -h "$OUT" | cut -f1))"
echo "Rebuild the app in Android Studio."
