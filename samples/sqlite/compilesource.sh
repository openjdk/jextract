jextract --output src \
  -t org.sqlite -lsqlite3 \
  "<sqlite3.h>"

javac --source=22 -d . src/org/sqlite/*.java
