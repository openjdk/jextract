java -Djava.library.path=${LIBCLANG_HOME}/lib --enable-native-access=ALL-UNNAMED \
    --enable-preview --source=21 \
    ASTPrinter.java $*
