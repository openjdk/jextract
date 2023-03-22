jextract -t org.pcre \
  -I /usr/local/opt/pcre2/include \
  --header-class-name Pcre \
  -DPCRE2_CODE_UNIT_WIDTH=8 \
  --library pcre2-8 \
  --source \
  /usr/local/opt/pcre2/include/pcre2.h

javac --enable-preview --source=20 org/pcre/*.java
