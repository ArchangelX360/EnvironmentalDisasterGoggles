#!/bin/bash
cd /apache-jena-fuseki-2.4.1/

rm -f /apache-jena-fuseki-2.4.1/run/shiro.ini
cp /usr/src/shiro.ini /apache-jena-fuseki-2.4.1/run/shiro.ini
#TODO(archangel): use user/pass auth as explained here: https://jena.apache.org/documentation/fuseki2/fuseki-security.html

./fuseki-server
