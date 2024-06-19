jextract --output src -l readline -t org.unix \
  --header-class-name readline_h \
  --include-function readline \
  --include-function free \
  "<stdlib.h>" \
  "<readline/readline.h>"

javac --source=22 -d . src/org/unix/*.java
