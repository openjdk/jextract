jextract --output src --source -t org.llvm.clang -lclang \
  -I ${LIBCLANG_HOME}/include/ \
  -I ${LIBCLANG_HOME}/include/clang-c \
  ${LIBCLANG_HOME}/include/clang-c/Index.h
javac --enable-preview --source=22 -d . src/org/llvm/clang/*.java
