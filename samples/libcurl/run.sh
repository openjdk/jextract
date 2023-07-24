java --enable-native-access=ALL-UNNAMED \
    --enable-preview --source=22 \
    -Djava.library.path=/usr/lib CurlMain.java $*
