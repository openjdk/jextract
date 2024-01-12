jextract --output src --source -t org.unix \
  /Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX.sdk/usr/include/libproc.h

javac --enable-preview --source=22 -d . src/org/unix/*.java
