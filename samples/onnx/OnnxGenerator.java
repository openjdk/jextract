/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.function.Consumer;

import static oracle.code.onnx.foreign.ort_genai_c_h$shared.*;
import static oracle.code.onnx.foreign.ort_genai_c_h.*;

public class OnnxGenerator implements AutoCloseable {


    static final String PROMPT_TEMPLATE = """
    <|system|>
You are a helpful assistant.<|end|>
<|user|>
%s<|end|>
<|assistant|>""";


    private final Arena arena;
    private final MemorySegment ret, model, tokenizer, tokenizerStream, generatorParams, generator, count;
    private final Consumer<String> out;

    public OnnxGenerator(String modelPath, Consumer<String> out) {
        arena = Arena.ofConfined();
        ret = arena.allocate(C_POINTER);
        model = call(OgaCreateModel(arena.allocateFrom(modelPath), ret));
        tokenizer = call(OgaCreateTokenizer(model, ret));
        tokenizerStream = call(OgaCreateTokenizerStream(tokenizer, ret));
        generatorParams = call(OgaCreateGeneratorParams(model, ret));
        call(OgaGeneratorParamsSetSearchNumber(generatorParams, arena.allocateFrom("max_length"), 1024)); // this is necessary to run with GPU model
        generator = call(OgaCreateGenerator(model, generatorParams, ret));
        count = arena.allocate(C_LONG);
        this.out = out;
    }

    public static void main(String[] args) throws Exception {
        System.loadLibrary("onnxruntime-genai");
        Reader inreader = new InputStreamReader(System.in);
        try (var gen = new OnnxGenerator(args[0], System.out::print)) {
            BufferedReader in = new BufferedReader(inreader);
            String str;
            System.out.print("> ");
            while ((str = in.readLine()) != null) {
                gen.prompt(str);
                System.out.print("> ");
            }
            in.close();
        }
    }

    private MemorySegment call(MemorySegment status) {
        try {
            if (!status.equals(MemorySegment.NULL)) {
                status = status.reinterpret(C_INT.byteSize());
                if (status.get(C_INT, 0) != 0) {
                    String errString = OgaResultGetError(status)
                            .reinterpret(Long.MAX_VALUE)
                            .getString(0L);
                    throw new RuntimeException(errString);
                }
            }
            return ret.get(C_POINTER, 0);
        } finally {
            OgaDestroyResult(status);
        }
    }

    public int prompt(String userPrompt) {
        var inputTokens = call(OgaCreateSequences(ret));
        int ntokens = 0;
        try {
            call(OgaTokenizerEncode(tokenizer, arena.allocateFrom(PROMPT_TEMPLATE.formatted(userPrompt)), inputTokens));
            call(OgaGenerator_AppendTokenSequences(generator, inputTokens));
            while (!OgaGenerator_IsDone(generator)) {
                ntokens++;
                call(OgaGenerator_GenerateNextToken(generator));
                int nextToken = call(OgaGenerator_GetNextTokens(generator, ret, count)).get(C_INT, 0);
                String response = call(OgaTokenizerStreamDecode(tokenizerStream, nextToken, ret)).getString(0);
                out.accept(response);
            }
            out.accept("\n");
            return ntokens;
        } finally {
            OgaDestroySequences(inputTokens);
        }
    }

    @Override
    public void close() throws Exception {
        arena.close();
        OgaDestroyGenerator(generator);
        OgaDestroyGeneratorParams(generatorParams);
        OgaDestroyTokenizerStream(tokenizerStream);
        OgaDestroyTokenizer(tokenizer);
        OgaDestroyModel(model);
    }
}
