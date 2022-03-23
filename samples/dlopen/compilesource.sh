cc --shared -o libhello.dylib hello.c

jextract --source -t org.unix \
  -I /Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX.sdk/usr/include \
  /Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX.sdk/usr/include/dlfcn.h

javac --add-modules jdk.incubator.foreign org/unix/*.java
