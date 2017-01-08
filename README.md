# EnvironmentalEventsDetector

## Installation

### Requirement: register your Earth Engine API token

To use our application you will need a Google Earth Engine API key. To generate one, please follow these instructions.

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

### Deploy our application

To deploy our application execute the following command:

    ./install.sh
    
## Run our application

To run our application, execute the following command:

    ./run.sh
    
   