# on Mac, lp_solve can be installed using
# brew install lp_solve
 
jextract \
  --output src \
  --source \
  -t net.sourceforge.lpsolve \
  -l lpsolve55 \
  /usr/local/Cellar/lp_solve/5.5.2.11/include/lp_lib.h

javac --enable-preview --source=22 -d . src/net/sourceforge/lpsolve/*.java
