#!/bin/bash
while true; do
  inotifywait -r -e modify,create,delete src/main/java/org/twc/terminator
  mvn package -Dmaven.test.skip
  for file in src/test/resources/tests_reduced/*.t2; do
    java -jar target/terminator-compiler-1.0.jar \
    $file --CLEAR --N 4096
  done
done
