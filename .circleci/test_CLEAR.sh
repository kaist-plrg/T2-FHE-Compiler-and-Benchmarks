#!/bin/bash

set -exo pipefail

echo "Testing CLEAR with SEAL"

mkdir -p ./src/CLEAR/compiled
mkdir -p ./src/SEAL/compiled

exclude_files=("ckks_test" "rotate" "rotate_ckks")

for file in ./src/test/resources/tests_reduced/*.t2; do
    filename=$(basename "$file" .t2)

    if [[ " ${exclude_files[@]} " =~ " $filename " ]]; then
        continue
    fi

    java -jar target/terminator-compiler-1.0.jar \
    $file --CLEAR --N 4096
    cp ./src/test/resources/tests_reduced/$(basename $file .t2).cpp ./src/CLEAR/compiled/test.cpp
    cd ./src/CLEAR
    cmake .
    make
    ./bin/test.out > ../test/resources/tests_reduced/$(basename $file .t2)_CLEAR.log
    diff <(head -n -1 ../test/resources/tests_reduced/$(basename $file .t2)_CLEAR.log | awk '{$1=$1};1') \
     <(head -n -1 ../test/resources/tests/$(basename $file .t2)_SEAL.log | awk '{$1=$1};1' | cut -d ' ' -f 3-)
    cd ../..
done
