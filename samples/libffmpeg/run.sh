java --enable-native-access=ALL-UNNAMED \
   --enable-preview --source=19 \
   -Djava.library.path=/usr/local/Cellar/ffmpeg/4.4.1_3/lib LibffmpegMain.java $*
