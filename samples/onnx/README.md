1. Clone `onnxruntime-genai` and build Java bindings:

    ```bash
    git clone https://github.com/microsoft/onnxruntime-genai.git
    cd onnxruntime-genai
    ./build.sh --build_java --config Release
   ````
    or
   ```bash
    ./build.sh
    ```

2. Set `ONNX_LIB_PATH` to the directory containing the built native library
   (e.g. where `libonnxruntime-genai.so` or `.dylib` lives):

    ```bash
    export ONNX_LIB_PATH=/.../onnxruntime-genai/build/macOS/RelWithDebInfo/
    ```

3. Set `ORT_GENAI_DIR` to your clone of `onnxruntime-genai` before running `compile.sh`:

    ```bash
    export ORT_GENAI_DIR=/path/.../onnxruntime-genai/
    ```

4. Download or prepare a valid ONNX model:

   ```bash
    git clone https://huggingface.co/microsoft/Phi-3-mini-4k-instruct-onnx
    cd Phi-3-mini-4k-instruct-onnx
    export MODEL_PATH=/path/.../Phi-3-mini-4k-instruct-onnx/cpu_and_mobile/cpu-int4-rtn-block-32-acc-level-4/
    ```

5. Run `compile.sh`:

    ```bash
    ./compile.sh
    ```

6. Run the example:

    ```bash
    sh ./run.sh
    ```
