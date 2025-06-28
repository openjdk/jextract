jextract \
  --target-package oracle.code.onnx.foreign \
  --output src \
  "$ORT_GENAI_DIR/src/ort_genai_c.h"

javac --release 24 -d . src/oracle/code/onnx/foreign/*.java OnnxGenerator.java
