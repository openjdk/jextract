java \
  --enable-native-access=ALL-UNNAMED \
  -Djava.library.path="$ONNX_LIB_PATH" \
  OnnxGenerator \
  "$MODEL_PATH"
