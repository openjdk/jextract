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

import opengl.glutDisplayFunc$func;
import opengl.glutIdleFunc$func;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Arrays;

import static java.lang.foreign.ValueLayout.JAVA_INT;
import static opengl.glut_h.C_CHAR;
import static opengl.glut_h.C_FLOAT;
import static opengl.glut_h.C_INT;
import static opengl.glut_h.GLUT_DEPTH;
import static opengl.glut_h.GLUT_DOUBLE;
import static opengl.glut_h.GLUT_RGB;
import static opengl.glut_h.GL_AMBIENT;
import static opengl.glut_h.GL_COLOR_BUFFER_BIT;
import static opengl.glut_h.GL_COLOR_MATERIAL;
import static opengl.glut_h.GL_DEPTH_BUFFER_BIT;
import static opengl.glut_h.GL_DEPTH_TEST;
import static opengl.glut_h.GL_DIFFUSE;
import static opengl.glut_h.GL_FRONT;
import static opengl.glut_h.GL_LIGHT0;
import static opengl.glut_h.GL_LIGHTING;
import static opengl.glut_h.GL_LINEAR;
import static opengl.glut_h.GL_NEAREST;
import static opengl.glut_h.GL_ONE;
import static opengl.glut_h.GL_POSITION;
import static opengl.glut_h.GL_QUADS;
import static opengl.glut_h.GL_SHININESS;
import static opengl.glut_h.GL_SMOOTH;
import static opengl.glut_h.GL_SPECULAR;
import static opengl.glut_h.GL_SRC_ALPHA;
import static opengl.glut_h.GL_TEXTURE_2D;
import static opengl.glut_h.GL_TEXTURE_MAG_FILTER;
import static opengl.glut_h.GL_TEXTURE_MIN_FILTER;
import static opengl.glut_h.GL_UNSIGNED_BYTE;
import static opengl.glut_h.glActiveTexture;
import static opengl.glut_h.glBegin;
import static opengl.glut_h.glBindTexture;
import static opengl.glut_h.glBlendFunc;
import static opengl.glut_h.glClear;
import static opengl.glut_h.glClearColor;
import static opengl.glut_h.glColor3f;
import static opengl.glut_h.glDisable;
import static opengl.glut_h.glEnable;
import static opengl.glut_h.glEnd;
import static opengl.glut_h.glGenTextures;
import static opengl.glut_h.glLightfv;
import static opengl.glut_h.glLoadIdentity;
import static opengl.glut_h.glMaterialfv;
import static opengl.glut_h.glPopMatrix;
import static opengl.glut_h.glPushMatrix;
import static opengl.glut_h.glRotatef;
import static opengl.glut_h.glScalef;
import static opengl.glut_h.glShadeModel;
import static opengl.glut_h.glTexCoord2f;
import static opengl.glut_h.glTexImage2D;
import static opengl.glut_h.glTexParameteri;
import static opengl.glut_h.glVertex3f;
import static opengl.glut_h.glutCreateWindow;
import static opengl.glut_h.glutDisplayFunc;
import static opengl.glut_h.glutIdleFunc;
import static opengl.glut_h.glutInit;
import static opengl.glut_h.glutInitDisplayMode;
import static opengl.glut_h.glutInitWindowSize;
import static opengl.glut_h.glutMainLoop;
import static opengl.glut_h.glutPostRedisplay;
import static opengl.glut_h.glutSolidTeapot;
import static opengl.glut_h.glutSwapBuffers;
import static opengl.glut_h_2.GL_BLEND;
import static opengl.glut_h_2.GL_RGBA;

