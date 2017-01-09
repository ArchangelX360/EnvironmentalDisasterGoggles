#!/bin/bash

PROJECT_PATH=$(dirname "$0")
PROJECT_EE_PATH="$PROJECT_PATH/computation/imagefetcher/earthengine_token.json"
EE_KEY="$HOME/.config/earthengine/credentials"

if [ ! -e "$PROJECT_EE_PATH" ]; then
    echo "Dumping your personal earth engine key"
    if [ ! -e "$EE_KEY" ]; then
        echo "Error: Earth Engine API key not found in $EE_KEY"
        echo "Please follow instructions to generate it in ./computation/imagefetcher/README.md"
        exit 1
    fi
    cp "$EE_KEY" "$PROJECT_EE_PATH"
fi

cd frontend/
npm install
./node_modules/.bin/ng build --prod
mkdir -p ../backend/public
cp -r ./dist/* ../backend/public/

cd ../backend/
sbt dist

cd target/universal/
unzip environmentaldisastergoggles-1.0.zip

echo "You may be asked for your sudo password to build docker images"
cd ../../../computation/imagefetcher/
sudo docker build -t imagefetcher .
cd ../nlp/
sudo docker build -t fuseki-server .

