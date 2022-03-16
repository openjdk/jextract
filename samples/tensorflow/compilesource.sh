jextract --source \
  -I /Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX.sdk/usr/include/ \
  -t org.tensorflow \
  -I ${LIBTENSORFLOW_HOME}/include \
  -l ${LIBTENSORFLOW_HOME}/lib/libtensorflow.dylib \
  ${LIBTENSORFLOW_HOME}/include/tensorflow/c/c_api.h

javac --add-modules jdk.incubator.foreign org/tensorflow/*.java
