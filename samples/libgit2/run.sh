java --enable-native-access=ALL-UNNAMED \
    --enable-preview --source=22 \
    -Djava.library.path=${LIBGIT2_HOME}/build/ \
    GitClone.java $*
