## Notes on Windows

This sample requires freeglut as a dependency. On Windows the freeglut package that comes with MinGW is know not to work,
because clang (which jextract uses under the hood) doesn't seem to understand MinGW builtins found in the MinGW standard library header files.

The sample has been tested against the freeglut MSVC package found here: https://www.transmissionzero.co.uk/software/freeglut-devel/

On top of that, the code in Teapot.java has to be changed to account for different parameter names in the Windows freeglut headers:

```
diff --git a/opengl/Teapot.java b/opengl/Teapot.java
index 22d1f44..d5eb786 100644
--- a/opengl/Teapot.java
+++ b/opengl/Teapot.java
@@ -79,8 +79,8 @@ public class Teapot {
             glutInitWindowSize(500, 500);
             glutCreateWindow(allocator.allocateUtf8String("Hello Panama!"));
             var teapot = new Teapot(allocator);
-            var displayStub = glutDisplayFunc$func.allocate(teapot::display, scope);
-            var idleStub = glutIdleFunc$func.allocate(teapot::onIdle, scope);
+            var displayStub = glutDisplayFunc$callback.allocate(teapot::display, scope);
+            var idleStub = glutIdleFunc$callback.allocate(teapot::onIdle, scope);
             glutDisplayFunc(displayStub);
             glutIdleFunc(idleStub);
             glutMainLoop();
```
