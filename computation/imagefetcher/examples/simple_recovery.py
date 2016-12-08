#!/usr/bin/env python2

"""Example using images taken from the Landsat 8 satellite.

Images are composed of many 'bands'. A band is basically an image taken using
a specific camera lens.
All satellites support at least 3 bands: red, green and blue. Some of them
have more bands, such as near infrared, thermal infrared, shortwave infrared...

Technically, the Earth Engine uses only a scale from 0 to 255 to generate its
image, but some bands may use other scales. We need to help the Earth Engine
understand the band format.

Also, by default, the Earth Engine takes the first 3 bands and respectively
map them to red, green and blue colors. However, bands may not match the RGB
colors so we may need to specify which band the engine should use.
"""

import ee

# Initialize the Earth Engine.
ee.Initialize()

rectangle = ee.Geometry.Rectangle(-123, 38, -121, 36)
region = rectangle.bounds().getInfo()['coordinates'][0]

# Take one image of the Landsat 8 satellite.
raw_image = ee.Image('LC8_L1T/LC80440342014077LGN00')

# The first three bands of a Landsat image is costal aerosol, blue and green.
# Tell the engine to use the 4th (red band), the 3rd (green band) and the 2nd
# (blue band).
image = raw_image.select(['B4', 'B3', 'B2'])

# Landsat images use a scale from 6000 to 18000, so tell the engine to use
# this scale.
params = {'min': 6000, 'max': 18000}
params.update({
    'region': region,
})

# Finally generate the link
print image.getDownloadUrl(params)
