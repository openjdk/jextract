This directory contains files, script needed to update libclang binding in jextract code.

Steps to update libclang binding

* remove the src/main/java/org/openjdk/jextract/clang/libclang folder
* make sure jextract project is built
* cd updateclang (this directory)
* set PATH to point to directory where generated jextract tool lives
* set LIBCLANG_HOME to point to the home of the LLVM distribution
* run sh ./extract.sh

Manually update the following file:

File: Index_h.java


    // Manual change to handle platform specific library name difference
    static {
        // Manual change to handle platform specific library name difference
        String libName = System.getProperty("os.name").startsWith("Windows")? "libclang" : "clang";
        System.loadLibrary(libName);
    }

File: CXUnsavedFile.java

```diff
// Manual change the layout of CXUnsavedFile to correctly handle the platform specific size of C_LONG

diff --git a/src/main/java/org/openjdk/jextract/clang/libclang/CXUnsavedFile.java b/src/main/java/org/openjdk/jextract/clang/libclang/CXUnsavedFile.java
index 12d25de..a0ebb98 100644
--- a/src/main/java/org/openjdk/jextract/clang/libclang/CXUnsavedFile.java
+++ b/src/main/java/org/openjdk/jextract/clang/libclang/CXUnsavedFile.java
@@ -52,11 +52,20 @@ public class CXUnsavedFile {
         // Should not be called directly
     }

-    private static final GroupLayout $LAYOUT = MemoryLayout.structLayout(
-        Index_h.C_POINTER.withName("Filename"),
-        Index_h.C_POINTER.withName("Contents"),
-        Index_h.C_LONG.withName("Length")
-    ).withName("CXUnsavedFile");
+    private static final GroupLayout $LAYOUT = (switch (Index_h.C_LONG) {
+        case OfInt _ -> MemoryLayout.structLayout(
+            Index_h.C_POINTER.withName("Filename"),
+                Index_h.C_POINTER.withName("Contents"),
+                Index_h.C_LONG.withName("Length"),
+                MemoryLayout.paddingLayout(4)
+        );
+        case OfLong _ -> MemoryLayout.structLayout(
+            Index_h.C_POINTER.withName("Filename"),
+                Index_h.C_POINTER.withName("Contents"),
+                Index_h.C_LONG.withName("Length")
+        );
+        default -> throw new IllegalStateException("Unhandled layout: " + Index_h.C_LONG);
+    }).withName("CXUnsavedFile");

     /**
      * The layout of this struct
@@ -153,7 +162,7 @@ public class CXUnsavedFile {
         struct.set(Contents$LAYOUT, Contents$OFFSET, fieldValue);
     }

-    private static final OfLong Length$LAYOUT = (OfLong)$LAYOUT.select(groupElement("Length"));
+    private static final ValueLayout Length$LAYOUT = (ValueLayout) $LAYOUT.select(groupElement("Length"));

     /**
      * Layout for field:
@@ -161,7 +170,7 @@ public class CXUnsavedFile {
      * unsigned long Length
      * }
      */
-    public static final OfLong Length$layout() {
+    public static final ValueLayout Length$layout() {
         return Length$LAYOUT;
     }

@@ -184,7 +193,11 @@ public class CXUnsavedFile {
      * }
      */
     public static long Length(MemorySegment struct) {
-        return struct.get(Length$LAYOUT, Length$OFFSET);
+        return switch (Length$LAYOUT) {
+            case OfInt l -> struct.get(l, Length$OFFSET);
+            case OfLong l -> struct.get(l, Length$OFFSET);
+            default -> throw new IllegalStateException("Unhandled layout: " + Length$LAYOUT);
+        };
     }

     /**
@@ -194,7 +207,11 @@ public class CXUnsavedFile {
      * }
      */
     public static void Length(MemorySegment struct, long fieldValue) {
-        struct.set(Length$LAYOUT, Length$OFFSET, fieldValue);
+        switch (Length$LAYOUT) {
+            case OfInt l -> struct.set(l, Length$OFFSET, Math.toIntExact(fieldValue));
+            case OfLong l -> struct.set(l, Length$OFFSET, fieldValue);
+            default -> throw new IllegalStateException("Unhandled layout: " + Length$LAYOUT);
+        }
     }

     /**
```
