ANACONDA3_HOME=/opt/anaconda3

jextract -l python3.8 \
  -I /Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX.sdk/usr/include \
  -I ${ANACONDA3_HOME}/include/python3.8 \
  -t org.python \
  ${ANACONDA3_HOME}/include/python3.8/Python.h
