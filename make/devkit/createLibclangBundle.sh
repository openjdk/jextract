#!/bin/bash
#
# Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation.
#
# This code is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# version 2 for more details (a copy is included in the LICENSE file that
# accompanied this code).
#
# You should have received a copy of the GNU General Public License version
# 2 along with this work; if not, write to the Free Software Foundation,
# Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
# or visit www.oracle.com if you need additional information or have any
# questions.
#

# This build script builds a libclang bundle to be used for jextract.
# Both cmake and ninja (ninja-build) are required to be installed for
# this script to run.

# Exit on error
set -e

LLVM_VERSION=13.0.0

BUNDLE_NAME=libclang-$LLVM_VERSION.tar.gz

SCRIPT_DIR="$(cd "$(dirname $0)" > /dev/null && pwd)"
SCRIPT_FILE="$(basename $0)"
OUTPUT_DIR="${SCRIPT_DIR}/../../build/libclang"
SRC_DIR="$OUTPUT_DIR/src"
BUILD_DIR="$OUTPUT_DIR/build"
DOWNLOAD_DIR="$OUTPUT_DIR/download"
INSTALL_DIR="$OUTPUT_DIR/install"
IMAGE_DIR="$OUTPUT_DIR/image"

OS_NAME=$(uname -s)
OS_ARCH=$(arch)
NUM_CORES=$(nproc --all)

USAGE="$0 <devkit dir>"

if [ "$1" = "" ]; then
    echo $USAGE
    exit 1
fi
DEVKIT_DIR="$1"

# OS specific build flags
case $OS_NAME in
  Linux)
    LIB_SUFFIX=.so
    CMAKE_C_COMPILER="$DEVKIT_DIR/bin/gcc"
    CMAKE_CXX_COMPILER="$DEVKIT_DIR/bin/g++"
    CMAKE_C_FLAGS="-static-libgcc"
    CMAKE_CXX_FLAGS="-static-libgcc -static-libstdc++"
    ;;
  Darwin)
    LIB_SUFFIX=.dylib
    CMAKE_C_COMPILER="$DEVKIT_HOME/Xcode/Contents/Developer/usr/bin/gcc"
    CMAKE_CXX_COMPILER="$DEVKIT_HOME/Xcode/Contents/Developer/usr/bin/g++"
    ;;
  *)
    echo " Unsupported OS: $OS_NAME"
    exit 1
    ;;
esac

# Figure out target arch to pass to LLVM build
case $OS_ARCH in
  aarch64)
    TARGET_ARCH=AArch64
    ;;
  x86_64)
    TARGET_ARCH=X86_64
    ;;
  *)
    echo " Unsupported arch: $OS_ARCH"
    exit 1
    ;;
esac

# Also supported: Debug, RelWithDebInfo
CMAKE_BUILD_TYPE=Release

# Download source
LLVM_FILE=llvmorg-$LLVM_VERSION.tar.gz
DOWNLOADED_FILE="$DOWNLOAD_DIR/$LLVM_FILE"
mkdir -p "$DOWNLOAD_DIR"
if [ ! -f "$DOWNLOADED_FILE" ]; then
  wget -O "$DOWNLOADED_FILE" https://github.com/llvm/llvm-project/archive/refs/tags/$LLVM_FILE 
fi

# Unpack
if [ ! -e "$SRC_DIR" ]; then
  tar -xvf "$DOWNLOADED_FILE" --one-top-level="$SRC_DIR" --strip-components=1
fi

# Configure LLVM
cmake \
  -B "$BUILD_DIR/llvm" \
  -S "$SRC_DIR/llvm" \
  -G Ninja \
  -DCMAKE_INSTALL_PREFIX="$INSTALL_DIR/llvm" \
  -DCMAKE_BUILD_TYPE=$CMAKE_BUILD_TYPE \
  -DLLVM_ENABLE_TERMINFO=no \
  -DLLVM_TARGETS_TO_BUILD=$TARGET_ARCH \
  -DCMAKE_C_COMPILER="$CMAKE_C_COMPILER" \
  -DCMAKE_CXX_COMPILER="$CMAKE_CXX_COMPILER" \
  -DCMAKE_C_FLAGS="$CMAKE_C_FLAGS" \
  -DCMAKE_CXX_FLAGS="$CMAKE_CXX_FLAGS"

# Build LLVM
cmake --build "$BUILD_DIR/llvm" --config $CMAKE_BUILD_TYPE --target install --parallel $NUM_CORES

# Configure Clang
cmake \
  -B "$BUILD_DIR/clang" \
  -S "$SRC_DIR/clang" \
  -G Ninja \
  -DCMAKE_INSTALL_PREFIX="$INSTALL_DIR/clang" \
  -DCMAKE_BUILD_TYPE=$CMAKE_BUILD_TYPE \
  -DCMAKE_PREFIX_PATH="$INSTALL_DIR/llvm/lib/cmake" \
  -DLLVM_INCLUDE_TESTS=OFF \
  -DLLVM_ENABLE_TERMINFO=no \
  -DLLVM_TARGETS_TO_BUILD=$TARGET_ARCH \
  -DCMAKE_C_COMPILER="$CMAKE_C_COMPILER" \
  -DCMAKE_CXX_COMPILER="$CMAKE_CXX_COMPILER" \
  -DCMAKE_C_FLAGS="$CMAKE_C_FLAGS" \
  -DCMAKE_CXX_FLAGS="$CMAKE_CXX_FLAGS"

# Build Clang
cmake --build "$BUILD_DIR/clang" --config $CMAKE_BUILD_TYPE --target install --parallel $NUM_CORES


mkdir -p $IMAGE_DIR
# Extract what we need into an image
echo "Copying libclang$LIB_SUFFIX to image"
mkdir -p "$IMAGE_DIR/lib"
cp -a $INSTALL_DIR/clang/lib/libclang.* $IMAGE_DIR/lib/

echo "Copying include to image"
mkdir -p $IMAGE_DIR/include
cp -a $INSTALL_DIR/clang/include/. $IMAGE_DIR/include/

echo "Copying lib/clang/*/include to image"
mkdir -p $IMAGE_DIR/lib/clang/$LLVM_VERSION/include
cp -a $INSTALL_DIR/clang/lib/clang/$LLVM_VERSION/include/. \
    $IMAGE_DIR/lib/clang/$LLVM_VERSION/include/

# Copy this script to image
echo "Copying this script to image"
cp -a $0 $IMAGE_DIR


# Create bundle
echo "Creating $OUTPUT_DIR/$BUNDLE_NAME"
cd $IMAGE_DIR
tar zcf $OUTPUT_DIR/$BUNDLE_NAME *
