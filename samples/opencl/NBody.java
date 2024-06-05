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
import java.util.stream.IntStream;

import static java.lang.foreign.ValueLayout.JAVA_FLOAT;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static opengl.glut_h.GL_COLOR_BUFFER_BIT;
import static opengl.glut_h.GL_DEPTH_BUFFER_BIT;
import static opengl.glut_h.GL_QUADS;
import static opengl.glut_h.GL_TEXTURE_2D;
import static opengl.glut_h.glBegin;
import static opengl.glut_h.glBindTexture;
import static opengl.glut_h.glClear;
import static opengl.glut_h.glColor3f;
import static opengl.glut_h.glEnd;
import static opengl.glut_h.glLoadIdentity;
import static opengl.glut_h.glPopMatrix;
import static opengl.glut_h.glPushMatrix;
import static opengl.glut_h.glRotatef;
import static opengl.glut_h.glScalef;
import static opengl.glut_h.glTexCoord2f;
import static opengl.glut_h.glVertex3f;
import static opengl.glut_h.glutSwapBuffers;
import static opengl.glut_h_3.C_FLOAT;

public class NBody extends GLWrap.GLWindow {

    protected final float delT = .1f;

    protected final float espSqr = 0.1f;

    protected final float mass = .5f;


    private static int STRIDE = 4;
    private static int Xidx = 0;
    private static int Yidx = 1;
    private static int Zidx = 2;

    final float[] xyzPos;
    final float[] xyzVel;

    final GLWrap.GLTexture particle;
    final MemorySegment xyzPosSeg;
    final MemorySegment xyzVelSeg;

    final CLWrap.Platform.Device.Context.Program.Kernel kernel;

    int count;
    int frames = 0;
    long startTime = 0l;

    public enum Mode {
        OpenCL("""
                    __kernel void nbody( __global float *xyzPos ,__global float* xyzVel, float mass, float delT, float espSqr ){
                        int body = get_global_id(0);
                        int STRIDE=4;
                        int Xidx=0;
                        int Yidx=1;
                        int Zidx=2;
                        int bodyStride = body*STRIDE;
                        int bodyStrideX = bodyStride+Xidx;
                        int bodyStrideY = bodyStride+Yidx;
                        int bodyStrideZ = bodyStride+Zidx;
                        
                        float accx = 0.0;
                        float accy = 0.0;
                        float accz = 0.0;
                                
                        float myPosx = xyzPos[bodyStrideX];
                        float myPosy = xyzPos[bodyStrideY];
                        float myPosz = xyzPos[bodyStrideZ];
                        for (int i = 0; i < get_global_size(0); i++) {
                            int iStride = i*STRIDE;
                            int iStrideX = iStride+Xidx;
                            int iStrideY = iStride+Yidx;
                            int iStrideZ = iStride+Zidx;
                            float dx = xyzPos[iStrideX] - myPosx;
                            float dy = xyzPos[iStrideY] - myPosy;
                            float dz = xyzPos[iStrideZ] - myPosz;
                            float invDist =  (float) 1.0/sqrt((float)((dx * dx) + (dy * dy) + (dz * dz) + espSqr));
                            float s = mass * invDist * invDist * invDist;
                            accx = accx + (s * dx);
                            accy = accy + (s * dy);
                            accz = accz + (s * dz);
                        }
                        accx = accx * delT;
                        accy = accy * delT;
                        accz = accz * delT;
                        xyzPos[bodyStrideX] = myPosx + (xyzVel[bodyStrideX] * delT) + (accx * 0.5 * delT);
                        xyzPos[bodyStrideY] = myPosy + (xyzVel[bodyStrideY] + delT) + (accy * 0.5 * delT);
                        xyzPos[bodyStrideZ] = myPosz + (xyzVel[bodyStrideZ] + delT) + (accz * 0.5 * delT);
                     
                        xyzVel[bodyStrideX] = xyzVel[bodyStrideX] + accx;
                        xyzVel[bodyStrideY] = xyzVel[bodyStrideY] + accy;
                        xyzVel[bodyStrideZ] = xyzVel[bodyStrideZ] + accz;
                        
                    }
                    """),
        OpenCL4("""
                    __kernel void nbody( __global float4 *xyzPos ,__global float4* xyzVel, float mass, float delT, float espSqr ){
                        float4 acc = (0.0,0.0,0.0,0.0);
                        float4 myPos = xyzPos[get_global_id(0)];
                        float4 myVel = xyzVel[get_global_id(0)];
                        for (int i = 0; i < get_global_size(0); i++) {
                               float4 delta =  xyzPos[i] - myPos;
                               float invDist =  (float) 1.0/sqrt((float)((delta.x * delta.x) + (delta.y * delta.y) + (delta.z * delta.z) + espSqr));
                               float s = mass * invDist * invDist * invDist;
                               acc= acc + (s * delta);
                        }
                        acc = acc*delT;
                        myPos = myPos + (myVel * delT) + (acc * delT)/2;
                        myVel = myVel + acc;
                        xyzPos[get_global_id(0)] = myPos;
                        xyzVel[get_global_id(0)] = myVel;
                  
                    }
                    """),
        JavaSeq(false),
        JavaMT(true);
        final public String code;
        final public boolean isOpenCL;
        final public boolean isJava;
        final public boolean isMultiThreaded;
        Mode(String code){
            this.code = code;
            this.isOpenCL = true;
            this.isJava = false;
            this.isMultiThreaded =false;
        }
        Mode(boolean isMultiThreaded){
            this.code = null;
            this.isOpenCL = false;
            this.isJava = true;
            this.isMultiThreaded = isMultiThreaded;
        }
    }

