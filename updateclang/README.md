This directory contains files, script needed to update libclang binding in jextract code.

Steps to update libclang binding

* remove the src/main/java/org/openjdk/jextract/clang/libclang folder
* make sure jextract project is built
* cd updateclang (this directory)
* set PATH to point to directory where generated jextract tool lives
* set LIBCLANG_HOME to point to the home of the LLVM distribution
* run sh ./extract.sh
* apply the patches `Index_h.patch`, `Index_h$shared.patch` and `CXUnsavedFile.patch`
