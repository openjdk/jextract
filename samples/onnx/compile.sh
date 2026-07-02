jextract \
  --target-package oracle.code.onnx.foreign \
  --output src \
  "$ONNX_GENAI_HOME/include/ort_genai_c.h"

javac --release 26 -d . src/oracle/code/onnx/foreign/*.java OnnxGenerator.java
