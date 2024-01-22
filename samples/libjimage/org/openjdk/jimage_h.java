// Generated by jextract

package org.openjdk;

import java.lang.invoke.*;
import java.lang.foreign.*;
import java.nio.ByteOrder;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import static java.lang.foreign.ValueLayout.*;
import static java.lang.foreign.MemoryLayout.PathElement.*;

public class jimage_h {

    static final SymbolLookup SYMBOL_LOOKUP;
    // manual change
    static {
        var libPath = System.getProperty("java.home");
        var OS = System.getProperty("os.name");
        if (OS.contains("Mac OS X")) {
            libPath += "/lib/libjimage.dylib";
        } else if (OS.contains("Windows")) {
            libPath = "/bin/jimage.dll";
        } else {
            libPath = "/lib/libjimage.so"; // some Unix
        }
        SymbolLookup loaderLookup = SymbolLookup.libraryLookup(libPath, Arena.global());
        SYMBOL_LOOKUP = name -> loaderLookup.find(name).or(() -> Linker.nativeLinker().defaultLookup().find(name));
    }

    jimage_h() {
        // Suppresses public default constructor, ensuring non-instantiability,
        // but allows generated subclasses in same package.
    }

    public static final ValueLayout.OfBoolean C_BOOL = ValueLayout.JAVA_BOOLEAN;
    public static final ValueLayout.OfByte C_CHAR = ValueLayout.JAVA_BYTE;
    public static final ValueLayout.OfShort C_SHORT = ValueLayout.JAVA_SHORT;
    public static final ValueLayout.OfInt C_INT = ValueLayout.JAVA_INT;
    public static final ValueLayout.OfLong C_LONG_LONG = ValueLayout.JAVA_LONG;
    public static final ValueLayout.OfFloat C_FLOAT = ValueLayout.JAVA_FLOAT;
    public static final ValueLayout.OfDouble C_DOUBLE = ValueLayout.JAVA_DOUBLE;
    public static final AddressLayout C_POINTER = ValueLayout.ADDRESS
            .withTargetLayout(MemoryLayout.sequenceLayout(java.lang.Long.MAX_VALUE, JAVA_BYTE));
    public static final ValueLayout.OfLong C_LONG = ValueLayout.JAVA_LONG;

    static final boolean TRACE_DOWNCALLS = Boolean.getBoolean("jextract.trace.downcalls");

    static void traceDowncall(String name, Object... args) {
         String traceArgs = Arrays.stream(args)
                       .map(Object::toString)
                       .collect(Collectors.joining(", "));
         System.out.printf("%s(%s)\n", name, traceArgs);
    }

    static MemorySegment findOrThrow(String symbol) {
        return SYMBOL_LOOKUP.find(symbol)
            .orElseThrow(() -> new UnsatisfiedLinkError("unresolved symbol: " + symbol));
    }

    static MemoryLayout[] inferVariadicLayouts(Object[] varargs) {
        MemoryLayout[] result = new MemoryLayout[varargs.length];
        for (int i = 0; i < varargs.length; i++) {
            result[i] = variadicLayout(varargs[i].getClass());
        }
        return result;
    }

