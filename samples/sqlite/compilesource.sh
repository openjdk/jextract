jextract --source \
  /Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX.sdk/usr/include/sqlite3.h \
  -t org.sqlite -lsqlite3

javac --enable-preview --source=22 org/sqlite/*.java
