# Python computation stack using the google earth engine.

This directory contains a module helping us to fetch data from the Google Earth
API, with all its requirements.

## Server Installation

In order to install the server, you need to have a working docker installation.
Please follow [the official documentation][docker install] if you do not have
any docker engine installed on your server. To check if a docker engine exists,
run the following command:

    sudo docker run hello-world

Once you have a working environment, you can continue with the following steps

### Requirement: register your Earth Engine API token

In order to run this server, you must own an Earth Engine API token. You can
ask for an access on their [official website][earth engine]. **This is a
requirement to run our server.**

#### Create a token

If you never used the Python Earth Engine API, you must first download the
required packages to get a token:

    sudo apt-get update
    sudo apt-get install python-dev python-pip
    pip install --user google-api-python-client pyCrypto earthengine-api

Once you have all those packages installed, you can then authenticate.

    ~/.local/bin/earthengine authenticate

Your token is stored in `~/.config/earthengine/credentials`.

#### Give your token to the server

To register your Earth Engine API token, you must simply copy the credentials
in this directory, as following:

    cp ~/.config/earthengine/credentials earthengine_token.json

You are now ready to build the Docker image.

### Build the docker image and run the docker image

Since we use several python packages that might require compilation, you first
need to build the docker image:

    sudo docker build -t imagefetcher .

Once the docker image is built, you can run the server by using the following command:

    sudo docker run -p 5000:5000 imagefetcher

### Running the examples

We provided usage examples of Earth Engine API, **which are not a requirement
for running our application**. If you want to run them, you should first install
several modules as following:

    sudo apt-get update
    sudo apt-get install python-dev python-pip
    pip install --user google-api-python-client pyCrypto earthengine-api

If you never used the earth engine API, you should also initialize it by running
the following command:

    ~/.local/bin/earthengine authenticate

You can then run any examples like this:

    python2 examples/colored.py

[docker install]: https://docs.docker.com/engine/installation/linux/
