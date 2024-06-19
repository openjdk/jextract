FFMPEG_HOME=/usr/local/Cellar/ffmpeg@4/4.4.4_4

jextract --output src -t libffmpeg \
  -I ${FFMPEG_HOME}/include \
  -l :${FFMPEG_HOME}/lib/libavcodec.dylib \
  -l :${FFMPEG_HOME}/lib/libavformat.dylib \
  -l :${FFMPEG_HOME}/lib/libavutil.dylib \
  -l :${FFMPEG_HOME}/lib/libswscale.dylib \
  --header-class-name Libffmpeg \
  "<libavcodec/avcodec.h>" \
  "<libavformat/avformat.h>" \
  "<libswscale/swscale.h>"

javac --source=22 -d . src/libffmpeg/*.java
