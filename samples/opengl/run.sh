java -XstartOnFirstThread --enable-native-access=ALL-UNNAMED \
    --enable-preview --source=20 \
    -Djava.library.path=.:/System/Library/Frameworks/OpenGL.framework/Versions/Current/Libraries/ Teapot.java $*
