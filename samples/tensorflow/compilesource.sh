jextract --output src --source \
  -t org.tensorflow \
  -I ${LIBTENSORFLOW_HOME}/include \
  -l ${LIBTENSORFLOW_HOME}/lib/libtensorflow.dylib \
  ${LIBTENSORFLOW_HOME}/include/tensorflow/c/c_api.h

javac --enable-preview --source=22 -d . src/org/tensorflow/*.java
