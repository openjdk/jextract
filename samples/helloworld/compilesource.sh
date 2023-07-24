cc -shared -o libhelloworld.dylib helloworld.c
jextract --source -t org.hello -lhelloworld helloworld.h
javac --enable-preview --source=22 org/hello/*.java
