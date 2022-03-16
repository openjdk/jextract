go build -o libhello.dylib  -buildmode=c-shared
jextract --source -l hello -t org.golang libhello.h
javac --add-modules jdk.incubator.foreign org/golang/*.java