public class GLWrap {
    public static class GLTexture {
        final Arena arena;
        final MemorySegment data;
        final int width;
        final int height;
        int idx;
        GLTexture(Arena arena, InputStream textureStream) {
            this.arena = arena;
            BufferedImage img = null;
            try {
                img = ImageIO.read(textureStream);
                this.width = img.getWidth();
                this.height = img.getHeight();
                BufferedImage image = new BufferedImage(width,height, BufferedImage.TYPE_4BYTE_ABGR_PRE);
                image.getGraphics().drawImage(img, 0, 0, null);
                var raster = image.getRaster();
                var dataBuffer = raster.getDataBuffer();
                data = arena.allocateFrom(C_CHAR, ((DataBufferByte) dataBuffer).getData());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class GLWindow {
        Arena arena;
        int width;
        int height;
        String name;
        GLTexture[] textures;
        MemorySegment textureBuf;
        GLWindow(Arena arena, int width, int height, String name, GLTexture... textures) {
            this.arena = arena;
            this.width = width;
            this.height = height;
            this.name = name;
            this.textures = textures;
            var argc = arena.allocateFrom(C_INT, 0);
            glutInit(argc, argc);
            glutInitDisplayMode(GLUT_DOUBLE() | GLUT_RGB() | GLUT_DEPTH());
            glutInitWindowSize(width, height);
            glutCreateWindow(arena.allocateFrom("NBODY!"));

            glClearColor(0f, 0f, 0f, 0f);
            // Setup Lighting see  https://www.khronos.org/opengl/wiki/How_lighting_works
            glShadeModel(GL_SMOOTH());
            glEnable(GL_BLEND());
            glBlendFunc(GL_SRC_ALPHA(), GL_ONE());
            glEnable(GL_TEXTURE_2D());
            textureBuf = arena.allocate(C_INT, textures.length*C_INT.byteSize());
            glGenTextures(textures.length, textureBuf);
            int[] count = {0};
            Arrays.stream(textures).forEach(texture -> {
                texture.idx=count[0]++;
                glBindTexture(GL_TEXTURE_2D(), textureBuf.get(JAVA_INT, texture.idx * JAVA_INT.byteSize()));
                glTexImage2D(GL_TEXTURE_2D(), 0, GL_RGBA(), texture.width,
                        texture.height, 0, GL_RGBA(), GL_UNSIGNED_BYTE(), texture.data);
                glTexParameteri(GL_TEXTURE_2D(), GL_TEXTURE_MAG_FILTER(), GL_LINEAR());
                glTexParameteri(GL_TEXTURE_2D(), GL_TEXTURE_MIN_FILTER(), GL_NEAREST());
            });
            var useLighting = false;
            if (useLighting) {
                glEnable(GL_LIGHTING());

                var light = GL_LIGHT0(); // .... GL_LIGHT_0 .. -> 7

                var pos = arena.allocateFrom(C_FLOAT, new float[]{0.0f, 15.0f, -15.0f, 0});

                glLightfv(light, GL_POSITION(), pos);

                var red_ambient_light = arena.allocateFrom(C_FLOAT, new float[]{1f, 0.0f, 0.0f, 0.0f});

                var grey_diffuse_light = arena.allocateFrom(C_FLOAT, new float[]{1f, 1f, 1f, 0.0f});

                var yellow_specular_light = arena.allocateFrom(C_FLOAT, new float[]{1.0f, 1.0f, 0.0f, 0.0f});
                glLightfv(light, GL_AMBIENT(), red_ambient_light);
                glLightfv(light, GL_DIFFUSE(), grey_diffuse_light);
                glLightfv(light, GL_SPECULAR(), yellow_specular_light);

                var shini = arena.allocate(C_FLOAT, 113);
                glMaterialfv(GL_FRONT(), GL_SHININESS(), shini);

                var useColorMaterials = false;
                if (useColorMaterials) {
                    glEnable(GL_COLOR_MATERIAL());
                } else {
                    glDisable(GL_COLOR_MATERIAL());
                }
                glEnable(light);
                glEnable(GL_DEPTH_TEST());
            } else {
                glDisable(GL_LIGHTING());
            }
            glutDisplayFunc(glutDisplayFunc$func.allocate(this::display, arena));
            glutIdleFunc(glutIdleFunc$func.allocate(this::onIdle, arena));
        }
        void display() {
            glClear(GL_COLOR_BUFFER_BIT() | GL_DEPTH_BUFFER_BIT());
            glPushMatrix();
            glLoadIdentity();
            glRotatef(0f, 0f,0f, 0f);
            //glRotatef(rot, 0f, 1f, 0f);
            //   glTranslatef(0f, 0f, trans);
            glScalef(.1f, .1f, 1);

            glActiveTexture(textureBuf.get(ValueLayout.JAVA_INT, 0));
            glBindTexture(GL_TEXTURE_2D(), textureBuf.get(ValueLayout.JAVA_INT, 0));
            glColor3f(1f, 1f, 1f);
            glBegin(GL_QUADS());
            {
                float dx = -.5f;
                float dy = -.5f;
                float dz = -.5f;
                float x = 0f;
                float y= 0f;
                float z = 0f;
                    glTexCoord2f(0, 1);
                    glVertex3f(x + dx, y + dy + 1, z + dz);
                    glTexCoord2f(0, 0);
                    glVertex3f(x + dx, y + dy, z + dz);
                    glTexCoord2f(1, 0);
                    glVertex3f(x + dx + 1, y + dy, z + dz);
                    glTexCoord2f(1, 1);
                    glVertex3f(x + dx + 1, y + dy + 1, z + dz);
            }
            glEnd();
            glColor3f(0.8f, 0.1f, 0.1f);
            glutSolidTeapot(1d);
            glPopMatrix();
            glutSwapBuffers();
        }

        void onIdle() {
            glutPostRedisplay();
        }

        public void mainLoop() {
            glutMainLoop();
        }
    }


    public void main(String[] args) throws IOException {
        try (var arena = Arena.ofConfined()) {
            new GLWindow(arena, 800,800,"name",
                    new GLTexture(arena, GLWrap.class.getResourceAsStream("/particle.png"))
            ).mainLoop();
        }
    }
}

