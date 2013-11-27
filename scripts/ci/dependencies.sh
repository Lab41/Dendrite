#!/usr/bin/env bash

sudo apt-get update -qq

# install git
sudo apt-get install -y git

# build titan-lab41
git clone https://github.com/Lab41/titan.git
cd titan
git checkout dendrite
mvn install -DskipTests
cd ..

# build faunus-lab41
git clone https://github.com/Lab41/faunus.git
cd faunus
git checkout dendrite
mvn install -DskipTests
cd ..
