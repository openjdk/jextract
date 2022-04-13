jextract -C "-D FORCE_OPENBLAS_COMPLEX_STRUCT" \
  -I /Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX.sdk/usr/include \
  -l openblas -t blas /usr/local/opt/openblas/include/cblas.h

