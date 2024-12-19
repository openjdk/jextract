jextract -t opengl -f GLUT \
  -f OpenGL \
  "<GLUT/glut.h>"

javac --source=22 -d . opengl/*.java
