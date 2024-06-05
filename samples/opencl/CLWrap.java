/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.ArrayList;
import java.util.List;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static opencl.opencl_h_1.CL_DEVICE_TYPE_ALL;
import static opencl.opencl_h_1.CL_MEM_READ_WRITE;
import static opencl.opencl_h_1.CL_MEM_USE_HOST_PTR;
import static opencl.opencl_h_1.CL_QUEUE_PROFILING_ENABLE;
import static opencl.opencl_h_1.read;
import static opencl.opencl_h_2.C_POINTER;
import static opencl.opencl_h_2.clBuildProgram;
import static opencl.opencl_h_2.clCreateBuffer;
import static opencl.opencl_h_2.clCreateCommandQueue;
import static opencl.opencl_h_2.clCreateContext;
import static opencl.opencl_h_2.clCreateKernel;
import static opencl.opencl_h_2.clCreateProgramWithSource;
import static opencl.opencl_h_2.clEnqueueNDRangeKernel;
import static opencl.opencl_h_2.clEnqueueReadBuffer;
import static opencl.opencl_h_2.clEnqueueWriteBuffer;
import static opencl.opencl_h_2.clFlush;
import static opencl.opencl_h_2.clGetDeviceIDs;
import static opencl.opencl_h_2.clGetDeviceInfo;
import static opencl.opencl_h_2.clGetPlatformIDs;
import static opencl.opencl_h_2.clGetPlatformInfo;
import static opencl.opencl_h_2.clGetProgramBuildInfo;
import static opencl.opencl_h_2.clReleaseContext;
import static opencl.opencl_h_2.clReleaseMemObject;
import static opencl.opencl_h_2.clSetKernelArg;
import static opencl.opencl_h_2.clWaitForEvents;
import static opencl.opencl_h_2.cl_event;
import static opencl.opencl_h_2.cl_mem;
import static opencl.opencl_h_3.CL_DEVICE_BUILT_IN_KERNELS;
import static opencl.opencl_h_3.CL_DEVICE_MAX_COMPUTE_UNITS;
import static opencl.opencl_h_3.CL_DEVICE_NAME;
import static opencl.opencl_h_3.CL_FALSE;
import static opencl.opencl_h_3.CL_PLATFORM_NAME;
import static opencl.opencl_h_3.CL_PLATFORM_VENDOR;
import static opencl.opencl_h_3.CL_PLATFORM_VERSION;
import static opencl.opencl_h_3.CL_PROGRAM_BUILD_LOG;
import static opencl.opencl_h_3.CL_SUCCESS;
import static opencl.opencl_h_3.CL_TRUE;
import static opencl.opencl_h_3.C_CHAR;
import static opencl.opencl_h_3.C_FLOAT;
import static opencl.opencl_h_3.C_INT;
import static opencl.opencl_h_3.C_LONG;
//import static opengl.glut_h.C_CHAR;
//import static opengl.glut_h.C_INT;
//import static opengl.glut_h_3.C_LONG;

public class CLWrap {
    public static MemorySegment NULL = java.lang.foreign.MemorySegment.NULL;

    // https://streamhpc.com/blog/2013-04-28/opencl-error-codes/
    static class Platform {
        static class Device {
            final Platform platform;
            final MemorySegment deviceId;

            int intDeviceInfo(int query) {
                var value = 0;
                int status;
                if ((status = clGetDeviceInfo(deviceId, query, JAVA_INT.byteSize(), platform.intValuePtr, NULL)) != CL_SUCCESS()) {
                    System.out.println("Failed to get query " + query);
                } else {
                    value = platform.intValuePtr.get(C_INT, 0);
                }
                return value;
            }

            String strDeviceInfo(int query) {
                String value = null;
                int status;
                if ((status = clGetDeviceInfo(deviceId, query, 2048, platform.byte2048ValuePtr, platform.intValuePtr)) != CL_SUCCESS()) {
                    System.out.println("Failed to get query " + query);
                } else {
                    int len = platform.intValuePtr.get(C_INT, 0);
                    byte[] bytes = platform.byte2048ValuePtr.toArray(ValueLayout.JAVA_BYTE);
                    value = new String(bytes).substring(0, len - 1);
                }
                return value;
            }

