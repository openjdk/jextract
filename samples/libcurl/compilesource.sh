jextract --output src -t org.jextract -lcurl "<curl/curl.h>"

javac --source=22 -d . src/org/jextract/*.java
