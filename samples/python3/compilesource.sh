if [[ -z "${ANACONDA3_HOME}" ]]; then
    ANACONDA3_HOME=/opt/anaconda3
fi

jextract --output src \
  -l :${ANACONDA3_HOME}/lib/libpython3.11.dylib \
  -I ${ANACONDA3_HOME}/include/python3.11 \
  -t org.python \
  ${ANACONDA3_HOME}/include/python3.11/Python.h

javac --source=22 -d . src/org/python/*.java
