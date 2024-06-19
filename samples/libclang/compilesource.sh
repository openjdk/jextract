jextract --output src -t org.llvm.clang \
  -l :${LIBCLANG_HOME}/lib/libclang.dylib \
  -I ${LIBCLANG_HOME}/include/ \
  -I ${LIBCLANG_HOME}/include/clang-c \
  "<Index.h>"

javac --source=22 -d . src/org/llvm/clang/*.java
