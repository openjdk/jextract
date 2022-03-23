java -Djava.library.path=${LIBCLANG_HOME}/lib --enable-native-access=ALL-UNNAMED --add-modules jdk.incubator.foreign \
    ASTPrinter.java $*
