LIBZSTD_HOME=/usr/local/Cellar/zstd/1.5.5

jextract --output src -t libzstd \
  -I ${LIBZSTD_HOME}/include \
  -l :${LIBZSTD_HOME}/lib/libzstd.dylib \
  --header-class-name Libzstd \
  ${LIBZSTD_HOME}/include/zstd.h

javac --source=22 -d . src/libzstd/*.java
