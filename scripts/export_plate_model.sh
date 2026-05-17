#!/usr/bin/env bash
# Exports a standard (mobile-friendly) YOLOv8 license plate TFLite model into the app.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$ROOT/app/src/main/assets/plate_detector.tflite"
PT="/tmp/yolov8_plate_best.pt"

echo "Installing export dependencies..."
python3 -m pip install --upgrade pip
python3 -m pip install ultralytics "tensorflow>=2.16" tf_keras onnx onnx2tf onnx_graphsurgeon

echo "Downloading YOLOv8 plate weights..."
curl -L "https://huggingface.co/Koushim/yolov8-license-plate-detection/resolve/main/best.pt" -o "$PT"

echo "Exporting TFLite (float32, 640) — may take 1–2 minutes..."
python3 - <<'PY'
from ultralytics import YOLO
m = YOLO("/tmp/yolov8_plate_best.pt")
m.export(format="tflite", imgsz=640, int8=False)
PY

TFLITE="$(find /tmp /Users/aramsadoyan/AndroidStudioProjects/AIBlurVideo -name 'best_float32.tflite' 2>/dev/null | head -1)"
if [ -z "$TFLITE" ]; then
  echo "Export failed: best_float32.tflite not found"
  exit 1
fi

cp "$TFLITE" "$OUT"
echo "Done: $OUT ($(du -h "$OUT" | cut -f1))"
echo "Rebuild the app in Android Studio."
