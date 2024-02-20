## Jextract

`jextract` is a tool which mechanically generates Java bindings from a native library headers. This tools leverages the [clang C API](https://clang.llvm.org/doxygen/group__CINDEX.html) in order to parse the headers associated with a given native library, and the generated Java bindings build upon the [Foreign Function & Memory API](https://openjdk.java.net/jeps/454). The `jextract` tool was originally developed in the context of [Project Panama](https://openjdk.java.net/projects/panama/) (and then made available in the Project Panama [Early Access binaries](https://jdk.java.net/panama/)).

### Getting jextract

Pre-built binaries for jextract are periodically released [here](https://jdk.java.net/jextract). These binaries are built from the `master` branch of this repo, and target the foreign memory access and function API in the latest mainline JDK (for which binaries can be found [here](https://jdk.java.net)).

Alternatively, to build jextract from the latest sources (which include all the latest updates and fixes) please refer to the [building](#building) section below.

---

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
jextract -l distance -t org.jextract point.h
```

We can then use the generated code as follows:

```java
import java.lang.foreign.*;
import static org.jextract.point_h.*;
import org.jextract.Point2d;

class TestPoint {
    public static void main(String[] args) {
        try (Arena arena = Arena.ofConfined()) {
           MemorySegment point = Point2d.allocate(arena);
           Point2d.x(point, 3d);
           Point2d.y(point, 4d);
           System.out.println("Distance to origin = " + distance(point));
        }
    }
}
```

(Note that, to run the above example, a native library called  `(lib)distance.(so|dylib|dll)` that exports the `distance` function needs to be available on the OS's standard library search path. `LD_LIBRARY_PATH` on Linux, `DYLD_LIBRARY_PATH` on Mac, or `PATH` on Windows)

As we can see, the `jextract` tool generated a `Point2d` class, modelling the C struct, and a `point_h` class which contains static native function wrappers, such as `distance`. If we look inside the generated code for `distance` we can find the following (for clarity, some details have been omitted):

```java
static final FunctionDescriptor DESC = FunctionDescriptor.of(
        foo_h.C_DOUBLE,
        Point2d.$LAYOUT()
);

static final MethodHandle MH = Linker.nativeLinker().downcallHandle(
        foo_h.findOrThrow("distance"),
        DESC);

public static double distance(MemorySegment x0) {
    return (double) mh$.invokeExact(x0);
}
```

In other words, the `jextract` tool has generated all the required supporting code (`MemoryLayout`, `MethodHandle` and `FunctionDescriptor`) that is needed to call the underlying `distance` native function. For more examples on how to use the `jextract` tool with real-world libraries, please refer to the [samples folder](samples) (building/running particular sample may require specific third-party software installation).

#### Command line options

The `jextract` tool includes several customization options. Users can select in which package the generated code should be emitted, and what the name of the main extracted class should be. If no package is specified, classes are generated in the unnamed package. If no name is specified for the main header class, then the header class name is
derived from the header file name. For example, if jextract is run on foo.h, then foo_h will be the name of the main header class.

A complete list of all the supported options is given below:

| Option                                                       | Meaning                                                      |
| :----------------------------------------------------------- | ------------------------------------------------------------ |
| `-D --define-macro <macro>=<value>`                          | define `<macro>` to `<value>` (or 1 if `<value>` omitted)          |
| `--header-class-name <name>`                                 | name of the generated header class. If this option is not specified, then header class name is derived from the header file name. For example, class "foo_h" for header "foo.h". |
| `-t, --target-package <package>`                             | target package name for the generated classes. If this option is not specified, then unnamed package is used.  |
| `-I, --include-dir <dir>`                                    | append directory to the include search paths. Include search paths are searched in order. For example, if `-I foo -I bar` is specified, header files will be searched in "foo" first, then (if nothing is found) in "bar".|
| `-l, --library <name \| path>`                               | specify a shared library that should be loaded by the generated header class. If <libspec> starts with `:`, then what follows is interpreted as a library path. Otherwise, `<libspec>` denotes a library name. Examples: <br>`-l GL`<br>`-l :libGL.so.1`<br>`-l :/usr/lib/libGL.so.1`|
| `--use-system-load-library`                                  | libraries specified using `-l` are loaded in the loader symbol lookup (using either `System::loadLibrary`, or `System::load`). Useful if the libraries must be loaded from one of the paths in `java.library.path`.| 
| `--output <path>`                                            | specify where to place generated files                       |
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
jextract -t org.jextract @includes.txt point.h
```

It is easy to see how this mechanism allows developers to look into the set of symbols seen by `jextract` while parsing, and then process the generated include file, so as to prevent code generation for otherwise unused symbols.

Users should exercise caution when filtering symbols, as it is relatively easy to filter out a declaration that is depended on by one or more declarations:

```c
// test.h
struct A {
   int x;
}
struct A aVar;
```

Here, we could run `jextract` and filter out `A`, like so:

```
jextract --include-var aVar test.h
```

However, doing so would lead to broken generated code, as the layout of the global variable `aVar` depends on the layout of the excluded struct `A`.

In such cases, `jextract` will report the missing dependency and terminate without generating any bindings:

```
ERROR: aVar depends on A which has been excluded
```

#### Tracing support

It is sometimes useful to inspect the parameters passed to a native call, especially when diagnosing application
bugs and/or crashes. The code generated by the `jextract` tool supports *tracing* of native calls, that is, parameters
passed to native calls can be printed on the standard output.

To enable the tracing support, just pass the `-Djextract.trace.downcalls=true` flag to the launcher used to start the application.
Below we show an excerpt of the output when running the [OpenGL example](samples/opengl) with tracing support enabled:

```
glutInit(MemorySegment{ address: 0x7fa6b03d6400, byteSize: 4 }, MemorySegment{ address: 0x7fa6b03d6400, byteSize: 4 })
glutInitDisplayMode(18)
glutInitWindowSize(900, 900)
glutCreateWindow(MemorySegment{ address: 0x7fa6b03f8e70, byteSize: 14 })
glClearColor(0.0, 0.0, 0.0, 0.0)
glShadeModel(7425)
glLightfv(16384, 4611, MemorySegment{ address: 0x7fa6b03de8d0, byteSize: 16 })
glLightfv(16384, 4608, MemorySegment{ address: 0x7fa6b0634840, byteSize: 16 })
glLightfv(16384, 4609, MemorySegment{ address: 0x7fa6b0634840, byteSize: 16 })
glLightfv(16384, 4610, MemorySegment{ address: 0x7fa6b0634840, byteSize: 16 })
glMaterialfv(1028, 5633, MemorySegment{ address: 0x7fa6b0634860, byteSize: 4 })
glEnable(2896)
glEnable(16384)
glEnable(2929)
glutDisplayFunc(MemorySegment{ address: 0x7fa6a002e820, byteSize: 0 })
glutIdleFunc(MemorySegment{ address: 0x7fa6a015a620, byteSize: 0 })
glutMainLoop()
glClear(16640)
glPushMatrix()
glRotatef(-20.0, 1.0, 1.0, 0.0)
glRotatef(0.0, 0.0, 1.0, 0.0)
glutSolidTeapot(0.5)
```

---

### Building

`jextract` depends on the [C libclang API](https://clang.llvm.org/doxygen/group__CINDEX.html). To build the jextract sources, the easiest option is to download LLVM binaries for your platform, which can be found [here](https://releases.llvm.org/download.html) (version 13.0.0 is recommended). Both the `jextract` tool and the bindings it generates depend heavily on the [Foreign Function & Memory API](https://openjdk.java.net/jeps/454), so a suitable [jdk 22 distribution](https://jdk.java.net/22/) is also required.

> <details><summary><strong>Building older jextract versions</strong></summary>
>
> The `master` branch always tracks the latest version of the JDK. If you wish to build an older version of jextract, which targets an earlier version of the JDK you can do so by checking out the appropriate branch.
> For example, to build a jextract tool which works against JDK 21:
>
> `git checkout jdk21`
>
> Over time, new branches will be added, each targeting a specific JDK version.
> </details>

`jextract` can be built using `gradle`, as follows (on Windows, `gradlew.bat` should be used instead).

We currently use gradle version 7.3.3 which is fetched automatically by the gradle wrapper. This version of gradle requires Java 17 on the `PATH`/`JAVA_HOME` to run. Note that the JDK we use to build (the toolchain JDK) is passed in separately as a property.



```sh
$ sh ./gradlew -Pjdk22_home=<jdk22_home_dir> -Pllvm_home=<libclang_dir> clean verify
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

### Testing

The repository also contains a comprehensive set of tests, written using the [jtreg](https://openjdk.java.net/jtreg/) test framework, which can be run as follows (again, on Windows, `gradlew.bat` should be used instead):

```sh
$ sh ./gradlew -Pjdk22_home=<jdk22_home_dir> -Pllvm_home=<libclang_dir> -Pjtreg_home=<jtreg_home> jtreg
```

Note: running `jtreg` task requires `cmake` to be available on the `PATH`.
