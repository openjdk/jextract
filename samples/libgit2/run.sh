java --enable-native-access=ALL-UNNAMED \
    --enable-preview --source=21 \
    -Djava.library.path=${LIBGIT2_HOME}/build/ \
    GitClone.java $*
