#!/bin/bash

set -exo pipefail

read -p "Do you want to build with Debug type? (y/n): " build_debug

if [ "$build_debug" = "y" ]; then
    cmake_build_type="-DCMAKE_BUILD_TYPE=Debug"
else
    cmake_build_type=""
fi

#echo "Build HElib v2.2.2"
#if [ ! -d "HElib/build" ] ; then
#    cd ./HElib
#    git reset --hard d7be6f0
#    mkdir -p build && cd build
#    cmake -DPACKAGE_BUILD=ON -DCMAKE_INSTALL_PREFIX=/opt/helib_install ..
#    make -j2
#    sudo make install
#    sudo ln -s /usr/local/lib/libntl.so.44 /usr/lib/libntl.so.44
#    cd ../..
#else
#    echo "Found in cache"
#fi
#
#echo "Build PALISADE v1.11.9"
#if [ ! -d "palisade-release/build" ] ; then
#    cd ./palisade-release
#    git reset --hard 3d1f9a3f
#    mkdir -p build && cd build
#    cmake ..
#    make -j2
#    sudo make install
#    sudo ln -s /usr/local/lib/libPALISADEcore.so.1 /usr/lib/libPALISADEcore.so.1
#    cd ../..
#else
#    echo "Found in cache"
#fi

openfhe_versions=("v1.0.1" "v1.0.2" "v1.0.3" "v1.0.4" "v1.1.2")
for version in "${openfhe_versions[@]}"; do
    git clone https://github.com/openfheorg/openfhe-development.git OpenFHE-$version
    echo "Build OpenFHE $version"
    cd ./OpenFHE-$version
    rm -rf build
    git reset --hard $version
    mkdir -p build && cd build
    cmake $cmake_build_type -DBUILD_UNITTESTS=OFF -DBUILD_EXAMPLES=OFF -DBUILD_BENCHMARKS=OFF -DCMAKE_INSTALL_PREFIX=/usr/local/OpenFHE-$version ..
    make -j 10
    sudo make install
    cd ../..
done

seal_versions=("v3.7.2" "v3.7.3" "v4.0.0" "v4.1.0" "v4.1.1")
for version in "${seal_versions[@]}"; do
    echo "Build SEAL $version"
    git clone https://github.com/microsoft/SEAL.git SEAL-$version
    cd ./SEAL-$version
    m -rf build
    git reset --hard $version
    cmake -S . -B build $cmake_build_type -DSEAL_BUILD_BENCH=OFF -DSEAL_BUILD_EXAMPLES=OFF -DSEAL_BUILD_TESTS=OFF -DCMAKE_INSTALL_PREFIX=/usr/local/SEAL-$version
    cmake --build build
    sudo cmake --install build
    cd ..
done

#echo "Build TFHE v1.0.1"
#if [ ! -d "tfhe/build" ] ; then
#    cd ./tfhe
#    make -j2 && sudo make install
#    sudo ln -s /usr/local/lib/libtfhe-nayuki-avx.so /usr/lib/libtfhe-nayuki-avx.so
#    sudo ln -s /usr/local/lib/libtfhe-nayuki-portable.so /usr/lib/libtfhe-nayuki-portable.so
#    sudo ln -s /usr/local/lib/libtfhe-spqlios-avx.so /usr/lib/libtfhe-spqlios-avx.so
#    sudo ln -s /usr/local/lib/libtfhe-spqlios-fma.so /usr/lib/libtfhe-spqlios-fma.so
#    cd ..
#else
#    echo "Found in cache"
#fi
#
#echo "Lattigo version is defined in src/Lattigo/go.mod"
