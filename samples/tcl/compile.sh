jextract -l tcl -t org.tcl \
  --include-function Tcl_CreateInterp \
  --include-function Tcl_Eval \
  --include-function Tcl_DeleteInterp \
  -I /Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX.sdk/usr/include \
   /Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX.sdk/usr/include/tcl.h
