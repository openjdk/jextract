jextract -t opengl -l :/System/Library/Frameworks/GLUT.framework/Versions/Current/GLUT \
  -l :/System/Library/Frameworks/OpenGL.framework/Versions/Current/OpenGL \
  /Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX.sdk/System/Library/Frameworks/GLUT.framework/Headers/glut.h

javac opengl/*.java
