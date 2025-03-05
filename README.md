## Jextract

`jextract` is a tool which mechanically generates Java bindings from native library headers. This tools leverages the [clang C API](https://clang.llvm.org/doxygen/group__CINDEX.html) in order to parse the headers associated with a given native library, and the generated Java bindings build upon the [Foreign Function & Memory API](https://openjdk.java.net/jeps/454). The `jextract` tool was originally developed in the context of [Project Panama](https://openjdk.java.net/projects/panama/) (and then made available in the Project Panama [Early Access binaries](https://jdk.java.net/panama/)).

:bulb: For instruction on how to use the jextract tool, please refer to the guide [here](doc/GUIDE.md).

### Getting jextract

Pre-built binaries for jextract are periodically released [here](https://jdk.java.net/jextract). These binaries are built from the `master` branch of this repo, and target the foreign memory access and function API in the latest mainline JDK (for which binaries can be found [here](https://jdk.java.net)).

Alternatively, to build jextract from the latest sources (which include all the latest updates and fixes) please refer to the [building](#building) section below.

---

### Building

`jextract` depends on the [C libclang API](https://clang.llvm.org/doxygen/group__CINDEX.html). To build the jextract sources, the easiest option is to download LLVM binaries for your platform, which can be found [here](https://releases.llvm.org/download.html) (version 13.0.0 is recommended). Both the `jextract` tool and the bindings it generates depend heavily on the [Foreign Function & Memory API](https://openjdk.java.net/jeps/454). A suitable [JDK 23 or higher distribution](https://jdk.java.net/23/) is also required.

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

We currently use gradle version 8.11.1 which is fetched automatically by the gradle wrapper. Please refer to the [compatibility matrix](https://docs.gradle.org/current/userguide/compatibility.html) to see which version of java is needed in `PATH`/`JAVA_HOME` to run gradle. Note that the JDK we use to build (the toolchain JDK) is passed in separately as a property.



```sh
$ sh ./gradlew -Pjdk_home=<jdk_home_dir> -Pllvm_home=<libclang_dir> clean verify
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
$ sh ./gradlew -Pjdk_home=<jdk_home_dir> -Pllvm_home=<libclang_dir> -Pjtreg_home=<jtreg_home> jtreg
```

Note: running `jtreg` task requires `cmake` to be available on the `PATH`.
