#!/bin/bash

if [ "$#" -ne 1 ]; then
    echo "Illegal number of parameters"
    echo -e "\nUsage:\n$0 [ontology_filepath] \n"
    exit 1
fi

echo "Creating server environment (for testing only)..."
echo "Downloading fuseki binaries..."
wget http://mirrors.ircam.fr/pub/apache/jena/binaries/apache-jena-fuseki-2.4.1.tar.gz
#cp ~/Downloads/apache-jena-fuseki-2.4.1.zip ./
echo "Extracting server..."
tar -xvf apache-jena-fuseki-2.4.1.tar.gz
#unzip apache-jena-fuseki-2.4.1.zip
cd apache-jena-fuseki-2.4.1/
echo "Starting empty server"
./fuseki-server &
last_pid=$!
cd ..
sleep 8 # TODO: cleaner waiting for server startup
echo "Creating persistent DB"
curl -X POST --data-urlencode "dbName=/environmentEvents" --data-urlencode "dbType=tdb" http://localhost:3030/$/datasets
echo "Filling DB with our custom ontology"
curl -X POST -F "name=@$1" http://localhost:3030/environmentEvents/data
sleep 5
echo "Shutting down server..."
kill -KILL $last_pid
