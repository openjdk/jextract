jextract --source -t org.unix \
   /Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX.sdk/usr/include/time.h

javac --enable-preview --source=22 org/unix/*.java