    ;
    final Mode mode;

    public NBody(Arena arena, int width, int height, GLWrap.GLTexture particle, int count, Mode mode) {
        super(arena, width, height, "nbody", particle);
        this.particle = particle;
        this.count = count;
        this.xyzPos = new float[count * STRIDE];
        this.xyzVel = new float[count * STRIDE];
        this.mode = mode;
        final float maxDist = 80f;

        System.out.println(count + " particles");

        for (int body = 0; body < count; body++) {
            final float theta = (float) (Math.random() * Math.PI * 2);
            final float phi = (float) (Math.random() * Math.PI * 2);
            final float radius = (float) (Math.random() * maxDist);

            // get random 3D coordinates in sphere
            xyzPos[(body * STRIDE) + Xidx] = (float) (radius * Math.cos(theta) * Math.sin(phi));
            xyzPos[(body * STRIDE) + Yidx] = (float) (radius * Math.sin(theta) * Math.sin(phi));
            xyzPos[(body * STRIDE) + Zidx] = (float) (radius * Math.cos(phi));
        }
        if (mode.isOpenCL) {
            xyzPosSeg = arena.allocateFrom(JAVA_FLOAT, xyzPos);
            xyzVelSeg = arena.allocateFrom(JAVA_FLOAT, xyzVel);
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
            var program = context.buildProgram(mode.code);
            kernel = program.getKernel("nbody");
        }else{
            kernel = null;
            xyzPosSeg=null;
            xyzVelSeg=null;
        }
    }


    float rot = 0f;

    public static void run(int body, int size, float[] xyzPos, float[] xyzVel, float mass, float delT, float espSqr) {
        float accx = 0.f;
        float accy = 0.f;
        float accz = 0.f;
        int bodyStride = body*STRIDE;
        int bodyStrideX = bodyStride+Xidx;
        int bodyStrideY = bodyStride+Yidx;
        int bodyStrideZ = bodyStride+Zidx;

        final float myPosx = xyzPos[bodyStrideX];
        final float myPosy = xyzPos[bodyStrideY];
        final float myPosz = xyzPos[bodyStrideZ];

        for (int i = 0; i < size; i++) {
            int iStride = i*STRIDE;
            int iStrideX = iStride+Xidx;
            int iStrideY = iStride+Yidx;
            int iStrideZ = iStride+Zidx;
            final float dx = xyzPos[iStrideX] - myPosx;
            final float dy = xyzPos[iStrideY] - myPosy;
            final float dz = xyzPos[iStrideZ] - myPosz;
            final float invDist = 1 / (float) Math.sqrt((dx * dx) + (dy * dy) + (dz * dz) + espSqr);
            final float s = mass * invDist * invDist * invDist;
            accx = accx + (s * dx);
            accy = accy + (s * dy);
            accz = accz + (s * dz);
        }
        accx = accx * delT;
        accy = accy * delT;
        accz = accz * delT;
        xyzPos[bodyStrideX] = myPosx + (xyzVel[bodyStrideX] * delT) + (accx * .5f * delT);
        xyzPos[bodyStrideY] = myPosy + (xyzVel[bodyStrideY] + delT) + (accy * .5f * delT);
        xyzPos[bodyStrideZ] = myPosz + (xyzVel[bodyStrideZ] + delT) + (accz * .5f * delT);

        xyzVel[bodyStrideZ] = xyzVel[bodyStrideX] + accx;
        xyzVel[bodyStrideY] = xyzVel[bodyStrideY] + accy;
        xyzVel[bodyStrideZ] = xyzVel[bodyStrideZ] + accz;
    }

