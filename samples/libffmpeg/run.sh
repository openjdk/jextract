java --enable-native-access=ALL-UNNAMED \
   --enable-preview --source=22 \
   -Djava.library.path=/usr/local/Cellar/ffmpeg@4/4.4.4_4/lib LibffmpegMain.java $*
