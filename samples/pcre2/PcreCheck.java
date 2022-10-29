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
import static java.lang.foreign.MemorySegment.NULL;
import static org.pcre.Pcre.*;

class PcreCheck {
  public static void main(String[] args) {
    if (args.length < 2) {
      System.err.println("java PcreCheck <regex> <string>");
      System.exit(1);
    }

    try (var session = MemorySession.openConfined()) {
      // given regex to C string
      var pattern = session.allocateUtf8String(args[0]);
      var patternSize = args[0].length();

      // output params from pcre2_compile_8
      var errcodePtr = session.allocate(C_INT);
      var erroffPtr = session.allocate(C_LONG);

      // compile the given regex
      var re = pcre2_compile_8(pattern, patternSize, 0,
          errcodePtr, erroffPtr, NULL);

      // if compilation failed, report error and exit
      if (re.equals(NULL)) {
        var buffer = session.allocateArray(C_CHAR, 128L);
        pcre2_get_error_message_8(errcodePtr.get(C_INT, 0),
            buffer, 127);
        System.err.printf("regex compilation failed: %s\n",
            buffer.getUtf8String(0));
        System.exit(2);
      }

      var ovecsize = 64;
      var matchData = pcre2_match_data_create_8(ovecsize, NULL);

      var subject = session.allocateUtf8String(args[1]);
      var subjectSize = args[1].length();
      var rc = pcre2_match_8(re, subject, subjectSize, 0, 0, matchData, NULL);
      if (rc == 0) {
        System.err.printf("offset vector too small: %d\n",rc);
      } else if (rc < 0) {
        System.err.println("no match found!");
      } else {
        var ovector = pcre2_get_ovector_pointer_8(matchData);
        var subjectArray = subject.toArray(C_CHAR);
        for (int i = 0; i < rc; i++) {
          var start = ovector.getAtIndex(C_LONG, 2*i);
          var end = ovector.getAtIndex(C_LONG, (2*i + 1));
          System.out.printf("%d: %s\n", i,
            new String(subjectArray, (int)start, (int)(end-start)));
        }
      }

      pcre2_match_data_free_8(matchData);
      pcre2_code_free_8(re);
    }
  }
}
