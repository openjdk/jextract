jextract \
  -I /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/usr/include \
  -t org.openjdk \
  --source \
  jimage.h

jextract \
  -I /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/usr/include \
  -t org.unix \
  --source \
  /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/usr/include/dlfcn.h 
