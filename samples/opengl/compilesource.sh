jextract -t opengl -l :/System/Library/Frameworks/GLUT.framework/GLUT \
  -l :/System/Library/Frameworks/OpenGL.framework/OpenGL \
  "<GLUT/glut.h>"

javac --source=22 -d . opengl/*.java