            int computeUnits() {
                return intDeviceInfo(CL_DEVICE_MAX_COMPUTE_UNITS());
            }

            String deviceName() {
                return strDeviceInfo(CL_DEVICE_NAME());
            }

            String builtInKernels() {
                return strDeviceInfo(CL_DEVICE_BUILT_IN_KERNELS());
            }

            Device(Platform platform, MemorySegment deviceId) {
                this.platform = platform;
                this.deviceId = deviceId;
            }

            public static class Context {
                Device device;
                MemorySegment context;
                MemorySegment queue;

                Context(Device device, MemorySegment context) {
                    this.device = device;
                    this.context = context;
                    var statusPtr = device.platform.openCL.arena.allocate(C_INT, 1);

                    var queue_props = CL_QUEUE_PROFILING_ENABLE();
                    if ((queue = clCreateCommandQueue(context, device.deviceId, queue_props, statusPtr)) == NULL) {
                        int status = statusPtr.get(C_INT, 0);
                        clReleaseContext(context);
                        // delete[] platforms;
                        // delete[] device_ids;
                        return;
                    }

                }

                static public class Program {
                    Context context;
                    String source;
                    MemorySegment program;
                    String log;

                    Program(Context context, String source) {
                        this.context = context;
                        this.source = source;
                        var statusPtr = context.device.platform.openCL.arena.allocate(C_INT, 1);
                   //     MemorySegment sourcePtr = context.device.platform.openCL.arena.allocate(C_CHAR, source.length());
                     //   MemorySegment.copy(source.getBytes(), 0, sourcePtr, C_CHAR, 0, source.length());
                        MemorySegment sourcePtr = context.device.platform.openCL.arena.allocateFrom(source);
                        var sourcePtrPtr = context.device.platform.openCL.arena.allocate(C_POINTER, 1);
                        sourcePtrPtr.set(C_POINTER, 0, sourcePtr);
                        var sourceLenPtr = context.device.platform.openCL.arena.allocate(C_LONG, 1);
                        sourceLenPtr.set(C_LONG, 0, source.length());
                        if ((program = clCreateProgramWithSource(context.context, 1, sourcePtrPtr, sourceLenPtr, statusPtr)) == NULL) {
                            int status = statusPtr.get(C_INT, 0);
                            if (status != CL_SUCCESS()) {
                                System.out.println("failed to createProgram " + status);
                            }
                            System.out.println("failed to createProgram");
                        } else {
                            int status = statusPtr.get(C_INT, 0);
                            if (status != CL_SUCCESS()) {
                                System.out.println("failed to create program " + status);
                            }
                            var deviceIdPtr = context.device.platform.openCL.arena.allocate(C_POINTER, 1);
                            deviceIdPtr.set(C_POINTER, 0, context.device.deviceId);
                            if ((status = clBuildProgram(program, 1, deviceIdPtr, NULL, NULL, NULL)) != CL_SUCCESS()) {
                                System.out.println("failed to build" + status);
                                // dont return we may still be able to get log!
                            }

                            var logLenPtr = context.device.platform.openCL.arena.allocate(C_LONG, 1);

                            if ((status = clGetProgramBuildInfo(program, context.device.deviceId, CL_PROGRAM_BUILD_LOG(), 0, NULL, logLenPtr)) != CL_SUCCESS()) {
                                System.out.println("failed to get log build " + status);
                            } else {
                                long logLen = logLenPtr.get(C_LONG, 0);

                                var logPtr = context.device.platform.openCL.arena.allocate(C_CHAR, 1 + logLen);


                                if ((status = clGetProgramBuildInfo(program, context.device.deviceId, CL_PROGRAM_BUILD_LOG(), logLen, logPtr, logLenPtr)) != CL_SUCCESS()) {
                                    System.out.println("clGetBuildInfo (getting log) failed");

                                } else {
                                    byte[] bytes = logPtr.toArray(ValueLayout.JAVA_BYTE);
                                    log = new String(bytes).substring(0, (int) logLen);

                                }
                            }
                        }


                    }

                    public static class Kernel {
                        Program program;
                        MemorySegment kernel;
                        String name;

