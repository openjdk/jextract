jextract -t org.pcre \
  -I /usr/local/opt/pcre2/include \
  --header-class-name Pcre \
  --library pcre2-8 \
  --source \
  Pcre.h

javac --enable-preview --source=20 org/pcre/*.java
