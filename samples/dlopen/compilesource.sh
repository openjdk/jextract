cc --shared -o libhello.dylib hello.c

jextract --output src --source -t org.unix \
  /Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX.sdk/usr/include/dlfcn.h

javac --enable-preview --source=22 -d . src/org/unix/*.java
