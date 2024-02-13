jextract --output src -t org.llvm.clang -lclang \
  -I ${LIBCLANG_HOME}/include/ \
  -I ${LIBCLANG_HOME}/include/clang-c \
  ${LIBCLANG_HOME}/include/clang-c/Index.h
javac --source=22 -d . src/org/llvm/clang/*.java
