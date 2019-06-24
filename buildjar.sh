#!/bin/bash
#
# Build the Katapult jar
#
# You don't have to use this script. You can have your IDE do it, for example.
# The main aspects are:
#
# 1. Run `yarn build` to produce the app distribution, which is copied to `api/src/main/resources/app`
# 2. Run the gradle shadowJar process, which produces a single jar, including the app as a static resource
#
set -e
cd app
yarn install
yarn build
cd ../api
./gradlew shadowJar
cp build/libs/katapult-*.*.*-all.jar ../
cd ..
