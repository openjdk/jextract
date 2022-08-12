# on Mac, lp_solve can be installed using
# brew install lp_solve
 
jextract \
  --source \
  -t net.sourceforge.lpsolve \
  -I /Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX.sdk/usr/include \
  -l lpsolve55 \
  /usr/local/Cellar/lp_solve/5.5.2.11/include/lp_lib.h

javac --enable-preview --source=20 net/sourceforge/lpsolve/*.java
