This directory contains files, script needed to update libclang binding in jextract code.

Steps to update libclang binding

* make sure jextract project is built
* cd updateclang (this directory)
* set PATH to point to directory where generated jextract tool lives
* run sh ./extract.sh

Manually update the following file:

File: RuntimeHelper.java


    // Manual change to handle platform specific library name difference
    static {
        // Manual change to handle platform specific library name difference
        String libName = System.getProperty("os.name").startsWith("Windows")? "libclang" : "clang";
        System.loadLibrary(libName);
    }
