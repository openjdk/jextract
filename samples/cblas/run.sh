time java --enable-native-access=ALL-UNNAMED --add-modules jdk.incubator.foreign \
    -Djava.library.path=/usr/local/opt/openblas/lib \
    TestBlas.java