                        public Kernel(Program program, String name) {
                            this.program = program;
                            this.name = name;
                            var statusPtr = program.context.device.platform.openCL.arena.allocate(C_INT, 1);
                            MemorySegment kernelNamePtr = program.context.device.platform.openCL.arena.allocate(C_CHAR, name.length() + 1);
                            MemorySegment.copy(name.getBytes(), 0, kernelNamePtr, C_CHAR, 0, name.length());

                            kernel = clCreateKernel(program.program, kernelNamePtr, statusPtr);
                            int status = statusPtr.get(C_INT, 0);
                            if (status != CL_SUCCESS()) {
                                System.out.println("failed to create kernel " + status);
                            }
                        }

                        public void run(int range, Object... args) {
                            var bufPtr = program.context.device.platform.openCL.arena.allocate(cl_mem, args.length);
                            var statusPtr = program.context.device.platform.openCL.arena.allocate(C_INT, 1);
                            int status;
                            var eventMax = args.length * 4 + 1;
                            int eventc = 0;
                            var eventsPtr = program.context.device.platform.openCL.arena.allocate(cl_event, eventMax);
                            boolean block =false;// true;
                            for (int i = 0; i < args.length; i++) {
                                if (args[i] instanceof MemorySegment memorySegment) {
                                    MemorySegment clMem = clCreateBuffer(program.context.context,
                                            CL_MEM_USE_HOST_PTR() | CL_MEM_READ_WRITE(),
                                            memorySegment.byteSize(),
                                            memorySegment,
                                            statusPtr);
                                    status = statusPtr.get(C_INT, 0);
                                    if (status != CL_SUCCESS()) {
                                        System.out.println("failed to create memory buffer " + status);
                                    }
                                    bufPtr.set(cl_mem, i*cl_mem.byteSize(), clMem);
                                    status = clEnqueueWriteBuffer(program.context.queue,
                                            clMem,
                                            block?CL_TRUE():CL_FALSE(), //block?
                                            0,
                                            memorySegment.byteSize(),
                                            memorySegment,
                                            block?0:eventc,
                                            block?NULL:((eventc == 0) ? NULL : eventsPtr),
                                            block?NULL:eventsPtr.asSlice( eventc*cl_event.byteSize(), cl_event)
                                    );
                                    if (status != CL_SUCCESS()) {
                                        System.out.println("failed to enqueue write " + status);
                                    }
                                    if (!block) {
                                        eventc++;
                                    }
                                    var clMemPtr =  program.context.device.platform.openCL.arena.allocate(C_POINTER, 1);
                                    clMemPtr.set(C_POINTER, 0, clMem);

                                    status = clSetKernelArg(kernel, i, C_POINTER.byteSize(), clMemPtr);
                                    if (status != CL_SUCCESS()) {
                                        System.out.println("failed to set arg " + status);
                                    }
                                } else {
                                    bufPtr.set(cl_mem, i*cl_mem.byteSize(), NULL);
                                    if (args[i] instanceof Integer integer) {
                                        var intPtr =  program.context.device.platform.openCL.arena.allocate(C_INT, 1);
                                        intPtr.set(C_INT,0,integer.intValue());
                                        status = clSetKernelArg(kernel, i, C_INT.byteSize(), intPtr);
                                        if (status != CL_SUCCESS()) {
                                            System.out.println("failed to set arg " + status);
                                        }
                                    }else if (args[i] instanceof Float f) {
                                            var floatPtr =  program.context.device.platform.openCL.arena.allocate(C_FLOAT, 1);
                                            floatPtr.set(C_FLOAT,0,f.floatValue());
                                            status = clSetKernelArg(kernel, i, C_FLOAT.byteSize(), floatPtr);
                                            if (status != CL_SUCCESS()) {
                                                System.out.println("failed to set arg " + status);
                                            }
                                        }

                                }
                            }

                            var globalSizePtr = program.context.device.platform.openCL.arena.allocate(C_INT, 3);
                            globalSizePtr.set(C_INT, 0, range);
                            status = clEnqueueNDRangeKernel(
                                    program.context.queue,
                                    kernel,
                                    1,
                                    NULL,
                                    globalSizePtr,
                                    NULL,
                                    block?0:eventc,
                                    block?NULL:((eventc == 0) ? NULL : eventsPtr),
                                    block?NULL:eventsPtr.asSlice( eventc*cl_event.byteSize(), cl_event
                                    )
                            );
                            if (status != CL_SUCCESS()) {
                                System.out.println("failed to enqueue NDRange " + status);
                            }


                            if (block) {
                                clFlush(program.context.queue);
                            }else{
                                eventc++;

                                status = clWaitForEvents(eventc, eventsPtr);
                                if (status != CL_SUCCESS()) {
                                    System.out.println("failed to wait for ndrange events " + status);
                                }
                            }

                            for (int i = 0; i < args.length; i++) {
                                if (args[i] instanceof MemorySegment memorySegment) {
                                    MemorySegment clMem = bufPtr.get(cl_mem, (long) i*cl_mem.byteSize());

                                   status = clEnqueueReadBuffer(program.context.queue,
                                            clMem,
                                            block?CL_TRUE():CL_FALSE(),
                                            0,
                                            memorySegment.byteSize(),
                                            memorySegment,
                                            block?0:eventc,
                                            block?NULL:((eventc == 0) ? NULL : eventsPtr),
                                            block?NULL:eventsPtr.asSlice( eventc*cl_event.byteSize(), cl_event)// block?NULL:readEventPtr
                                    );
                                    if (status != CL_SUCCESS()) {
                                        System.out.println("failed to enqueue read " + status);
                                    }
                                    if (!block){
                                        eventc++;
                                    }
                                }
                            }
                            if (!block) {
                                status = clWaitForEvents(eventc, eventsPtr);
                                if (status != CL_SUCCESS()) {
                                    System.out.println("failed to wait for events " + status);
                                }
                            }
                            for (int i = 0; i < args.length; i++) {
                                if (args[i] instanceof MemorySegment memorySegment) {
                                    MemorySegment clMem = bufPtr.get(cl_mem, (long) i*cl_mem.byteSize());
                                    status = clReleaseMemObject(clMem);
                                    if (status != CL_SUCCESS()) {
                                        System.out.println("failed to release memObject " + status);
                                    }
                                }
                            }
                        }
                    }

