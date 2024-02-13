jextract --output src -D FORCE_OPENBLAS_COMPLEX_STRUCT \
  -l openblas -t blas /usr/local/opt/openblas/include/cblas.h

javac --source=22 -d . src/blas/*.java