    static MethodHandle upcallHandle(Class<?> fi, String name, FunctionDescriptor fdesc) {
        try {
            return MethodHandles.lookup().findVirtual(fi, name, fdesc.toMethodType());
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
    }

    static MethodHandle downcallHandleVariadic(String name, FunctionDescriptor baseDesc, MemoryLayout[] variadicLayouts) {
        FunctionDescriptor variadicDesc = baseDesc.appendArgumentLayouts(variadicLayouts);
        Linker.Option fva = Linker.Option.firstVariadicArg(baseDesc.argumentLayouts().size());
        return SYMBOL_LOOKUP.find(name)
                .map(addr -> Linker.nativeLinker().downcallHandle(addr, variadicDesc, fva)
                        .asSpreader(Object[].class, variadicLayouts.length))
                .orElse(null);
    }

    // Internals only below this point

    private static MemoryLayout variadicLayout(Class<?> c) {
        // apply default argument promotions per C spec
        // note that all primitives are boxed, since they are passed through an Object[]
        if (c == Boolean.class || c == Byte.class || c == Character.class || c == Short.class || c == Integer.class) {
            return JAVA_INT;
        } else if (c == Long.class) {
            return JAVA_LONG;
        } else if (c == Float.class || c == Double.class) {
            return JAVA_DOUBLE;
        } else if (MemorySegment.class.isAssignableFrom(c)) {
            return ADDRESS;
        }
        throw new IllegalArgumentException("Invalid type for ABI: " + c.getTypeName());
    }
    private static final int JIMAGE_MAX_PATH = (int)4096L;

    /**
     * {@snippet lang=c :
     * #define JIMAGE_MAX_PATH 4096
     * }
     */
    public static int JIMAGE_MAX_PATH() {
        return JIMAGE_MAX_PATH;
    }
    /**
     * {@snippet lang=c :
     * typedef long long jlong
     * }
     */
    public static final OfLong jlong = jimage_h.C_LONG_LONG;
    /**
     * {@snippet lang=c :
     * typedef int jint
     * }
     */
    public static final OfInt jint = jimage_h.C_INT;
    /**
     * {@snippet lang=c :
     * typedef jlong JImageLocationRef
     * }
     */
    public static final OfLong JImageLocationRef = jimage_h.C_LONG_LONG;

    public static MethodHandle JIMAGE_Open$MH() {
        class Holder {
            static final FunctionDescriptor DESC = FunctionDescriptor.of(
                jimage_h.C_POINTER,
                jimage_h.C_POINTER,
                jimage_h.C_POINTER
            );

            static final MethodHandle MH = Linker.nativeLinker().downcallHandle(
                    jimage_h.findOrThrow("JIMAGE_Open"),
                    DESC);
        }
        return Holder.MH;
    }

    /**
     * {@snippet lang=c :
     * JImageFile *JIMAGE_Open(const char *name, jint *error)
     * }
     */
    public static MemorySegment JIMAGE_Open(MemorySegment name, MemorySegment error) {
        var mh$ = JIMAGE_Open$MH();
        try {
            if (TRACE_DOWNCALLS) {
                traceDowncall("JIMAGE_Open", name, error);
            }
            return (MemorySegment) mh$.invokeExact(name, error);
        } catch (Throwable ex$) {
           throw new AssertionError("should not reach here", ex$);
        }
    }

    public static MethodHandle JIMAGE_Close$MH() {
        class Holder {
            static final FunctionDescriptor DESC = FunctionDescriptor.ofVoid(
                jimage_h.C_POINTER
            );

            static final MethodHandle MH = Linker.nativeLinker().downcallHandle(
                    jimage_h.findOrThrow("JIMAGE_Close"),
                    DESC);
        }
        return Holder.MH;
    }

    /**
     * {@snippet lang=c :
     * void JIMAGE_Close(JImageFile *jimage)
     * }
     */
    public static void JIMAGE_Close(MemorySegment jimage) {
        var mh$ = JIMAGE_Close$MH();
        try {
            if (TRACE_DOWNCALLS) {
                traceDowncall("JIMAGE_Close", jimage);
            }
            mh$.invokeExact(jimage);
        } catch (Throwable ex$) {
           throw new AssertionError("should not reach here", ex$);
        }
    }

    public static MethodHandle JIMAGE_PackageToModule$MH() {
        class Holder {
            static final FunctionDescriptor DESC = FunctionDescriptor.of(
                jimage_h.C_POINTER,
                jimage_h.C_POINTER,
                jimage_h.C_POINTER
            );

            static final MethodHandle MH = Linker.nativeLinker().downcallHandle(
                    jimage_h.findOrThrow("JIMAGE_PackageToModule"),
                    DESC);
        }
        return Holder.MH;
    }

    /**
     * {@snippet lang=c :
     * const char *JIMAGE_PackageToModule(JImageFile *jimage, const char *package_name)
     * }
     */
    public static MemorySegment JIMAGE_PackageToModule(MemorySegment jimage, MemorySegment package_name) {
        var mh$ = JIMAGE_PackageToModule$MH();
        try {
            if (TRACE_DOWNCALLS) {
                traceDowncall("JIMAGE_PackageToModule", jimage, package_name);
            }
            return (MemorySegment) mh$.invokeExact(jimage, package_name);
        } catch (Throwable ex$) {
           throw new AssertionError("should not reach here", ex$);
        }
    }

    public static MethodHandle JIMAGE_FindResource$MH() {
        class Holder {
            static final FunctionDescriptor DESC = FunctionDescriptor.of(
                jimage_h.C_LONG_LONG,
                jimage_h.C_POINTER,
                jimage_h.C_POINTER,
                jimage_h.C_POINTER,
                jimage_h.C_POINTER,
                jimage_h.C_POINTER
            );

            static final MethodHandle MH = Linker.nativeLinker().downcallHandle(
                    jimage_h.findOrThrow("JIMAGE_FindResource"),
                    DESC);
        }
        return Holder.MH;
    }

    /**
     * {@snippet lang=c :
     * JImageLocationRef JIMAGE_FindResource(JImageFile *jimage, const char *module_name, const char *version, const char *name, jlong *size)
     * }
     */
    public static long JIMAGE_FindResource(MemorySegment jimage, MemorySegment module_name, MemorySegment version, MemorySegment name, MemorySegment size) {
        var mh$ = JIMAGE_FindResource$MH();
        try {
            if (TRACE_DOWNCALLS) {
                traceDowncall("JIMAGE_FindResource", jimage, module_name, version, name, size);
            }
            return (long) mh$.invokeExact(jimage, module_name, version, name, size);
        } catch (Throwable ex$) {
           throw new AssertionError("should not reach here", ex$);
        }
    }

    public static MethodHandle JIMAGE_GetResource$MH() {
        class Holder {
            static final FunctionDescriptor DESC = FunctionDescriptor.of(
                jimage_h.C_LONG_LONG,
                jimage_h.C_POINTER,
                jimage_h.C_LONG_LONG,
                jimage_h.C_POINTER,
                jimage_h.C_LONG_LONG
            );

            static final MethodHandle MH = Linker.nativeLinker().downcallHandle(
                    jimage_h.findOrThrow("JIMAGE_GetResource"),
                    DESC);
        }
        return Holder.MH;
    }

    /**
     * {@snippet lang=c :
     * jlong JIMAGE_GetResource(JImageFile *jimage, JImageLocationRef location, char *buffer, jlong size)
     * }
     */
    public static long JIMAGE_GetResource(MemorySegment jimage, long location, MemorySegment buffer, long size) {
        var mh$ = JIMAGE_GetResource$MH();
        try {
            if (TRACE_DOWNCALLS) {
                traceDowncall("JIMAGE_GetResource", jimage, location, buffer, size);
            }
            return (long) mh$.invokeExact(jimage, location, buffer, size);
        } catch (Throwable ex$) {
           throw new AssertionError("should not reach here", ex$);
        }
    }

    public static MethodHandle JIMAGE_ResourceIterator$MH() {
        class Holder {
            static final FunctionDescriptor DESC = FunctionDescriptor.ofVoid(
                jimage_h.C_POINTER,
                jimage_h.C_POINTER,
                jimage_h.C_POINTER
            );

            static final MethodHandle MH = Linker.nativeLinker().downcallHandle(
                    jimage_h.findOrThrow("JIMAGE_ResourceIterator"),
                    DESC);
        }
        return Holder.MH;
    }

    /**
     * {@snippet lang=c :
     * void JIMAGE_ResourceIterator(JImageFile *jimage, JImageResourceVisitor_t visitor, void *arg)
     * }
     */
    public static void JIMAGE_ResourceIterator(MemorySegment jimage, MemorySegment visitor, MemorySegment arg) {
        var mh$ = JIMAGE_ResourceIterator$MH();
        try {
            if (TRACE_DOWNCALLS) {
                traceDowncall("JIMAGE_ResourceIterator", jimage, visitor, arg);
            }
            mh$.invokeExact(jimage, visitor, arg);
        } catch (Throwable ex$) {
           throw new AssertionError("should not reach here", ex$);
        }
    }
    private static final int JIMAGE_NOT_FOUND = (int)0L;

    /**
     * {@snippet lang=c :
     * #define JIMAGE_NOT_FOUND 0
     * }
     */
    public static int JIMAGE_NOT_FOUND() {
        return JIMAGE_NOT_FOUND;
    }
    private static final int JIMAGE_BAD_MAGIC = (int)-1L;

    /**
     * {@snippet lang=c :
     * #define JIMAGE_BAD_MAGIC -1
     * }
     */
    public static int JIMAGE_BAD_MAGIC() {
        return JIMAGE_BAD_MAGIC;
    }
    private static final int JIMAGE_BAD_VERSION = (int)-2L;

    /**
     * {@snippet lang=c :
     * #define JIMAGE_BAD_VERSION -2
     * }
     */
    public static int JIMAGE_BAD_VERSION() {
        return JIMAGE_BAD_VERSION;
    }
    private static final int JIMAGE_CORRUPTED = (int)-3L;

    /**
     * {@snippet lang=c :
     * #define JIMAGE_CORRUPTED -3
     * }
     */
    public static int JIMAGE_CORRUPTED() {
        return JIMAGE_CORRUPTED;
    }
}

