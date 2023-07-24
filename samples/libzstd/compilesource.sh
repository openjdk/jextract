jextract --source -t libzstd \
  -I /usr/local/Cellar/zstd/1.5.5/include \
  -l zstd \
  --header-class-name Libzstd \
  /usr/local/Cellar/zstd/1.5.5/include/zstd.h

javac --enable-preview --source=22 libzstd/*.java
