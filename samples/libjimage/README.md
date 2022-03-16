This example demonstrates Java interface for libjimage C++ library
that parses Java modules file.

** Note: libjimage is a private interface to Hotspot - not a public API. **

libjimage.dylib/libjimages.so/jimage.dll is loaded by boot loader.
So an application cannot load this using System.loadLibrary/.load. This
is because System.loadLibrary/.load is classloader based. Two or more
classloaders cannot share a native library. While this is needed for type
safety of JNI (Java Native Interface) based native libraries, Panama libraries
need not have this restriction. This sample demonstrates using platform native
dlopen/dlsym to load & use libjimage.dylib from a Java application.

extract.sh script was used to jextract jimage.h and dlfcn.h. These two extracted
libraries are used together in this project to access libjimage.dylib functions
from a Java app.
