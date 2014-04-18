#!/usr/bin/env bash

sudo apt-get update -qq

# install git
sudo apt-get install -y git

# build titan-lab41
git clone https://github.com/Lab41/titan.git
cd titan
git checkout dendrite-hadoop2
mvn install -DskipTests
cd ..

# build faunus-lab41
git clone https://github.com/Lab41/faunus.git
cd faunus
git checkout dendrite-hadoop2
mvn install -DskipTests
cd ..

## (optional) build wsdoc for REST api documentation
git clone https://github.com/scottfrederick/springdoclet.git
pushd springdoclet
mvn install -DskipTests
popd

## (optional) setup LESS compilation for CSS styling
sudo apt-get install --assume-yes nodejs npm
npm install less
