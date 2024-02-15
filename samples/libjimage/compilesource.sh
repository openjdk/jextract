jextract \
  --output src \
  -l jimage \
  -t org.openjdk \
  jimage.h

javac --source=22 -d . src/org/openjdk/*.java
