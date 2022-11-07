java --enable-native-access=ALL-UNNAMED \
   --enable-preview --source=20 \
   -Djava.library.path=/usr/local/Cellar/ffmpeg@4/4.4.3/lib LibffmpegMain.java $*
