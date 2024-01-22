jextract --output src \
   -l lapacke -t lapack \
   /usr/local/opt/lapack/include/lapacke.h 

javac --source=22 -d . src/lapack/*.java
