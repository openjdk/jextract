To run Generative AI models with ONNX runtime, make sure to have downloaded a native [`libonnxruntime`](https://github.com/microsoft/onnxruntime/release) and [`libonnxruntime-genai`](https://github.com/microsoft/onnxruntime-genai/releases).

1. The [`libonnxruntime-genai`](https://github.com/microsoft/onnxruntime-genai/releases) binary needs the statically built binaries from [`libonnxruntime`](https://github.com/microsoft/onnxruntime/release), so you have to set `ORT_LIB_PATH` to the directory containing the downloaded [`libonnxruntime`](https://github.com/microsoft/onnxruntime/release), then the ONNX_GEN_AI_HOME to [`libonnxruntime-genai`](https://github.com/microsoft/onnxruntime-genai/releases). 
The native library extension (`.dylib` or `.so` or `.dll`) is platform specific.

    ```bash
    export ORT_LIB_PATH=/path/.../onnxruntime/lib/libonnxruntime.[dylib|so|dll]
    export ONNX_GENAI_HOME=/path/.../onnxruntime-genai/
    ```
2. Run `compile.sh`:

    ```bash
    ./compile.sh
    ```
   
3. Download or prepare a valid ONNX model:

   ```bash
    git clone https://huggingface.co/microsoft/Phi-3-mini-4k-instruct-onnx
    cd Phi-3-mini-4k-instruct-onnx
    export MODEL_PATH=/path/.../Phi-3-mini-4k-instruct-onnx/cpu_and_mobile/cpu-int4-rtn-block-32-acc-level-4/
    ```

4. Run the example:

    ```bash
    sh ./run.sh
    ```
   