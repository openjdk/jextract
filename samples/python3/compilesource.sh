ANACONDA3_HOME=/opt/anaconda3

jextract --source -l python3.8 \
  -I ${ANACONDA3_HOME}/include/python3.8 \
  -t org.python \
  ${ANACONDA3_HOME}/include/python3.8/Python.h

javac --enable-preview --source=22 org/python/*.java