    void display() {
        if (startTime == 0) {
            startTime = System.currentTimeMillis();
        }
        glClear(GL_COLOR_BUFFER_BIT() | GL_DEPTH_BUFFER_BIT());
        glPushMatrix();
        glLoadIdentity();
        glRotatef(-rot / 2f, 0f, 0f, 1f);
        //glRotatef(rot, 0f, 1f, 0f);
        //   glTranslatef(0f, 0f, trans);
        glScalef(.01f, .01f, .01f);
        glColor3f(1f, 1f, 1f);

        if (mode.isJava){
            if (mode.isMultiThreaded) {
                IntStream.range(0, count).parallel().forEach(
                        i -> run(i, count, xyzPos, xyzVel, mass, delT, espSqr)
                );
            }else {
                IntStream.range(0, count).forEach(
                        i -> run(i, count, xyzPos, xyzVel, mass, delT, espSqr)
                );
            }
        } else {
            kernel.run(count,  xyzPosSeg, xyzVelSeg, mass, delT, espSqr);
        }
        glBegin(GL_QUADS());
        {
            glBindTexture(GL_TEXTURE_2D(), textureBuf.get(JAVA_INT, particle.idx * JAVA_INT.byteSize()));
            float dx = -.5f;
            float dy = -.5f;
            float dz = -.5f;
            for (int i = 0; i < count; i++) {
                float x = mode.isOpenCL ? xyzPosSeg.get(C_FLOAT, (i * STRIDE * C_FLOAT.byteSize()) + (Xidx * C_FLOAT.byteSize())) : xyzPos[(i * STRIDE) + Xidx];
                float y = mode.isOpenCL ? xyzPosSeg.get(C_FLOAT, (i * STRIDE * C_FLOAT.byteSize()) + (Yidx * C_FLOAT.byteSize())) : xyzPos[(i * STRIDE) + Yidx];
                float z = mode.isOpenCL ? xyzPosSeg.get(C_FLOAT, (i * STRIDE * C_FLOAT.byteSize()) + (Zidx * C_FLOAT.byteSize())) : xyzPos[(i * STRIDE) + Zidx];
                final int LEFT = 0;
                final int RIGHT = 1;
                final int TOP = 0;
                final int BOTTOM = 1;
                glTexCoord2f(LEFT, BOTTOM);
                glVertex3f(x + dx + LEFT, y + dy + BOTTOM, z + dz);
                glTexCoord2f(LEFT, TOP);
                glVertex3f(x + dx + LEFT, y + dy + TOP, z + dz);
                glTexCoord2f(RIGHT, TOP);
                glVertex3f(x + dx + RIGHT, y + dy + TOP, z + dz);
                glTexCoord2f(RIGHT, BOTTOM);
                glVertex3f(x + dx + RIGHT, y + dy + BOTTOM, z + dz);
            }
        }
        glEnd();
        glColor3f(0.8f, 0.1f, 0.1f);
        glPopMatrix();
        glutSwapBuffers();
        frames++;
        long elapsed = System.currentTimeMillis()-startTime;
        if (elapsed >200 || (frames % 100) == 0) {
            float secs = elapsed / 1000f;
            System.out.println((frames / secs) + "fps");
        }
    }

    void onIdle() {
        rot += 1f;
        super.onIdle();
    }
}

public void main(String[] args) throws IOException {
    int particleCount = args.length>2?Integer.parseInt(args[2]):32768;
    NBody.Mode mode = args.length>3?switch(args[3]){
        case "OpenCL" -> NBody.Mode.OpenCL;
        case "JavaSeq" -> NBody.Mode.JavaSeq;
        case "JavaMT" -> NBody.Mode.JavaMT;
        case "OpenCL4" -> NBody.Mode.JavaMT;
        default -> throw new IllegalStateException("No mode "+args[3]);
    }:NBody.Mode.OpenCL;
    System.out.println("mode"+mode);
    try (var arena = Arena.ofConfined()) {
        var particleTexture = new GLWrap.GLTexture(arena, NBody.class.getResourceAsStream("/particle.png"));
        new NBody(arena, 1000, 1000, particleTexture, particleCount, mode).mainLoop();
    }
}

