java --enable-native-access=ALL-UNNAMED \
    --enable-preview --source=20 \
    -Djava.library.path=/usr/local/opt/pcre2/lib PcreCheck.java $*
