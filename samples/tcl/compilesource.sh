jextract --output src -l tcl -t org.tcl \
   /Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX.sdk/usr/include/tcl.h

javac --source=22 -d . src/org/tcl/*.java
