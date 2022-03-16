jextract --source \
   -I /Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX.sdk/usr/include \
   -l lapacke -t lapack \
   /usr/local/opt/lapack/include/lapacke.h 

javac --add-modules jdk.incubator.foreign lapack/*.java
