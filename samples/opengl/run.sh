java -XstartOnFirstThread --enable-native-access=ALL-UNNAMED \
    --enable-preview --source=19 \
    -Djava.library.path=.:/System/Library/Frameworks/OpenGL.framework/Versions/Current/Libraries/ Teapot.java $*
