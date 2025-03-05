jextract -t opengl -F GLUT \
  -F OpenGL \
  "<GLUT/glut.h>"

javac --source=22 -d . opengl/*.java
