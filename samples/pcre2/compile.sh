jextract -t org.pcre \
  -I /usr/local/opt/pcre2/include \
  -DPCRE2_CODE_UNIT_WIDTH=8 \
  --header-class-name Pcre \
  --library pcre2-8 \
  /usr/local/opt/pcre2/include/pcre2.h
