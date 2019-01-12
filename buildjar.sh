#!/bin/bash
#
# Build the Katapult jar
#
set -e
cd app
yarn install
yarn build
cd ../api
./gradlew shadowJar
cp build/libs/Katapult-*.*.*-all.jar ../
cd ..
