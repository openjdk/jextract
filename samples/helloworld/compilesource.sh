cc -shared -o libhelloworld.dylib helloworld.c
jextract --output src --source -t org.hello -lhelloworld helloworld.h
javac --enable-preview --source=22 -d . src/org/hello/*.java
