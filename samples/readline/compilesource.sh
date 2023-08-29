jextract --source -l readline -t org.unix \
  --header-class-name readline_h \
  --include-function readline \
  --include-function free \
  myreadline.h

javac --enable-preview --source=22 org/unix/*.java
