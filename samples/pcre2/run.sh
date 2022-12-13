java --enable-native-access=ALL-UNNAMED \
    --enable-preview --source=21 \
    -Djava.library.path=/usr/local/opt/pcre2/lib PcreCheck.java $*
