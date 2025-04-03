jextract -t opengl --framework GLUT \
  --framework OpenGL \
  "<GLUT/glut.h>"

javac --source=22 -d . opengl/*.java
