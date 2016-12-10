# Python computation stack using the google earth engine.

This directory contains a module helping us to fetch data from the Google Earth
API, with all its requirements.

## Installation

In order to install this module, please run the following:

    python2 setup.py install --user

Once everything is installed, you need to initialize your credentials:

    python2 -c "import ee; ee.Initialize()"

If everything works correctly, the following snippet should work:

```python
import ee

# Initialize the Earth Engine object, using the authentication credentials.
ee.Initialize()

# Print the information for an image asset.
image = ee.Image('srtm90_v4')
print(image.getInfo())
```

**Important note**: whenever an update is done on the code, you may need to
re-run the installation.

## Usage

Once you installed the module, you can run it with the following command:

    python2 -m imagefetcher [options...]

You can also run tests if needed:

    python2 -m imagefetcher.tests
