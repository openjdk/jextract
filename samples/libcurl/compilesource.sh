jextract --output src --source -t org.jextract -lcurl \
  /Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX.sdk/usr/include/curl/curl.h

javac --enable-preview --source=22 -d . src/org/jextract/*.java
