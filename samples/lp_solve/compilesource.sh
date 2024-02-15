# on Mac, lp_solve can be installed using
# brew install lp_solve

LP_SOLVE_HOME=/usr/local/Cellar/lp_solve/5.5.2.11
 
jextract \
  --output src \
  -t net.sourceforge.lpsolve \
  -l :${LP_SOLVE_HOME}/lib/liblpsolve55.dylib \
  ${LP_SOLVE_HOME}/include/lp_lib.h

javac --source=22 -d . src/net/sourceforge/lpsolve/*.java
