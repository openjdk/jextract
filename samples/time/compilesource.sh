jextract --output src -t org.unix \
   /Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX.sdk/usr/include/time.h

javac --source=22 -d . src/org/unix/*.java
