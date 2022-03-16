jextract -l readline -t org.unix \
  -I /Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX.sdk/usr/include \
  --header-class-name readline_h \
  --include-function readline \
  --include-function free \
  myreadline.h
