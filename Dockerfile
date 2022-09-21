FROM ubuntu:22.10 AS builder

# Allow configuring JDK version
# COMPILE_VERSION will be used with the gradle build task
# RUNTIME_VERSION will be passed to the "-Pjdk19_home" gradle property
ARG SDKMAN_JAVA_COMPILE_VERSION=17-open
ARG SDKMAN_JAVA_RUNTIME_VERSION=19-open

# Allow configuring LLVM download URL
ARG LLVM_DOWNLOAD_URL=https://github.com/llvm/llvm-project/releases/download/llvmorg-14.0.0/clang+llvm-14.0.0-x86_64-linux-gnu-ubuntu-18.04.tar.xz

SHELL ["/bin/bash", "-c"]

# Install dependencies
RUN DEBIAN_FRONTEND=noninteractive apt-get update && apt-get install -y \
    git \
    curl \
    wget \
    bash \
    unzip \
    zip \
    xz-utils

# Install sdkman
RUN curl -s "https://get.sdkman.io" | bash

# Use sdkman to install JDK 19
RUN source "$HOME/.sdkman/bin/sdkman-init.sh" \
    && sdk install java ${SDKMAN_JAVA_COMPILE_VERSION} \
    && sdk install java ${SDKMAN_JAVA_RUNTIME_VERSION} \
    && sdk use java ${SDKMAN_JAVA_COMPILE_VERSION}

# Add sdkman to PATH
ENV SDKMAN_PATH="/root/.sdkman/candidates"
ENV PATH="${SDKMAN_PATH}/java/${SDKMAN_JAVA_COMPILE_VERSION}/bin:$PATH"

# Convenience variable
ENV LLVM_HOME="/tmp/llvm"

# Download and extract LLVM
RUN wget -q -O llvm.tar.xz $LLVM_DOWNLOAD_URL \
    && mkdir -p ${LLVM_HOME} \
    && tar -xf llvm.tar.xz -C ${LLVM_HOME} --strip-components=1

# Download, extract and build jextract
RUN git clone https://github.com/openjdk/jextract.git /tmp/jextract \
    && cd /tmp/jextract \
    && java -version \
    && sh ./gradlew \
        -Pjdk19_home="${SDKMAN_PATH}/java/${SDKMAN_JAVA_RUNTIME_VERSION}" \
        -Pllvm_home=${LLVM_HOME} \
        clean verify

# Copy the built jextract sources to a fresh slim OpenJDK 19 image
FROM openjdk:19-slim
COPY --from=builder /tmp/jextract/build/jextract /usr/local/jextract
ENTRYPOINT ["/usr/local/jextract/bin/jextract"]
