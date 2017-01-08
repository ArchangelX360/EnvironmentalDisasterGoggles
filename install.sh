#!/bin/bash

echo "If you don't have a Google Earth Engine API key, please follow instructions to generate it in ./computation/image-fetcher/README.md."
echo "--------"
echo "Type your Google Earth Engine key, followed by [ENTER]:"

read ee_key
echo "{\"refresh_token\": \"$ee_key\"}" > ./computation/imagefetcher/earthengine_token.json

cd frontend/
npm install
./node_modules/.bin/ng build --prod
mkdir -p ../backend/public
cp -r ./dist/* ../backend/public/

cd ../backend/
sbt dist

cd target/universal/
unzip environmentaldisastergoggles-1.0.zip

cd ../../../computation/imagefetcher/
docker build -t imagefetcher .
cd ../nlp/
docker build -t fuseki-server .

