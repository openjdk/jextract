jextract --output src --source \
   -l lapacke -t lapack \
   /usr/local/opt/lapack/include/lapacke.h 

javac --enable-preview --source=22 -d . src/lapack/*.java
