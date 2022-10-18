## Jextract

`jextract` is a tool which mechanically generates Java bindings from a native library headers. This tools leverages the [clang C API](https://clang.llvm.org/doxygen/group__CINDEX.html) in order to parse the headers associated with a given native library, and the generated Java bindings build upon the [Foreign Function & Memory API](https://openjdk.java.net/jeps/424). The `jextract` tool was originally developed in the context of [Project Panama](https://openjdk.java.net/projects/panama/) (and then made available in the Project Panama [Early Access binaries](https://jdk.java.net/panama/)).

### Getting started

`jextract` depends on the [C libclang API](https://clang.llvm.org/doxygen/group__CINDEX.html). To build the jextract sources, the easiest option is to download LLVM binaries for your platform, which can be found [here](https://releases.llvm.org/download.html) (a version >= 9 is required). Both the `jextract` tool and the bindings it generates depend heavily on the [Foreign Function & Memory API](https://openjdk.java.net/jeps/424), so a suitable build of the [panama/foreign repository](https://github.com/openjdk/panama-foreign) is also required.

> <details><summary><strong>Building older jextract versions</strong></summary>
> 
> The `master` branch always tracks the latest version of the JDK. If you wish to build an older version of jextract, which targets an earlier version of the JDK you can do so by chercking out the appropriate branch.
> For example, to build a jextract tool which works against JDK 18:
> 
> `git checkout jdk18`
> 
> Over time, new branches will be added, each targeting a specific JDK version.
> </details>

`jextract` can be built using `gradle`, as follows (on Windows, `gradlew.bat` should be used instead).

(**Note**: Run the Gradle build with a Java version appropriate for the Gradle version. For example, Gradle 7.5.1
supports JDK 18. Please checkout the [Gradle compatibility matrix](https://docs.gradle.org/current/userguide/compatibility.html#java) for the appropate JDK version needed for builds)



```sh
$ sh ./gradlew -Pjdk20_home=<jdk20_home_dir> -Pllvm_home=<libclang_dir> clean verify
```


> <details><summary><strong>Using a local installation of LLVM</strong></summary>
> 
> While the recommended way is to use a [release from the LLVM project](https://releases.llvm.org/download.html),
> extract it then make `llvm_home` point to this directory, it may be possible to use a local installation instead.
>
> E.g. on macOs the `llvm_home` can also be set as one of these locations :
> 
> * `/Library/Developer/CommandLineTools/usr/` if using Command Line Tools
> * `/Applications/Xcode.app/Contents/Developer/Toolchains/XcodeDefault.xctoolchain/usr/` if using XCode
> * `$(brew --prefix llvm)` if using the [LLVM install from Homebrew](https://formulae.brew.sh/formula/llvm#default)
> 
> </details>

After building, there should be a new `jextract` folder under `build`.
To run the `jextract` tool, simply run the `jextract` command in the `bin` folder:

```sh
$ build/jextract/bin/jextract
Expected a header file
```

The repository also contains a comprehensive set of tests, written using the [jtreg](https://openjdk.java.net/jtreg/) test framework, which can be run as follows (again, on Windows, `gradlew.bat` should be used instead):

```sh
$ sh ./gradlew -Pjdk20_home=<jdk20_home_dir> -Pllvm_home=<libclang_dir> -Pjtreg_home=<jtreg_home> jtreg
```

Note however that running `jtreg` task requires `cmake` to be available on the `PATH`.

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
import java.lang.foreign.*;
import static org.jextract.point_h.*;
import org.jextract.Point2d;

class TestPoint {
    public static void main(String[] args) {
        try (var session = MemorySession.openConfined()) {
           MemorySegment point = MemorySegment.allocateNative(Point2d.$LAYOUT(), session);
           Point2d.x$set(point, 3d);
           Point2d.y$set(point, 4d);
           distance(point);
        }
    }
}
```

As we can see, the `jextract` tool generated a `Point2d` class, modelling the C struct, and a `point_h` class which contains static native function wrappers, such as `distance`. If we look inside the generated code for `distance` we can find the following:

```java
static final FunctionDescriptor distance$FUNC = FunctionDescriptor.of(Constants$root.C_DOUBLE$LAYOUT,
    MemoryLayout.structLayout(
         Constants$root.C_DOUBLE$LAYOUT.withName("x"),
         Constants$root.C_DOUBLE$LAYOUT.withName("y")
    ).withName("Point2d")
);
static final MethodHandle distance$MH = RuntimeHelper.downcallHandle(
    "distance",
    constants$0.distance$FUNC
);

public static MethodHandle distance$MH() {
    return RuntimeHelper.requireNonNull(constants$0.distance$MH,"distance");
}
public static double distance ( MemorySegment x0) {
    var mh$ = distance$MH();
    try {
        return (double)mh$.invokeExact(x0);
    } catch (Throwable ex$) {
        throw new AssertionError("should not reach here", ex$);
    }
}
```

In other words, the `jextract` tool has generated all the required supporting code (`MemoryLayout`, `MethodHandle` and `FunctionDescriptor`) that is needed to call the underlying `distance` native function. For more examples on how to use the `jextract` tool with real-world libraries, please refer to the [samples folder](samples) (building/running particular sample may require specific third-party software installation).

#### Command line options

The `jextract` tool includes several customization options. Users can select in which package the generated code should be emitted, and what the name of the main extracted class should be. If no package is specified, classes are generated in the unnamed package. If no name is specified for the main header class, then the header class name is
derived from the header file name. For example, if jextract is run on foo.h, then foo_h will be the name of the main header class.

A complete list of all the supported options is given below:

| Option                                                       | Meaning                                                      |
| :----------------------------------------------------------- | ------------------------------------------------------------ |
| `-D --define-macro <macro>=<value>`                          | define <macro> to <value> (or 1 if <value> omitted)          |
| `--header-class-name <name>`                                 | name of the generated header class. If this option is not specified, then header class name is derived from the header file name. For example, class "foo_h" for header "foo.h". |
| `-t, --target-package <package>`                             | target package name for the generated classes. If this option is not specified, then unnamed package is used.  |
| `-I, --include-dir <dir>`                                    | append directory to the include search paths. Include search paths are searched in order. For example, if `-I foo -I bar` is specified, header files will be searched in "foo" first, then (if nothing is found) in "bar".|
| `-l, --library <name \| path>`                               | specify a library by platform-independent name (e.g. "GL") or by absolute path ("/usr/lib/libGL.so") that will be loaded by the generated class. |
| `--output <path>`                                            | specify where to place generated files                       |
| `--source`                                                   | generate java sources instead of classfiles                  |
| `--dump-includes <String>`                                   | dump included symbols into specified file (see below)        |
| `--include-[function,constant,struct,union,typedef,var]<String>` | Include a symbol of the given name and kind in the generated bindings (see below). When one of these options is specified, any symbol that is not matched by any specified filters is omitted from the generated bindings. |
| `--version`                                                  | print version information and exit                           |


#### Additional clang options

Users can specify additional clang compiler options, by creating a file named
`compile_flags.txt` in the current folder, as described [here](https://clang.llvm.org/docs/JSONCompilationDatabase.html#alternatives).

#### Filtering symbols

To allow for symbol filtering, `jextract` can generate a *dump* of all the symbols encountered in an header file; this dump can be manipulated, and then used as an argument file (using the `@argfile` syntax also available in other JDK tools) to e.g. generate bindings only for a *subset* of symbols seen by `jextract`. For instance, if we run `jextract` with as follows:

```
jextract --dump-includes includes.txt point.h
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

