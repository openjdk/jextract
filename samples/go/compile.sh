go build -o libhello.dylib  -buildmode=c-shared
jextract -l hello -t org.golang libhello.h
