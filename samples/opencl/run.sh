/Users/grfrost/github/babylon/build/macosx-aarch64-server-release/jdk/bin/java \
    -XstartOnFirstThread \
    --enable-native-access=ALL-UNNAMED \
    --enable-preview \
    NBody.java GLWrap.java CLWrap.java $*
