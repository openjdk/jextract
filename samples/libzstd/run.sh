java --enable-native-access=ALL-UNNAMED \
   --enable-preview --source=21 \
   -Djava.library.path="$(brew --prefix zstd)/lib" LibzstdMain.java
