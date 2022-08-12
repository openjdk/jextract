java --enable-native-access=ALL-UNNAMED \
    --enable-preview --source=20 \
    -Djava.library.path=${LIBGIT2_HOME}/build/ \
    GitClone.java $*
