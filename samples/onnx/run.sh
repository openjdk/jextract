java \
  --enable-native-access=ALL-UNNAMED \
  -Djava.library.path="$ONNX_GENAI_HOME/lib" \
  OnnxGenerator \
  "$MODEL_PATH"
