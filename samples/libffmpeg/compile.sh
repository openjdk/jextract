jextract -t libffmpeg \
  -I /Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX.sdk/usr/include \
  -I /usr/local/Cellar/ffmpeg/4.4.1_3/include \
  -l avcodec \
  -l avformat \
  -l avutil \
  -l swscale \
  --header-class-name Libffmpeg \
  libffmpeg.h

