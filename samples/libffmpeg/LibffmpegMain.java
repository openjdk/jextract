/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.lang.foreign.*;
import libffmpeg.AVCodecContext;
import libffmpeg.AVFormatContext;
import libffmpeg.AVFrame;
import libffmpeg.AVPacket;
import libffmpeg.AVStream;
import static libffmpeg.Libffmpeg.*;
import static java.lang.foreign.MemoryAddress.NULL;

/*
 * This sample is based on C sample from the ffmpeg tutorial at
 * http://dranger.com/ffmpeg/tutorial01.html
 *
 * This sample extracts first five frames of the video stream
 * from a given .mp4 file and stores those as .ppm image files.
*/
public class LibffmpegMain {
    private static int NUM_FRAMES_TO_CAPTURE = 5;

    static class ExitException extends RuntimeException {
        final int exitCode;
        ExitException(int exitCode, String msg) {
            super(msg);
            this.exitCode = exitCode;
        }
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("please pass a .mp4 file");
            System.exit(1);
        }

        av_register_all();

        int exitCode = 0;
        var pCodecCtxOrig = NULL;
        var pCodecCtx = NULL;
        var pFrame = NULL;
        var pFrameRGB = NULL;
        var buffer = NULL;

        try (var session = MemorySession.openConfined()) {
            // AVFormatContext *ppFormatCtx;
            var ppFormatCtx = MemorySegment.allocateNative(C_POINTER, session);
            // char* fileName;
            var fileName = session.allocateUtf8String(args[0]);

            // open video file
            if (avformat_open_input(ppFormatCtx, fileName, NULL, NULL) != 0) {
                throw new ExitException(1, "Cannot open " + args[0]);
            }
            System.out.println("opened " + args[0]);
            // AVFormatContext *pFormatCtx;
            var pFormatCtx = ppFormatCtx.get(C_POINTER, 0);

            // Retrieve stream info
            if (avformat_find_stream_info(pFormatCtx, NULL) < 0) {
                throw new ExitException(1, "Could not find stream information");
            }

            session.addCloseAction(()-> {
                // Close the video file
                avformat_close_input(ppFormatCtx);
            });

            // Dump AV format info on stderr
            av_dump_format(pFormatCtx, 0, fileName, 0);

            // Find the first video stream
            int videoStream = -1;
            // formatCtx.nb_streams
            int nb_streams = AVFormatContext.nb_streams$get(pFormatCtx);
            System.out.println("number of streams: " + nb_streams);
            // formatCtx.streams
            var pStreams = AVFormatContext.streams$get(formatCtx);

            // AVCodecContext* pVideoCodecCtx;
            var pVideoCodecCtx = NULL;
            // AVCodec* pCodec;
            var pCodec = NULL;
            for (int i = 0; i < nb_streams; i++) {
                // AVStream* pStream;
                var pStream = pStreams.getAtIndex(C_POINTER, i);
                // AVCodecContext* pCodecCtx;
                pCodecCtx = AVStream.codec$get(pStream);
                if (AVCodecContext.codec_type$get(pCodecCtx) == AVMEDIA_TYPE_VIDEO()) {
                    videoStream = i;
                    pVideoCodecCtx = pCodecCtx;
                    // Find the decoder for the video stream
                    pCodec = avcodec_find_decoder(AVCodecContext.codec_id$get(avcodecCtx));
                    break;
                }
            }

            if (videoStream == -1) {
                throw new ExitException(1, "Didn't find a video stream");
            } else {
                System.out.println("Found video stream (index: " + videoStream + ")");
            }

            if (pCodec.equals(NULL)) {
                throw new ExitException(1, "Unsupported codec");
            }

            // Copy context
            // AVCodecContext *pCodecCtxOrig;
            pCodecCtxOrig = pVideoCodecCtx;
            // AVCodecContext *pCodecCtx;
            pCodecCtx = avcodec_alloc_context3(pCodec);
            if (avcodec_copy_context(pCodecCtx, pCodecCtxOrig) != 0) {
                throw new ExitException(1, "Cannot copy context");
            }

            // Open codec
            if (avcodec_open2(pCodecCtx, pCodec, NULL) < 0) {
                throw new ExitException(1, "Cannot open codec");
            }

            // Allocate video frame
            // AVFrame* pFrame;
            pFrame = av_frame_alloc();
            // Allocate an AVFrame structure
            // AVFrame* pFrameRGB;
            pFrameRGB = av_frame_alloc();

            // Determine required buffer size and allocate buffer
            int width = AVCodecContext.width$get(pCodecCtx);
            int height = AVCodecContext.height$get(pCodecCtx);
            int numBytes = avpicture_get_size(AV_PIX_FMT_RGB24(), width, height);
            buffer = av_malloc(numBytes * C_CHAR.byteSize());


            if (pFrame.equals(NULL)) {
                throw new ExitException(1, "Cannot allocate frame");
            }
            if (pFrameRGB.equals(NULL)) {
                throw new ExitException(1, "Cannot allocate RGB frame");
            }
            if (buffer.equals(NULL)) {
                throw new ExitException(1, "cannot allocate buffer");
            }

            // Assign appropriate parts of buffer to image planes in pFrameRGB
            // Note that pFrameRGB is an AVFrame, but AVFrame is a superset
            // of AVPicture
            avpicture_fill(pFrameRGB, buffer, AV_PIX_FMT_RGB24(), width, height);

            // initialize SWS context for software scaling
            int pix_fmt = AVCodecContext.pix_fmt$get(codecCtx);
            var sws_ctx = sws_getContext(width, height, pix_fmt, width, height,
                AV_PIX_FMT_RGB24(), SWS_BILINEAR(), NULL, NULL, NULL);

            int i = 0;
            // ACPacket packet;
            var packet = AVPacket.allocate(session);
            // int* pFrameFinished;
            var pFrameFinished = MemorySegment.allocateNative(C_INT, session);

            while (av_read_frame(pFormatCtx, packet) >= 0) {
                // Is this a packet from the video stream?
                // packet.stream_index == videoStream
                if (AVPacket.stream_index$get(packet) == videoStream) {
                    // Decode video frame
                    avcodec_decode_video2(pCodecCtx, pFrame, pFrameFinished, packet);

                    int frameFinished = pFrameFinished.get(C_INT, 0);
                    // Did we get a video frame?
                    if (frameFinished != 0) {
                        // Convert the image from its native format to RGB
                        sws_scale(sws_ctx, AVFrame.data$slice(pFrame),
                            AVFrame.linesize$slice(pFrame), 0, height,
                            AVFrame.data$slice(pFrameRGB), AVFrame.linesize$slice(pFrameRGB));

                        // Save the frame to disk
                        if (++i <= NUM_FRAMES_TO_CAPTURE) {
                            try {
                                saveFrame(pFrameRGB, session, width, height, i);
                            } catch (Exception exp) {
                                exp.printStackTrace();
                                throw new ExitException(1, "save frame failed for frame " + i);
                            }
                        }
                     }
                 }

                 // Free the packet that was allocated by av_read_frame
                 av_free_packet(packet);
            }

            throw new ExitException(0, "Goodbye!");
        } catch (ExitException ee) {
            System.err.println(ee.getMessage());
            exitCode = ee.exitCode;
        } finally {
            // clean-up everything

            // Free the RGB image
            if (!buffer.equals(NULL)) {
                av_free(buffer);
            }

            if (!pFrameRGB.equals(NULL)) {
                av_free(pFrameRGB);
            }

            // Free the YUV frame
            if (!pFrame.equals(NULL)) {
                av_free(pFrame);
            }

            // Close the codecs
            if (!pCodecCtx.equals(NULL)) {
                avcodec_close(pCodecCtx);
            }

            if (!pCodecCtxOrig.equals(NULL)) {
                avcodec_close(pCodecCtxOrig);
            }
        }

        System.exit(exitCode);
    }

    private static void saveFrame(MemorySegment frameRGB, MemorySession session,
            int width, int height, int iFrame)
            throws IOException {
        var header = String.format("P6\n%d %d\n255\n", width, height);
        var path = Paths.get("frame" + iFrame + ".ppm");
        try (var os = Files.newOutputStream(path)) {
            System.out.println("writing " + path.toString());
            os.write(header.getBytes());
            var data = AVFrame.data$slice(frameRGB);
            // frameRGB.data[0]
            var pdata = data.get(C_POINTER, 0);
            // frameRGB.linespace[0]
            var linesize = AVFrame.linesize$slice(frameRGB).get(C_INT, 0);
            // Write pixel data
            for (int y = 0; y < height; y++) {
                // frameRGB.data[0] + y*frameRGB.linesize[0] is the pointer. And 3*width size of data
                var pixelArray = pdata.asSlice(y*linesize, 3*width);
                // dump the pixel byte buffer to file
                os.write(pixelArray.toArray(C_CHAR));
            }
        }
    }
}
