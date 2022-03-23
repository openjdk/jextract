## jextract

`jextract` is a tool that mechanically generates Java bindings from a native library headers. We would like to include this tool, originally developed in the context of [Project Panama](https://openjdk.java.net/projects/panama/) (and available in the Project Panama [Early Access binaries](https://jdk.java.net/panama/)) in the set of tools that are part of the code-tools project.

The Java SE 18 API defines an [incubating API](https://openjdk.java.net/jeps/419) to:

* manipulate foreign memory, that is memory which resides *outside* the Java heap; and

* invoke foreign functions, that is functions whose implementation is not defined in Java.

To allocate and access off-heap memory, clients can use the `MemorySegment` API. Memory segments can be associated with optional *layouts* (e.g `MemoryLayout`) which describe the contents of a given memory region and can be used to derive access expression e.g. into a struct field:

```java
// struct Point2d {
//     double x;
//     double y;
// }
MemoryLayout POINT_2D = MemoryLayout.structLayout(
        ValueLayout.JAVA_DOUBLE.withName("x"),
        ValueLayout.JAVA_DOUBLE.withName("y"),
);

VarHandle xHandle = POINT_2D.varHandle(PathElement.groupElement("x")); // var handle to access Point2d::x
VarHandle yHandle = POINT_2D.varHandle(PathElement.groupElement("y")); // var handle to access Point2d::x

MemorySegment segment = MemorySegment.allocateNative(POINT_2D, ReosurceScope.newImplicitScope());
xHandle.set(segment, 3d);
yHandle.set(segment, 4d);
```

Furthermore, clients can use the `CLinker` API to create a so called *downcall* method handle, that is, a method handle which targets a native function, directly:

```java
// double distance(struct Point2d);
MethodHandle distance = CLinker.systemCLinker().downcallHandle(
                                    LibraryLookup.loaderLookup().lookup("distance").get()
                                    FunctionDescriptor.of(ValueLayout.JAVA_DOUBLE, POINT_2D);
distance.invoke(segment);
```

While the new APIs allow clients to manipulate off-heap memory and call native functions using only Java code (unlike JNI), handwriting the above code can be tedious and error-prone, especially when working with big libraries. For instance, inferring the layout of a struct is not an easy task, given that different platforms might have different size and alignment requirements, resulting in different padding bits inserted in the struct layout.

Indeed, when working with big libraries, it would be far more convenient to rely on some tool which:

* is able to parse the contents of the header file of a given native library;
* identify the set of functions, structs and constant symbols that need to be generated;
* for each of the symbols identified in the previous step, emit a Java binding using the Java 18 API.

In other word, to work with a given native library, a developer would have to point the tool to the header file(s) of said library; the tool will then generate a set of Java sources that the developer can check-in into the source code repository, and then update when required (e.g. when the native library is updated), by re-running the tool.

### Description

`jextract` is a tool that leverages the [clang C API](https://clang.llvm.org/doxygen/group__CINDEX.html) in order to parse the headers associated to a given native library and generate the Java glue code that is required to interact with said library. More specifically, jextract generates the following code:

* for each native function, a downcall method handle constant is created and a small static method which wraps the downcall method handle invocation (`invokeExact`) is also generated;
* for each global variable, some static accessor methods are generated, which can be used to get, set or retrieve the address of the global variable;
* for each struct/union type, a `MemoryLayout` constant is generated, which describes the platform-specific layout of the struct/union. Moreover, for each struct/union, some static accessor methods for all fields in the struct/union are also generated. These accessors can be used to get, set or retrieve the address of a field in a given struct.
* for each function pointer type mentioned in function signatures, global variables or struct/union field types, a new functional interface is emitted, together with a static factory method, which allows clients to create a function pointer (of type `MemoryAddress`) using a simple Java lambda expression, or a method reference;
* for each constant defined using the  `#define`  directive, a numeric constant (of a suitable Java primitive type) or pointer constant (of type `MemoryAddress`) is generated;
* for each enum constant, a numeric constant (of a suitable Java primitive type) is generated;
* each `typedef` is visited recursively. If the entity being defined is a struct/union, then the struct/union is processed accordingly (see above). If the entity being defined is a function pointer, then the function pointer is processed accordingly (see above). If the `typedef` refers to a primitive type, an additional `MemoryLayout` constant is generated;
* finally, a starter set of `MemoryLayout` constants (for each basic C type) is also generated.

To understand how `jextract` works, consider the following C header file:

```c
//point.h
struct Point2d {
    double x;
    double y;
};

double distance(struct Point2d);
```

We can run `jextract`, as follows:

```
jextract --source -t org.jextract point.h
```

We can then use the generated code as follows:

```java
import jdk.incubator.foreign.*;
import static org.jextract.point_h.*;
import org.jextract.Point2d;

class TestPoint {
    public static void main(String[] args) {
        try (ResourceScope scope = ResourceScope.newConfinedScope()) {
           MemorySegment point = MemorySegment.allocateNative(Point2d.$LAYOUT(), scope);
           Point2d.x$set(point, 3d);
           Point2d.y$set(point, 4d);
           distance(point);
        }
    }
}
```

As we can see, the `jextract` tool generated a `Point2d` class, modelling the C struct, and a `point_h` class which contains static native function wrappers, such as `distance`. If we look inside the generated code for `distance` we can find the following:

```java
static final FunctionDescriptor distance$FUNC =
    FunctionDescriptor.of(Constants$root.C_DOUBLE$LAYOUT,
                          MemoryLayout.structLayout(
    	                      Constants$root.C_DOUBLE$LAYOUT.withName("x"),
                              Constants$root.C_DOUBLE$LAYOUT.withName("y")
                          ).withName("Point2d"));

static final MethodHandle distance$MH = RuntimeHelper.downcallHandle(
    "distance",
    constants$0.distance$FUNC, false
);

public static MethodHandle distance$MH() {
    return RuntimeHelper.requireNonNull(constants$0.distance$MH,"distance");
}
public static double distance ( MemorySegment x0) {
    var mh$ = RuntimeHelper.requireNonNull(constants$0.distance$MH, "distance");
    try {
        return (double)mh$.invokeExact(x0);
    } catch (Throwable ex$) {
        throw new AssertionError("should not reach here", ex$);
    }
}
```

In other words, the `jextract` tool has generated all the required supporting code (`MemoryLayout`, `MethodHandle` and `FunctionDescriptor`) that is needed to call the underlying `distance` native function.

The `jextract` tool includes several customization options. Users can select what in which package the generated code should be emitted, and what the name of the main extracted class should be. To allow for symbol filtering, jextract can generate a *dump* of all the symbols encountered in an header file; this dump can be manipulated, and then used as an argument file (using the `@argfile` syntax also available in other JDK tools) to e.g. generate bindings only for a *subset* of symbols seen by `jextract`. For instance, if we run `jextract` with as follows:

```
jextract --dump-includes=includes.txt point.h
```

We obtain the following file (`includes.txt`):

```
#### Extracted from: point.h

--include-struct Point2d    # header: point.h
--include-function distance # header: point.h
```

This file can be passed back to jextract, as follows:

```
jextract -t org.jextract --source @includes.txt point.h
```

It is easy to see how this mechanism allows developers to look into the set of symbols seen by `jextract` while parsing, and then process the generated include file, so as to prevent code generation for otherwise unused symbols.

For more examples on how to use the `jextract` tool with real-world libraries, please refer to this [document](https://github.com/openjdk/panama-foreign/blob/d8c0fe5918cb1c6c744eb26797ea4fa04142c237/doc/panama_jextract.md).


### Building jextract tool

jextract depends on clang+LLVM binaries. Please download and install clang+LLVM binaries for your platform.
You can find the prebuilt binaries from [https://releases.llvm.org/download.html](https://releases.llvm.org/download.html). The path of the clang+LLVM installation is provided using the `LIBCLANG_HOME` variable.

Gradle tool needs jdk 17 or below to run. JAVA_HOME should be set to
jdk 17 or below. Or PATH should contain java from jdk 17 or below. jdk18 build is
needed to build jextract which is passed from command with -Pjdk18_home option.

You can download jdk18 early access build from [https://jdk.java.net/18/](https://jdk.java.net/18/)

For Windows, please use gradlew.bat.

```sh

$ sh ./gradlew -Pjdk18_home=<jdk18_home_dir> -PLIBCLANG_HOME=<libclang_dir> clean verify

```

### Testing jextract tool

jextract tests are written for jtreg test framework. Please download and install jtreg binaries.
The path of the jtreg installation is provided using the `jtreg_home` variable.

For Windows, please use gradlew.bat.

```sh

$ sh ./gradlew -Pjdk18_home=<jdk18_home_dir> -PLIBCLANG_HOME=<libclang_dir> -Pjtreg_home=<jtreg_dir> clean jtreg

```

### jextract samples

jextract samples can be found "samples" top-level directory. Building/running particular sample may require
specific third-party software installation.
