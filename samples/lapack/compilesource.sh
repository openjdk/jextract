LAPACK_HOME=/usr/local/opt/lapack

jextract --output src \
   -l :${LAPACK_HOME}/lib/liblapacke.dylib \
   -t lapack \
   ${LAPACK_HOME}/include/lapacke.h 

javac --source=22 -d . src/lapack/*.java
