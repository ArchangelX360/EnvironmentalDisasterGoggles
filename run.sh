#!/bin/bash

docker run -p 5000:5000 imagefetcher &
docker run -p 3030:3030 fuseki-server &

backend/target/universal/environmentaldisastergoggles-1.0/bin/environmentaldisastergoggles -Dplay.crypto.secret=eisti

