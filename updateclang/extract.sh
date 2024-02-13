#!/bin/bash

echo "Extracting libclang headers..."

jextract --output ../src/main/java \
  -t org.openjdk.jextract.clang.libclang -lclang \
  --use-system-load-library \
  -I /Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX.sdk/usr/include/ \
  -I ${LIBCLANG_HOME}/include/ \
  -I ${LIBCLANG_HOME}/include/clang-c \
  @clang.symbols \
  ${LIBCLANG_HOME}/include/clang-c/Index.h

echo "Adding copyrights..."

for x in ../src/main/java/org/openjdk/jextract/clang/libclang/*.java; do
(cat cp_header.txt; echo; cat $x) > /tmp/file;
mv /tmp/file $x
done

echo "Done!"
