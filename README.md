## Jextract

`jextract` is a tool which mechanically generates Java bindings from a native library headers. This tools leverages the [clang C API](https://clang.llvm.org/doxygen/group__CINDEX.html) in order to parse the headers associated with a given native library, and the generated Java bindings build upon the [Foreign Function & Memory API](https://openjdk.java.net/jeps/419). The `jextract` tool was originally developed in the context of [Project Panama](https://openjdk.java.net/projects/panama/) (and then made available in the Project Panama [Early Access binaries](https://jdk.java.net/panama/)).

### Getting started

`jextract` depends on the [C libclang API](https://clang.llvm.org/doxygen/group__CINDEX.html). To build the jextract sources, the easiest option is to download LLVM binaries for your platform, which can be found [here](https://releases.llvm.org/download.html) (a version <= 9 is required). Both the `jextract` tool and the bindings it generates depend heavily on the [Foreign Function & Memory API](https://openjdk.java.net/jeps/419), so a suitable [jdk 18 distribution](https://jdk.java.net/18/) is also required.

`jextract` can be built using `gradle`, as follows (on Windows, `gradlew.bat` should be used instead):

```sh
$ sh ./gradlew -Pjdk18_home=<jdk18_home_dir> -Plibclang_home=<libclang_dir> clean verify
```

After building, there should be a new `jextract` folder under `build` (the contents and the name of this folder might vary slightly depending on the platform):

```
build/jextract
├── bin
└── lib
    ├── app
    └── runtime
        ├── bin
        ├── conf
        ├── include
        ├── legal
        └── lib
```

To run the `jextract` tool, simply run the `jextract` command in the `bin` folder (again, the exact location of the binary might vary slightly depending on the platform):

```sh
build/jextract/bin/jextract 
WARNING: Using incubator modules: jdk.incubator.foreign
Expected a header file
```

The repository also contains a comprehensive set of tests, written using the [jtreg](https://openjdk.java.net/jtreg/) test framework, which can be run as follows (again, on Windows, `gradlew.bat` should be used instead):

```sh
$ sh ./gradlew -Pjdk18_home=<jdk18_home_dir> -Plibclang_home=<libclang_dir> jtreg
```

### Using jextract

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

In other words, the `jextract` tool has generated all the required supporting code (`MemoryLayout`, `MethodHandle` and `FunctionDescriptor`) that is needed to call the underlying `distance` native function. For more examples on how to use the `jextract` tool with real-world libraries, please refer to the [samples folder](samples) (building/running particular sample may require specific third-party software installation).

#### Filtering symbols

The `jextract` tool includes several customization options. Users can select in which package the generated code should be emitted, and what the name of the main extracted class should be. To allow for symbol filtering, `jextract` can generate a *dump* of all the symbols encountered in an header file; this dump can be manipulated, and then used as an argument file (using the `@argfile` syntax also available in other JDK tools) to e.g. generate bindings only for a *subset* of symbols seen by `jextract`. For instance, if we run `jextract` with as follows:

```
jextract --dump-includes=includes.txt point.h
```

We obtain the following file (`includes.txt`):

```
#### Extracted from: point.h

--include-struct Point2d    # header: point.h
--include-function distance # header: point.h
```

This file can be passed back to `jextract`, as follows:

```
jextract -t org.jextract --source @includes.txt point.h
```

It is easy to see how this mechanism allows developers to look into the set of symbols seen by `jextract` while parsing, and then process the generated include file, so as to prevent code generation for otherwise unused symbols.

