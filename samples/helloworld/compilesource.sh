cc -shared -o libhelloworld.dylib helloworld.c
jextract --source -t org.hello -lhelloworld helloworld.h
javac --add-modules jdk.incubator.foreign org/hello/*.java
