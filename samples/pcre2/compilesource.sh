jextract --output src -t org.pcre \
  -I /usr/local/opt/pcre2/include \
  --header-class-name Pcre \
  -DPCRE2_CODE_UNIT_WIDTH=8 \
  --library pcre2-8 \
  "<pcre2.h>"

javac --source=22 -d . src/org/pcre/*.java
