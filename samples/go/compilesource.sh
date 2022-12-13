go build -o libhello.dylib  -buildmode=c-shared
jextract --source -l hello -t org.golang libhello.h
javac --enable-preview --source=21 org/golang/*.java
