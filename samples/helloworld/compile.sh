cc -shared -o libhelloworld.dylib helloworld.c
jextract -t org.hello -lhelloworld helloworld.h
