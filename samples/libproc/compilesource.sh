jextract --output src -t org.unix "<libproc.h>"

javac --source=22 -d . src/org/unix/*.java