                    public Kernel getKernel(String kernelName) {
                        return new Kernel(this, kernelName);
                    }
                }

                public Program buildProgram(String source) {
                    var program = new Program(this, source);
                    return program;
                }
            }

            public Context createContext() {

                var statusPtr = platform.openCL.arena.allocate(C_INT, 1);
                MemorySegment context;
                var deviceIds = platform.openCL.arena.allocate(C_POINTER, 1);
                deviceIds.set(C_POINTER, 0, this.deviceId);
                if ((context = clCreateContext(NULL, 1, deviceIds, NULL, NULL, statusPtr)) == NULL) {
                    int status = statusPtr.get(C_INT, 0);
                    System.out.println("Failed to get context  ");
                    return null;
                } else {
                    int status = statusPtr.get(C_INT, 0);
                    if (status != CL_SUCCESS()) {
                        System.out.println("failed to get context  " + status);
                    }
                    return new Context(this, context);
                }
            }
        }

        int intPlatformInfo(int query) {
            var value = 0;
            int status;
            if ((status = clGetPlatformInfo(platformId, query, JAVA_INT.byteSize(), intValuePtr, NULL)) != CL_SUCCESS()) {
                System.out.println("Failed to get query " + query);
            } else {
                value = intValuePtr.get(C_INT, 0);
            }
            return value;
        }

        String strPlatformInfo(int query) {
            String value = null;
            int status;
            if ((status = clGetPlatformInfo(platformId, query, 2048, byte2048ValuePtr, intValuePtr)) != CL_SUCCESS()) {
                System.err.println("Failed to get query " + query);
            } else {
                int len = intValuePtr.get(C_INT, 0);
                byte[] bytes = byte2048ValuePtr.toArray(ValueLayout.JAVA_BYTE);
                value = new String(bytes).substring(0, len - 1);
            }
            return value;
        }

        CLWrap openCL;
        MemorySegment platformId;
        List<Device> devices = new ArrayList<>();
        final MemorySegment intValuePtr;
        final MemorySegment byte2048ValuePtr;

