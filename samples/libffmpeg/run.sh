java --enable-native-access=ALL-UNNAMED \
   --add-modules jdk.incubator.foreign \
   -Djava.library.path=/usr/local/Cellar/ffmpeg/4.4.1_3/lib LibffmpegMain.java $*
