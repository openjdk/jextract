jextract -t com.github -lgit2 \
  -I /Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX.sdk/usr/include/ \
  -I ${LIBGIT2_HOME}/include/ \
  -I ${LIBGIT2_HOME}/include/git2 \
  ${LIBGIT2_HOME}/include/git2.h
