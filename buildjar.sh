#!/bin/bash
#
# Build the Katapult jar
#
set -e
cd web
yarn install
yarn build
cd ../srv
./gradlew shadowJar
cp build/libs/Katapult-*.*.*-all.jar ../
cd ..
