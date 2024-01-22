jextract --output src -t libzstd \
  -I /usr/local/Cellar/zstd/1.5.5/include \
  -l zstd \
  --header-class-name Libzstd \
  /usr/local/Cellar/zstd/1.5.5/include/zstd.h

javac --source=22 -d . src/libzstd/*.java
