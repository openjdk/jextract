if [[ -z "${ANACONDA3_HOME}" ]]; then
    ANACONDA3_HOME=/opt/anaconda3
fi

jextract -l python3.11 \
  -I ${ANACONDA3_HOME}/include/python3.11 \
  -t org.python \
  ${ANACONDA3_HOME}/include/python3.11/Python.h
