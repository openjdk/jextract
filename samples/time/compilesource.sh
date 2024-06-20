jextract --output src -t org.unix "<time.h>"

javac --source=22 -d . src/org/unix/*.java
