# Fuseki Server installation guide

This directory contains a module helping us to communicate with our ontology
 to cache our processing and thus avoid reprocessing similar queries.

## Server Installation

### Docker (recommended)

In order to install the server, you need to have a working docker installation.
Please follow [the official documentation][docker install] if you do not have
any docker engine installed on your server. To check if a docker engine exists,
run the following command:

    sudo docker run hello-world

Once you have a working environment, you can continue with the final steps

#### Build the docker image and run the docker image

You first need to build the docker image:

    sudo docker build -t fuseki-server .

Once the docker image is built, you can run the server by using the following command:

    sudo docker run -p 3030:3030 fuseki-server

Our application is now able to communicate with our ontology through the port 3030.

### Installation Script

If you don't want to install Docker, you can manually use the installation script.
You just need to execute:

    ./install-fuseki ontology-events.ttl
    
#### Run it without Docker

You will need Java JDK 8 to run the server and execute this in the _computation/nlp/apache-jena-fuseki-2.4.1/_ folder:

    ./fuseki-server