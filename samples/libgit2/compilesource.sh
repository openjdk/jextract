jextract --output src -t com.github \
  -l :${LIBGIT2_HOME}/build/libgit2.dylib \
  -I /Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX.sdk/usr/include/ \
  -I ${LIBGIT2_HOME}/include/ \
  -I ${LIBGIT2_HOME}/include/git2 \
  "<git2.h>"

javac --source=22 -d . src/com/github/*.java