        String platformName() {
            return strPlatformInfo(CL_PLATFORM_NAME());
        }

        String vendorName() {
            return strPlatformInfo(CL_PLATFORM_VENDOR());
        }

        String version() {
            return strPlatformInfo(CL_PLATFORM_VERSION());
        }

        public Platform(CLWrap openCL, MemorySegment platformId) {
            this.openCL = openCL;
            this.platformId = platformId;
            this.intValuePtr = openCL.arena.allocate(C_INT, 1);
            this.byte2048ValuePtr = openCL.arena.allocate(C_CHAR, 2048);
            var devicecPtr = openCL.arena.allocate(C_INT, 1);
            int status;
            if ((status = clGetDeviceIDs(platformId, CL_DEVICE_TYPE_ALL(), 0, NULL, devicecPtr)) != CL_SUCCESS()) {
                System.err.println("Failed getting devicec for platform 0 ");
            } else {
                int devicec = devicecPtr.get(C_INT, 0);
                //  System.out.println("platform 0 has " + devicec + " device" + ((devicec > 1) ? "s" : ""));
                var deviceIdsPtr = openCL.arena.allocate(C_POINTER, devicec);
                if ((status = clGetDeviceIDs(platformId, CL_DEVICE_TYPE_ALL(), devicec, deviceIdsPtr, devicecPtr)) != CL_SUCCESS()) {
                    System.err.println("Failed getting deviceids  for platform 0 ");
                } else {
                    // System.out.println("We have "+devicec+" device ids");
                    for (int i = 0; i < devicec; i++) {
                        devices.add(new Device(this, deviceIdsPtr.get(C_POINTER, i*C_POINTER.byteSize())));
                    }
                }
            }
        }
    }

    List<Platform> platforms = new ArrayList<>();

    Arena arena;

    CLWrap(Arena arena) {
        this.arena = arena;
        int status;
        var platformcPtr = arena.allocate(C_INT, 1);

        if ((status = clGetPlatformIDs(0, NULL, platformcPtr)) != CL_SUCCESS()) {
            System.out.println("Failed to get opencl platforms");
        } else {
            int platformc = platformcPtr.get(JAVA_INT, 0);
            // System.out.println("There are "+platformc+" platforms");
            var platformIdsPtr = arena.allocate(C_POINTER, platformc);
            if ((status = clGetPlatformIDs(platformc, platformIdsPtr, platformcPtr)) != CL_SUCCESS()) {
                System.out.println("Failed getting ids");
            } else {
                for (int i = 0; i < platformc; i++) {
                    // System.out.println("We should have the ids");
                    platforms.add(new Platform(this, platformIdsPtr.get(C_POINTER, i)));
                }
            }
        }
    }


    public static void main(String[] args) throws IOException {
        try (var arena = Arena.ofConfined()) {
            CLWrap openCL = new CLWrap(arena);

            CLWrap.Platform.Device[] selectedDevice = new CLWrap.Platform.Device[1];
            openCL.platforms.forEach(platform -> {
                System.out.println("Platform Name " + platform.platformName());
                platform.devices.forEach(device -> {
                    System.out.println("   Compute Units     " + device.computeUnits());
                    System.out.println("   Device Name       " + device.deviceName());
                    System.out.println("   Built In Kernels  " + device.builtInKernels());
                    selectedDevice[0] = device;
                });
            });
            var context = selectedDevice[0].createContext();
            var program = context.buildProgram("""
                    __kernel void squares(__global int* in,__global int* out ){
                        int gid = get_global_id(0);
                        out[gid] = in[gid]*in[gid];
                    }
                    """);
            var kernel = program.getKernel("squares");
            var in = arena.allocate(C_INT, 512);
            var out = arena.allocate(C_INT, 512);
            for (int i = 0; i < 512; i++) {
                in.set(C_INT, (int)i*C_INT.byteSize(),i);
            }
            kernel.run(512, in, out );
            for (int i = 0; i < 512; i++) {
                System.out.println(i + " " + out.get(C_INT, (int)i*C_INT.byteSize()));
            }
        }
    }
}
