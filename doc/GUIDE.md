# Jextract Guide

The jextract tool parses header (.h) files of native libraries, and generates Java code,
called _bindings_, which use the [Foreign Function and Memory API](https://openjdk.org/jeps/454)
(FFM API) under the hood, that can be used by a client to access the native library.

Interacting with native C code through the FFM API works
by loading a native library (e.g., a `.so`/`.dll`/`.dylib` file), which is essentially an
archive of native functions and global variables. The user then has to look up the
functions they want to call using a [`SymbolLookup`], and finally _link_ the functions by
using the [`Linker::downcallHandle`] method. Additionally, a client may need to create
function pointers for Java functions using [`Linker::upcallStub`], access global variables
through the addresses returned by a lookup, and construct [`MemoryLayout`] instances for
the structs they want to access. The jextract tool aims to automate many of these steps,
so that a client can instead immediately start using the native libraries they are
interested in.

This guide shows how to run the jextract tool, and how to use the Java code that it generates.
The samples under the [`samples`](../samples) directory are also a good source of examples.

Note that at this time, jextract (and FFM) only supports C header files. If you have a
library written in another language, see the section on [other languages](#other-languages).

## Running Jextract

A native library typically has an `include` directory which contains all the header files
that define the interface of the library, with one _main_ header file. Let's say we have a
library called `mylib` stored at `/path/to/mylib` that has a directory `/path/to/mylib/include`
where the header files of that library are stored. And let's say that we have a shell open
in the root directory of the Java project we're working on, which has an `src` source
directory corresponding to the root package. A typical way to run jextract would be like
this:

```sh
$ jextract \
  --include-dir /path/to/mylib/include \
  --output src \
  --target-package org.jextract.mylib \
  --library mylib \
  /path/to/mylib/include/mylib.h
```

In this command:

- `/path/to/mylib/include/mylib.h` is the main header file of the native library we want
  to generate bindings for.
- `--include-dir /path/to/mylib/include` specifies a header file search directory, which
   is used to find header files included through `#include` in the main header file.
- `--output src` specifies the root directory for the output. This matches the root package
  of the project's source directory.
- `--target-package org.jextract.mylib` specifies the target package to which the generated
  classes and interfaces will belong. (Note that jextract will automatically create the
  directories representing the package structure under the `src` directory specified
  through `--output`)
- `--library mylib` tells jextract that the generated bindings should load the library
  called `mylib`. (The section on [library loading](#library-loading) discusses how this
  is done)

Note that specifying the wrong header file to jextract may result in errors during parsing.
Please consult the documentation of the library in question about which header file
should be included. This is also the header file that should be passed to jextract.

If a library has multiple main header files, they can be passed to jextract on the command line.
Also, there is special syntax to pass header files relative to C compiler include paths.
The section on [command line option reference](#command-line-option-reference) discusses this.


The library name specified to `--library` will be mapped to a platform specific library
file name, and should be findable through the OS's library search mechanism, typically by
specifying the library's containing directory on `LD_LIBRARY_PATH` (Linux),
`DYLD_LIBRARY_PATH` (Mac), or `PATH` (Windows). See the section on
[library loading](#library-loading) for more information.

Besides these options, it is also possible to filter the output of jextract using one of the `--include-XYZ` options
that jextract has. See the section on [filtering](#filtering) for a more detailed overview. See also the full
list of command line options [here](#command-line-option-reference).

Most of the code that jextract generates will be available through a single class. By default,
the name of that class is derived from the name of the main header file that is passed to
jextract. For example, if the header file is named `mylib.h`, then the derived class name
will be `mylib_h`. The class name can be set explicitly using the `--header-class-name`
option as well.

Besides the main header class that is generated, jextract also generates several
separate files for certain declarations in the C header file. Namely, structs, unions,
function pointer types, and typedefs of struct or union types result in additional files
being generated. (All of these are discussed in more detail in the
[Using The Code Generated By Jextract section](#using-the-code-generated-by-jextract)).

Generally speaking, the bindings generated by jextract depend on the platform on which
jextract is running. For example, when a C header file is processed by the C pre-processor,
it is possible for code in the header file to detect the current platform using
pre-processor directives, and expand to a different result based on that platform (see
also the section on [pre-processor definitions](#preprocessor-definitions)). Additionally,
different built in C types can have different formats depending on the platform (for
example, the `long` type has different formats on Linux and Windows). Both of these are
examples of things that can lead jextract to generate different outputs depending on the
platform on which it is run. Care should be taken when generating bindings with jextract
on one platform, and using those bindings on another platform, as the header files may
contain platform-dependent code that can cause the generated code to misbehave when used
on another platform.

However, it is also possible for a C library to be written in such a way that it is not
platform dependent: a so-called _portable_ library. These libraries, for instance, use
data types that have the same format on all supported platforms (such as `long long`
instead of `long`, or an explicitly-sized integer type such as `int64_t`). Sharing the
bindings generated for a portable library across different platforms should work without
issues. It is typically advisable to generate different sets of bindings, one on each
platform on which the bindings are intended to be used, and then comparing the generated
code to make sure that there are no differences between platforms, before sharing a single
set of bindings between different platforms.

Jextract assumes that the version of a native library that a project uses is relatively stable.
Therefore, jextract is intended to be run once, and then for the generated sources to be added to the project.
Jextract only needs to be run again when the native library, or jextract itself are updated.
(This is also the workflow that jextract itself follows: jextract depends on the
[libclang](https://clang.llvm.org/docs/LibClang.html) native library in order to parse C sources).

## Using The Code Generated By Jextract

In the following section we'll go over examples of declarations that might be found in a C
header file, show the code that jextract generates for these declarations, and show
some examples of how to use the generated Java code.

Most of the methods that jextract generates are `static`, and are designed to be imported
using `import static`. Typically, to access the code that jextract generates for a header
file called `mylib.h`, only the following two wildcard imports are needed:

```java
import static org.mypackage.mylib_h.*;
import org.mypackage.*;
```

Where `org.mypackage` is the package into which jextract puts the generates source files
(using `--target-package`/`-t`).

The former import statement will import all the static functions and fields from the class
that jextract generates for the main header file of the library. This includes methods to
access functions, global variables, macros, enums, primitive typedefs, and layouts for
builtin C types.

The latter import statement imports all the other classes generated by jextract, which
include: classes representing structs or unions, function types, and struct or union
typedefs.

### Builtin Type Layouts

For every jextract run, regardless of the contents of the library header files, jextract
will generate a set of memory layouts for the common builtin C types in the main header
class it generates:

```java
// mylib_h.java

public static final ValueLayout.OfBoolean C_BOOL = ValueLayout.JAVA_BOOLEAN;
public static final ValueLayout.OfByte C_CHAR = ValueLayout.JAVA_BYTE;
public static final ValueLayout.OfShort C_SHORT = ValueLayout.JAVA_SHORT;
public static final ValueLayout.OfInt C_INT = ValueLayout.JAVA_INT;
public static final ValueLayout.OfLong C_LONG_LONG = ValueLayout.JAVA_LONG;
public static final ValueLayout.OfFloat C_FLOAT = ValueLayout.JAVA_FLOAT;
public static final ValueLayout.OfDouble C_DOUBLE = ValueLayout.JAVA_DOUBLE;
public static final AddressLayout C_POINTER = ValueLayout.ADDRESS
        .withTargetLayout(
                MemoryLayout.sequenceLayout(Long.MAX_VALUE, ValueLayout.JAVA_BYTE));
public static final ValueLayout.OfInt C_LONG = ValueLayout.JAVA_INT;
public static final ValueLayout.OfDouble C_LONG_DOUBLE = ValueLayout.JAVA_DOUBLE;
```

The above layout constants represent the layouts for the C builtin types: `bool`, `char,`
`short,` `int`, `long long`, `float`, `double` `long`, and `long double`. Additionally,
there is a `C_POINTER` layout which represents the layout for any C pointer type (such as
`T*`). Note that the layouts that jextract generates depend on the platform that jextract
runs on. For instance, since these constants were generated on Windows, the `long` type
has the same layout as the Java `int` type, indicating a 32-bit value, and the
`long double` type has the same layout as the Java `double` type. (Note that the latter is
only available on Windows).

### Functions

Let's say we have a main library header file `mylib.h` that contains the following
function declaration:

```c
// mylib.h

void foo(int x);
```

Jextract will generate the following set of methods for this function in the main header
class it generates:

```java
// mylib_h.java

public static void foo(int x) { ... } // 1

public static MemorySegment foo$address() { ... } // 2
public static FunctionDescriptor foo$descriptor() { ... } // 3
public static MethodHandle foo$handle() { ... } // 4
```

First and foremost, there is:

1. a static wrapper method that is generated that can be used to
call the C function.

Besides that, there are also several accessors that return additional meta-data for the
method:

2. the function's address, represented as a [`MemorySegment`]
3. the [`FunctionDescriptor`]
4. the [`MethodHandle`] returned by the FFM linker, which is used to implement the static
  wrapper method (1).

The parameter types and return type of this method depend on the [carrier types] of the
layouts that make up the function descriptor of the function, which is itself derived from
the parsed header files.

### Global Variables

For a global variable declaration in a header file like this:

```c
// mylib.h

int bar;
```

Jextract generates the following:

```java
// mylib_h.java

public static OfInt bar$layout() { ... } // 1
public static MemorySegment bar$segment() { ... } // 2

public static int bar() { ... } // 3
public static void bar(int varValue) { ... } // 3
```

`bar$layout` is the FFM memory layout of the global variable (1), and `bar$segment` is the
address of the global variable (2).

Besides that, jextract also generates a getter and a setter method to get and set the value
of the global variable (3). Once again, the parameter and return type of the getter and
setter depend on the carrier type of the layout of the global variable, which is itself
derived from the header files.

### Constants (Macros & Enums)

Both macros and enums are translated similarly. If we have a header file containing these
declarations:

```c
// mylib.h

#define MY_MACRO 42
enum MY_ENUM { A, B, C };
```

Jextract will generate simple getter methods to access the constant values of the macro
and the enum constants:

```java
// mylib_h.java

public static int MY_MACRO() { ... }

public static int A() { ... }
public static int B() { ... }
public static int C() { ... }
```

Note that the enum constants are exposed as top-level methods, rather than being nested
inside a class called `MY_ENUM`, or through the use of a Java `enum`. This translation
strategy mimics C's behavior of enum constants being accessible as a top-level declaration,
and also makes it easier to do bitwise operations like `A | B`, which are not possible
with Java `enum`s.

Not all types of macros are supported though. Only macros that have a primitive numerical
value, a string, or a pointer type are supported. Most notably, function-like macros are
not supported by jextract. (See the section on [unsupported features](#unsupported-features))

Note that for macros, jextract only generates an accessor when it sees a macro definition,
like the one in the example, in the header files it parses. When a macro is defined
using `-D` on the command line, no accessor will be generated. (See the section on
[pre-processor definitions](#preprocessor-definitions))

### Structs & Unions

Things get a little more complicated for structs and unions. For a struct declaration like
this:

```c
// mylib.h

struct Point {
    int x;
    int y;
};
```

Jextract generates a separate class which roughly looks like the following:

```java
// Point.java

public class Point {
    public static final GroupLayout layout() { ... } // 3

    public static final OfInt x$layout() { ... } // 2
    public static final long x$offset() { ... } // 2

    public static int x(MemorySegment struct) { ... } // 1
    public static void x(MemorySegment struct, int fieldValue) { ... } // 1

    public static final OfInt y$layout() { ... } // 2
    public static final long y$offset() { ... } // 2

    public static int y(MemorySegment struct) { ... } // 1
    public static void y(MemorySegment struct, int fieldValue) { ... } // 1

    public static MemorySegment asSlice(MemorySegment array, long index) { ... } // 5

    public static long sizeof() { ... } // 3

    public static MemorySegment allocate(SegmentAllocator allocator) { ... } // 4
    public static MemorySegment allocateArray(long elementCount,
            SegmentAllocator allocator) { ... } // 4

    public static MemorySegment reinterpret(MemorySegment addr, Arena arena,
            Consumer<MemorySegment> cleanup) { ... } // 6
    public static MemorySegment reinterpret(MemorySegment addr, long elementCount,
            Arena arena, Consumer<MemorySegment> cleanup) { ... } // 6
}
```

There are:

1. a getter and setter for each field of the struct, which takes a pointer to a struct
  (a `MemorySegment`) to get/set the field from/to.
2. meta-data accessors for each field (`xxx$layout()` and `xxx$offset()`).
3. meta-data accessors `sizeof` and `layout`, which can be used to get the size and layout
  of the struct.
4. `allocate*` methods for allocating a single struct or arrays of structs.
5. an `asSlice` method which can be used to access elements of an array of structs.
6. two `reinterpret` methods which can be used to sanitize raw addresses
  returned by native code, or read from native memory.

The following example shows how to allocate a struct using the `allocate` method, and then
sets both the `x` and `y` field to `10` and `5` respectively:

```java
// Main.java

try (Arena arena = Arena.ofConfined()) {
    MemorySegment point = Point.allocate(arena);
    Point.x(point, 10);
    Point.y(point, 5);
    // ...
}
```

For working with arrays of structs, we can use the `allocateArray` method which accepts an
additional element count, indicating the length of the array:

```java
// Main.java

try (Arena arena = Arena.ofConfined()) {
    int arrLen = 5;
    MemorySegment points = Point.allocateArray(arrLen, arena);

    for (int i = 0; i < arrLen; i++) {
        MemorySegment element = Point.asSlice(points, i);
        Point.x(element, 10 + i);
        Point.y(element, 5 + i);
    }

    // ...
}
```

In the above example, the `asSlice` method is used to _slice_ out a section of
the array, which corresponds to a single `Point` struct element. This method
can be used to access individual elements of the `points` array when given
an index.

Finally, the `reinterpret` method can be used to _sanitize_ a pointer that is
returned from native code. Let's say we have a C function that creates an instance
of a `Point`, and returns a pointer to it, as well as a function that deletes a
point, given a pointer:

```c
struct Point* new_point(void);
void delete_point(struct Point* ptr);
```

The pointer that is returned by `new_point` does not have the correct bounds or lifetime
associated with it. These properties can not be inferred: for instance, a pointer could
point at a single `Point` struct, or an array of multiple `Point` structs.

The `reinterpret` method can be used to associate the correct bounds and lifetime:

```java
// Main.java

try (Arena arena = Arena.ofConfined()) {
    MemorySegment point = Point.reinterpret(new_point(), arena, mylib_h::delete_point);

    // ...
} // 'delete_point` called here
```

The `point` segment returned by `reinterpret` has exactly the size of one `Point` (for
arrays of struct, use the `reinterpret` overload that takes an element count as well). The
lifetime we associate with the segment is the lifetime denoted by `arena`, and when the
arena is closed, we want to call `delete_point`, which we can do by passing a method
reference to `delete_point` as a cleanup action when calling `reinterpret`.

The class that jextract generates for unions is identical to the class generated for
structs.

### Function Pointers

Jextract generates a separate class for each function pointer type found in the header
files it parses. For instance, for a function pointer `typedef` like this:

```c
// mylib.h

typedef int (*callback_t)(int x, int y);
```

Jextract generates the following class in a `callback_t.java` file:

```java
// callback_t.java

public class callback_t {
    public interface Function {
        int apply(int x, int y);
    }

    public static FunctionDescriptor descriptor() { ... } // 1
    public static MemorySegment allocate(callback_t.Function fi, Arena arena) { ... } // 2
    public static int invoke(MemorySegment funcPtr,int x, int y) { ... } // 3
}
```

In the generated class we have:

1. a meta-data accessor for the function descriptor (`descriptor()`).
2. an `allocate` method that can be used to allocate a new instance of this function
  pointer, whose implementation is defined by the `fi` functional interface instance.
3. an `invoke` method which can be used to invoke an instance of `callback_t` that
  we received from native code.

For instance, let's say we have a C function that accepts an instance of the `callback_t`
function pointer type:

```c
// mylib.c

int call_me_back(callback_t callback) {
    return callback(1, 2);
}
```

We can call this function from Java as follows (assuming this function is also exported
through the header files passed to jextract):

```java
// Main.java

try (Arena arena = Arena.ofConfined()) {
    MemorySegment cb = callback_t.allocate((a, b) -> a * b, arena);
    int result = call_me_back(cb);
    System.out.println(result); // prints: 2
} // 'cb' freed here
```

Here we use the lambda `(a, b) -> a * b` as the implementation of the `callback_t` instance
we create using `allocate`. This method returns an upcall stub like the ones
returned by the [`Linker::upcallStub`] method. The `arena` argument denotes the lifetime
of the upcall stub, meaning that the upcall stub will be freed when the arena is closed
(after which the callback instance can no longer be called).

Additionally, we can use the `callback_t::invoke` method invoke an instance of
`callback_t` that we get back from a call to a C function. Let's say we have a couple of
functions like this:

```c
// mylib.c

int mult(int x, int y) {
    return x * y;
}

callback_t get_callback(void) {
    return &mult;
}
```

The `get_callback` function returns an instance of `callback_t`, which is a function pointer
pointing to the native `mult` function. We can call the `callback_t` instance that `get_callback()`
returns in Java using the `invoke` method in the `callback_t` class that jextract generates
for us:

```java
// Main.java

MemorySegment cb = get_callback();
int result = callback_t.invoke(cb, 1, 2);
System.out.println(result); // prints: 2
```

Here the `callback_t` instance we want to invoke is passed as the first argument to
`invoke`, and then the `1` and `2` represent the arguments passed when calling the
`callback_t` instance.

Jextract generates function pointer classes like the `callback_t` class for function
pointers found in function parameter or return types, typedefs, or the types of variables
(such as struct fields or global variables):

```c
void func(void (*cb)(void)); // function parameter
void (*func(void))(void); // function return type
typedef void (*cb)(void); // typedef
void (*cb)(void); // global variable
struct Foo {
  void (*cb)(void); // struct field
};
```

### Variadic Functions

Jextract handles variadic functions differently from regular functions. Variadic functions
in C behave more or less like a template, where the calling convention changes based on the number
and types of arguments passed to the function. Because of this, the FFM linker needs to
know exactly which argument types are going to be passed to a variadic function when the
function is linked. This is described in greater detail in the [javadoc of the
`java.lang.foreign.Linker` class][`Linker`].

To make calling variadic functions easier, jextract introduces the concept of an _invoker_.
An invoker represents a particular _instantiation_ of a variadic function for a particular
set of variadic parameter types. When the header files contain a variadic function like this:

```c
// mylib.h

void foo_variadic(int x, ...);
```

Jextract doesn't generate a regular method, but a _class_, which represents the invoker:

```java
// mylib.h

public static class foo_variadic {
    public static MemorySegment address() { ... }
    public static foo_variadic makeInvoker(MemoryLayout... layouts) { ... }

    // not static!
    public MethodHandle handle() { ... }
    public FunctionDescriptor descriptor() { ... }
    public void apply(int x, Object... x1) { ... }
}
```

This class has a `static` meta-data accessor for the function address, and a `makeInvoker`
factory method which can be used to create an instance on the invoker class. The `MemoryLayout...`
arguments passed to `makeInvoker` represent the memory layouts of the variadic parameters
that are to be passed to the function. The `makeInvoker` factory essentially instantiates
the variadic function (like you would a template) for a particular set of parameter types.

We can then use the instance methods `apply` or `handle` if we want to invoke the function,
as follows:

```java
// Main.java

foo_variadic invoker = foo_variadic.makeInvoker(C_INT, C_INT, C_INT);
invoker.apply(3, 1, 2, 3);
invoker.handle().invokeExact(3, 1, 2, 3);
```

Here we instantiate `foo_variadic` for 3 parameter types of the C type `int`. These parameter
types essentially replace the `...` ellipsis in the C function type.

We can call the instantiated invoker either by calling `apply`, which will box the arguments
into an `Object[]`, or through the method handle returned by `handle()` which avoids the
overhead of boxing.

### Typedefs

As mentioned before, typedefs are either translated as a `static final` memory layout fields
in the main header class that jextract generates, or as a separate class, depending on
whether the typedef is for a primitive type or a struct/union type respectively.

Take for example the following `typedef` declarations:

```c
// mylib.h

typedef int MyInt;

struct Point {
    int x;
    int y;
};
typedef struct Point MyPoint;
```

`MyInt` is a `typedef` of the primitive type `int`, so it is translated by jextract as a
`static final` layout field in the main header class:

```java
// mylib_h.java

public static final OfInt MyInt = mylib_h.C_INT;
```

The `MyPoint` `typedef`, on the other hand, is a typedef for a struct, so it is translated
as a separate class which extends the class that is generated for the `Point` struct:

```java
// MyPoint.java

public class MyPoint extends Point { }
```

Through static inheritance, all the methods in the `Point` class are available through the
`MyPoint` class as well.

### Array Types

Jextract treats variables (global variables or struct/union fields) with an array type
specially. For instance, if we have a header file with the following declaration:

```c
// mylib.h

int FOO_ARRAY[3][5];
```

Jextract generates a few extra methods that are useful for working with arrays:

```java
// mylib_h.java

public static MemorySegment FOO_ARRAY() { ... } // 1
public static void FOO_ARRAY(MemorySegment varValue) { ... } // 1

public static int FOO_ARRAY(long index0, long index1) { ... } // 2
public static void FOO_ARRAY(long index0, long index1, int varValue) { ... } // 2

public static SequenceLayout FOO_ARRAY$layout() { ... } // 3
public static long[] FOO_ARRAY$dimensions() { ... } // 4
```

Jextract generates:

1. a getter and setter pair for the array variable. Note that the getter replaces the usual
  `XYZ$segment` method that jextract generates for global variables, as the results of the
  two methods would be identical.
2. a pair of _indexed_ getter and setter methods. These methods can be used to get or set
  a single element of the array. Each leading `long` parameter represents an index of one
  of the dimensions of the array.
3. a layout accessor, just like we have for a regular variable, but note that the return
  type is [`SequenceLayout`].
4. a `$dimensions` meta-data accessor, which returns the _dimensions_ of the array type.
  This method returns a `long[]` where each element represents the length of a dimension
  of the array type. For instance, in the example `FOO_ARRAY` has two dimensions - `3` and
  `5` respectively - so the `FOO_ARRAY$dimensions` method will return a `long[]`
  with two elements whose values are `3` and `5` in that order.

For struct and union fields, the generate methods are comparable, with an additional
leading `MemorySegment` parameter for the getters and setters, representing the struct or
union instance.

Using the generated methods, we can access the elements of `FOO_ARRAY` as follows:

```java
// Main.java

for (long i = 0; i < FOO_ARRAY$dimensions()[0]; i++) {
    for (long j = 0; j < FOO_ARRAY$dimensions()[1]; j++) {
        // print out element at FOO_ARRAY[i][j]
        int e = FOO_ARRAY(i, j);
        System.out.println("FOO_ARRAY[" + i + "][" + j + "] = " + e);
    }
}
```

### Nested Types

C allows variable declarations to have an inline anonymous type. For instance, if we
have a struct such as this:

```c
// mylib.h

struct Foo {
    struct {
        int baz;
    } bar; // field of Foo

    void (*cb)(void);
};
```

Jextract generates a _nested_ struct and function pointer class for the `bar` and `cb`
fields _inside of_ the class it generates for the `Foo` struct itself:

```java
// mylib_h.java

public class Foo {
    ...
    public static class bar { ... }
    public static final GroupLayout bar$layout() { ... }
    public static final long bar$offset() { ... }

    public static MemorySegment bar(MemorySegment struct) { ... }
    public static void bar(MemorySegment struct, MemorySegment fieldValue) { ... }
    ...
    public static class cb { ... }
    public static final AddressLayout cb$layout() { ... }
    public static final long cb$offset() { ... }

    public static MemorySegment cb(MemorySegment struct) { ... }
    public static void cb(MemorySegment struct, MemorySegment fieldValue) { ... }
    ...
}
```

In both cases, the name of the nested class is the name of the field of the struct.

Both fields also have the usual getter and setter, but note that the getter for the struct
field returns a _reference_ to the memory inside the `Foo` struct that corresponds to the
`bar` field. This means that writes to the returned memory segment will be visible in the
enclosing struct instance as well:

```java
// Main.java

try (Arena arena = Arena.ofConfined()) {
    MemorySegment foo = Foo.allocate(arena);
    MemorySegment bar = Foo.bar(foo); // reference to bar inside foo
    System.out.println(Foo.bar.baz(bar));  // prints: 0

    MemorySegment bar2 = Foo.bar.allocate(arena);
    Foo.bar.baz(bar2, 42);
    Foo.bar(foo, bar2);  // copies bar2 into foo
    System.out.println(Foo.bar.baz(bar));  // prints: 42
}
```

In the above snippet, note that the load of the `baz` field value on the last line will
_see_ the update to the `bar` field of the `foo` instance on the line before.

### Unsupported Features

Finally, there are some features that jextract does not support, listed below:

- Certain C types bigger than 64 bits (e.g. `long double` on Linux).

- Function-like macros. Alternatives include re-writing the code inside the macro in Java,
  using the FFM API, or writing a small C library which wraps the function-like macro in a
  proper exported C function that can then be linked against through the FFM API.

- Bit fields. You will see a warning about bit fields being skipped, such as:

  ```txt
  WARNING: Skipping Foo.x (bitfields are not supported)
  ```

- Opaque types. When a struct or union type is declared but not defined, like:

  ```c
  struct Foo;
  ```

  Jextract is not able to generate the regular `Foo` class. You will see a warning about
  these structs being skipped, such as:

  ```txt
  WARNING: Skipping Foo (type Declared(Foo) is not supported)
  ```

- Linker options. It is currently not possible to tell jextract to use linker options to
  link a particular function. Code will have to be edited manually to add them. (for instance,
  if the function sets `errno`, the `Linker.Option.captureCallState` option has to be added
  manually).

- It is not possible to specify the byte order of a struct field on the command line.
  Attributes that control the byte order, such as GCC's `scalar_storage_order` are currently
  ignored. Again, manual editing of the generated code is required to work around this.

## Advanced

### Preprocessor Definitions

C header files are processed by a pre-processor before they are inspected further. It is
possible for a header file to contain so-called 'compiler switches', which can be used to
conditionally generate code based on the value of a macro, for instance:

```c
#ifdef MY_MACRO
int x = 42;
#else
int x = 0;
#endif
```

The value of these macros also affects the behavior of jextract. Therefore, jextract
supports setting macro values on the command line using the `-D` or
`--define-macro <macro>=<value>` option. For instance, we can use `-D MY_MACRO` to set
the value of `MY_MACRO` in the above snippet to `1`, and trigger the first _branch_ of the
compiler switch, thereby defining `int x = 42`.

Please note that other header files included by jextract may also define macro values using
the `#define` pre-processor directive. It is therefore important to notice the order in
which header files are processed by a compiler, as feeding header files to jextract in the
wrong order may result in weird errors due to missing macro definitions. A well-known
example of this are Windows SDK headers. Almost always, the main `Windows.h` header file
should be passed to jextract for things to work correctly. Please consult the
documentation of the library that you're trying to use to find out which header file should
be included/passed to jextract.

### Library Loading

When using the `--library <libspec>` option, the generated code internally uses [`SymbolLookup::libraryLookup`]
to load libraries specified by `<libspec>`. If `<libspec>` denotes a library name, the
name is then mapped to a platform dependent name using [`System::mapLibraryName`].
This means, for instance, that on Linux, when specifying `--library mylib`, the bindings will
try to load `libmylib.so` using the OS-specific library loading mechanism on Linux, which
is [`dlopen`](https://man7.org/linux/man-pages/man3/dlopen.3.html). This way of loading
libraries also relies on OS-specific search mechanisms to find the library file. On Linux
the search path can be amended using the `LD_LIBRARY_PATH` environment variable (see the
documentation of `dlopen`). On Mac the relevant environment variable is `DYLD_LIBRARY_PATH`,
and on Windows the variable is `PATH`. Though, for the latter the overall library search
mechanism is entirely different (described [here](https://learn.microsoft.com/en-us/windows/win32/dlls/dynamic-link-library-search-order)).
When using the HotSpot JVM, the `-Xlog:library` option can also be used to log where the JVM
is trying to load a library from, which can be useful to debug a failure to load a library.

The `<libspec>` argument of the `--library` option can either be a library name, or a path
to a library file (either relative or absolute) if `<libspec>` is prefixed with the `:`
character, such as `:mylib.dll`.

It is important to understand how libraries are loaded on the platform that is being used,
as the library search mechanisms differ between them. Alternatively, JNI's library loading
and search mechanism can be used as well. When the `--use-system-load-library` option is
specified to jextract, the generated bindings will try to load libraries specified using
`--library` through [`System::loadLibrary`]. The library search path for
`System::loadLibrary` is specified through the [`java.library.path`] system property
instead of the OS-specific environment variable. Though, please note that if the loaded
library has any dependencies, those dependencies will again be loaded through the
OS-specific library loading mechanism (this is outside of the JVM's control).

When no `--library` option is specified, the generated bindings will try to load function
from libraries loaded through `System::loadLibrary` and `System::load`, using
[`SymbolLookup::loaderLookup`], with [`Linker::defaultLookup`] as a fallback. When
`--library` is specified when generating the bindings, these 2 lookup modes will be used
as a fallback.

In both cases, the library is unloaded when the class loader that loads the binding
classes is garbage collected.

### Filtering

Some libraries are incredibly large (such as `Windows.h`), and we might not be
interested in letting jextract generate code for the entire library. In cases like that,
we can use jextract's `--include-XXX` command line options to only generate classes for
the elements we specify.

To allow for symbol filtering, jextract can generate a _dump_ of all the symbols
encountered in an header file; this dump can be manipulated, and then used as an argument
file (using the `@argfile` syntax also available in other JDK tools) to e.g., generate
bindings only for a _subset_ of symbols seen by jextract. For instance, if we run
jextract with as follows:

```sh
$ jextract --dump-includes includes.txt mylib.h
```

We obtain the following file (`includes.txt`):

```sh
#### Extracted from: /workspace/myproj/mylib.h

--include-struct Foo                  # header: /workspace/myproj/mylib.h
--include-struct Point                # header: /workspace/myproj/mylib.h
--include-typedef callback_t          # header: /workspace/myproj/mylib.h
--include-function call_me_back       # header: /workspace/myproj/mylib.h
...
```

The include options in this file can then be edited down to a set of symbols that is
desired, for instance, using other command line tools such as `grep` or `Select-String`,
and passed back to jextract:

```sh
$ jextract --dump-includes includes.txt mylib.h
$ grep Foo includes.txt > includes_filtered.txt
$ jextract @includes_filtered.txt mylib.h
```

Users should exercise caution when filtering symbols, as it is relatively easy to filter
out a declaration that is depended on by one or more declarations:

```c
// test.h
struct A {
   int x;
}
struct A aVar;
```

Here, we could run jextract and filter out `A`, like so:

```sh
$ jextract --include-var aVar test.h
```

However, doing so would lead to broken generated code, as the layout of the global variable
`aVar` depends on the layout of the excluded struct `A`.

In such cases, jextract will report the missing dependency and terminate without
generating any bindings:

```txt
ERROR: aVar depends on A which has been excluded
```

### Tracing

It is sometimes useful to inspect the parameters passed to a native call, especially when
diagnosing application bugs and/or crashes. The code generated by the jextract tool
supports _tracing_ of native calls, that is, parameters passed to native calls can be
printed on the standard output.

To enable the tracing support, just pass the `-Djextract.trace.downcalls=true` flag as a VM
argument to the launcher used to start the application that uses the generated bindings.
Below we show an excerpt of the output when running the [OpenGL example](samples/opengl)
with tracing support enabled:

```txt
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

### Command Line Option Reference

A complete list of all the supported command line options is given below:

| Option                                                       | Meaning                                                      |
| :----------------------------------------------------------- | ------------------------------------------------------------ |
| `-D --define-macro <macro>=<value>`                          | define `<macro>` to `<value>` (or 1 if `<value>` omitted)          |
| `--header-class-name <name>`                                 | name of the generated header class. If this option is not specified, then header class name is derived from the header file name. For example, class "foo_h" for header "foo.h". If multiple headers are specified, then this option is mandatory. |
| `-t, --target-package <package>`                             | target package name for the generated classes. If this option is not specified, then unnamed package is used.  |
| `-I, --include-dir <dir>`                                    | append directory to the include search paths. Include search paths are searched in order. For example, if `-I foo -I bar` is specified, header files will be searched in "foo" first, then (if nothing is found) in "bar".|
| `-l, --library <name \| path>`                               | specify a shared library that should be loaded by the generated header class. If <libspec> starts with `:`, then what follows is interpreted as a library path. Otherwise, `<libspec>` denotes a library name. Examples: <br>`-l GL`<br>`-l :libGL.so.1`<br>`-l :/usr/lib/libGL.so.1`|
| `--use-system-load-library`                                  | libraries specified using `-l` are loaded in the loader symbol lookup (using either `System::loadLibrary`, or `System::load`). Useful if the libraries must be loaded from one of the paths in `java.library.path`.|
| `--output <path>`                                            | specify where to place generated files                       |
| `--dump-includes <String>`                                   | dump included symbols into specified file (see below)        |
| `--include-[function,constant,struct,union,typedef,var]<String>` | Include a symbol of the given name and kind in the generated bindings. When one of these options is specified, any symbol that is not matched by any specified filters is omitted from the generated bindings. |
| `--version`                                                  | print version information and exit |
| `-F <dir>` (macOs only)                                          | specify the framework directory include files. Defaults to the current Mac OS X SDK dir.|
| `--framework <framework>` (macOs only)                           | specify the name of the library, path will be expanded to that of the framework folder.|


Jextract accepts one or more header files. When multiple header files are specified,
the `--header-class-name` option is mandatory. Header files can be specified in two different ways:

   1. Simple header file name like `foo.h` or header file path like `bar/foo.h`

   2. Special header file path or file name like `<stdio.h>`, `<GLUT/glut.h>`.
      With this syntax, the header path is considered to be relative to one of the paths
      in the C compiler include path. This simplifies the extraction of header files
      from standard include paths and include paths specified by `-I` options.

      Note that `>` and `<` are special characters in OS Shells and therefore those
      need to be escaped appropriately. On Unix platforms, simple quoting like `"<stdio.h>"`
      is enough.


#### Additional clang options

Jextract uses an embedded clang compiler (through libclang) to parse header files. Users
can also specify additional clang compiler options by creating a file named
`compile_flags.txt` in the current folder, as described
[here](https://clang.llvm.org/docs/JSONCompilationDatabase.html#alternatives).

### Other Languages

As noted in the introduction, jextract currently only supports C header files, but many
other languages also support C interop, and jextract/FFM can still be used to talk to
libraries written in those language through an intermediate C layer. The table below
describes how to do this for various different langauges:

| Language  | Method of access                                             |
| :---------| ------------------------------------------------------------ |
| C++       | C++ allows declaring C methods using `extern "C"`, and many C++ libraries have a C interface to go with them. Jextract can consume such a C interface, which can then be used to access the library in question. |
| Rust      | The Rust ecosystem has a tool called `cbindgen` which can be used to generate a C interface for a Rust library. Such a generated C interface can then be consumed by jextract, and be used to access the library in question. |

[`SymbolLookup`]: https://docs.oracle.com/en/java/javase/22/docs/api/java.base/java/lang/foreign/SymbolLookup.html
[`SymbolLookup::libraryLookup`]: https://docs.oracle.com/en/java/javase/22/docs/api/java.base/java/lang/foreign/SymbolLookup.html#libraryLookup(java.nio.file.Path,java.lang.foreign.Arena)
[`SymbolLookup::loaderLookup`]: https://docs.oracle.com/en/java/javase/22/docs/api/java.base/java/lang/foreign/SymbolLookup.html#loaderLookup()
[`Linker`]: https://docs.oracle.com/en/java/javase/22/docs/api/java.base/java/lang/foreign/Linker.html#variadic-funcs
[`Linker::downcallHandle`]: https://docs.oracle.com/en/java/javase/22/docs/api/java.base/java/lang/foreign/Linker.html#downcallHandle(java.lang.foreign.MemorySegment,java.lang.foreign.FunctionDescriptor,java.lang.foreign.Linker.Option...)
[`Linker::upcallStub`]: https://docs.oracle.com/en/java/javase/22/docs/api/java.base/java/lang/foreign/Linker.html#upcallStub(java.lang.invoke.MethodHandle,java.lang.foreign.FunctionDescriptor,java.lang.foreign.Arena,java.lang.foreign.Linker.Option...)
[`Linker::defaultLookup`]: https://docs.oracle.com/en/java/javase/22/docs/api/java.base/java/lang/foreign/Linker.html#defaultLookup()
[`MemoryLayout`]: https://docs.oracle.com/en/java/javase/22/docs/api/java.base/java/lang/foreign/MemoryLayout.html
[`SequenceLayout`]: https://docs.oracle.com/en/java/javase/22/docs/api/java.base/java/lang/foreign/SequenceLayout.html
[`MemorySegment`]: https://docs.oracle.com/en/java/javase/22/docs/api/java.base/java/lang/foreign/MemorySegment.html
[`FunctionDescriptor`]: https://docs.oracle.com/en/java/javase/22/docs/api/java.base/java/lang/foreign/FunctionDescriptor.html
[`MethodHandle`]: https://docs.oracle.com/en/java/javase/22/docs/api/java.base/java/lang/invoke/MethodHandle.html
[carrier types]: https://docs.oracle.com/en/java/javase/22/docs/api/java.base/java/lang/foreign/FunctionDescriptor.html#toMethodType()
[`System::mapLibraryName`]: https://docs.oracle.com/en/java/javase/22/docs/api/java.base/java/lang/System.html#mapLibraryName(java.lang.String)
[`System::loadLibrary`]: https://docs.oracle.com/en/java/javase/22/docs/api/java.base/java/lang/System.html#loadLibrary(java.lang.String)
[`java.library.path`]: https://docs.oracle.com/en/java/javase/22/docs/api/java.base/java/lang/System.html#java.library.path
