ANACONDA3_HOME=/opt/anaconda3

java --enable-native-access=ALL-UNNAMED \
    --enable-preview --source=21 \
    -Djava.library.path=${ANACONDA3_HOME}/lib \
    PythonMain.java
