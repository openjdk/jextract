ANACONDA3_HOME=/opt/anaconda3

java --enable-native-access=ALL-UNNAMED \
    --enable-preview --source=20 \
    -Djava.library.path=${ANACONDA3_HOME}/lib \
    PythonMain.java
