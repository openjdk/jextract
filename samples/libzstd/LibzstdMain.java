/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.foreign.*;
import java.util.HexFormat;

import static libzstd.Libzstd.*;

/*
 * This sample compresses text from a memory segment to another memory segment.
 */
public class LibzstdMain {

    // Credit: William Shakespeare (1564-1616 CE).
    // From: "Hamlet", Act III, Scene I
    private static final String TEXT = """
            To be, or not to be: that is the question:
            Whether 'tis nobler in the mind to suffer
            The slings and arrows of outrageous fortune,
            Or to take arms against a sea of troubles,
            And by opposing end them? To die: to sleep;
            No more; and by a sleep to say we end
            The heart-ache and the thousand natural shocks
            That flesh is heir to, 'tis a consummation
            Devoutly to be wish'd. To die, to sleep;
            To sleep: perchance to dream: ay, there's the rub;
            For in that sleep of death what dreams may come
            When we have shuffled off this mortal coil,
            Must give us pause: there's the respect
            That makes calamity of so long life;
            For who would bear the whips and scorns of time,
            The oppressor's wrong, the proud man's contumely,
            The pangs of despised love, the law's delay,
            The insolence of office and the spurns
            That patient merit of the unworthy takes,
            When he himself might his quietus make
            With a bare bodkin? who would fardels bear,
            To grunt and sweat under a weary life,
            But that the dread of something after death,
            The undiscover'd country from whose bourn
            No traveller returns, puzzles the will
            And makes us rather bear those ills we have
            Than fly to others that we know not of?
            Thus conscience does make cowards of us all;
            And thus the native hue of resolution
            Is sicklied o'er with the pale cast of thought,
            And enterprises of great pith and moment
            With this regard their currents turn awry,
            And lose the name of action.—Soft you now!
            The fair Ophelia! Nymph, in thy orisons
            Be all my sins remember'd.""";

    public static void main(String[] args) {
        System.out.println("Original text:");
        System.out.println(TEXT);
        System.out.println();

        try (var session = MemorySession.openConfined()) {
            // Compress
            var uncompressedText = session.allocateUtf8String(TEXT);
            // At least, the compressed text should not be larger than the uncompressed text.
            var compressedText = session.allocate(TEXT.length());
            long compressResult = ZSTD_compress(compressedText, compressedText.byteSize(), uncompressedText, uncompressedText.byteSize(), ZSTD_defaultCLevel());
            if (ZSTD_isError(compressResult) != 0) {
                System.out.println("Error compressing: " + errorMessage(compressResult));
                return;
            }
            System.out.println(TEXT.length() + " text length was compressed to " + compressResult + " bytes of data:");
            var formatter = HexFormat.ofDelimiter(" ");
            System.out.println(formatter.formatHex(compressedText.asSlice(0, compressResult).toArray(ValueLayout.JAVA_BYTE)));
            System.out.println();

            // Decompress again
            var decompressed = session.allocate(TEXT.length() * 2, 64); // Needs extra space to decompress
            long decompressResult = ZSTD_decompress(decompressed, decompressed.byteSize(), compressedText, compressResult);
            if (ZSTD_isError(decompressResult) != 0) {
                System.out.println("Error decompressing: " + errorMessage(decompressResult));
                return;
            }
            String decompressedString = decompressed.getUtf8String(0);
            System.out.println(compressResult + " bytes of compressed data was decompressed to " + decompressedString.length() + " text length:");
            System.out.println(decompressedString);
        }
    }

    private static String errorMessage(long code) {
        return code + " (" + ZSTD_getErrorName(code).getUtf8String(0) + ")";
    }

}
