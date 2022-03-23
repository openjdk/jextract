ANACONDA3_HOME=/opt/anaconda3

java --enable-native-access=ALL-UNNAMED --add-modules jdk.incubator.foreign \
    -Djava.library.path=${ANACONDA3_HOME}/lib \
    PythonMain.java
