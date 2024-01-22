cc -shared -o libhelloworld.dylib helloworld.c
jextract --output src -t org.hello -lhelloworld helloworld.h
javac --source=22 -d . src/org/hello/*.java
