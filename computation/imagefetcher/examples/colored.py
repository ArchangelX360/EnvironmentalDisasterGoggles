#!/usr/bin/env python2

"""Example of images collections manipulation.

In this example, we will see how to manipulate images collections. Image
collections are a set of images taken from a satellite (so using the same
scales and bands).

The first issue is the number of images we have. Some images may not be taken
at the same times than others, resulting in discontinuities while connecting
them. Earth Engine give some utilities to select images matching a specific
date period. To give you an idea, Landsat 8 satellite visits the same spot on
the Earth every 16 days.

Also, some parts of an image may overlap other images. To re-create a smooth
image, we can use some reducers. By default, the engine will select the most
recent pixels. Here, we will use the median to get the most accurate value of
the pixel.
"""

import ee

# Initialize the Earth Engine.
ee.Initialize()

# We need then to project the image on a polygon. We are doing treatments on
# the whole earth, which is quite long to compute. Let's reduce this to a part
# of the Colorado state, using a definition of the region.
rectangle = ee.Geometry.Rectangle(-101, 39, -102, 40)

# Get the Landsat 8 collection, filtered on a date chunk.
raw_collection = (ee.ImageCollection('LANDSAT/LC8_L1T')
        .filterDate("2000-01-01", "2015-06-1")
        .filterBounds(rectangle))

# Compile the collection as a single image using the median value of a band
# when multiple images overlap.
sanitized_image = raw_collection.median()

# Snap it to the desired bounds and select the expected bands.
clipped = sanitized_image.clip(rectangle)

# Convert it to RGB image
visualized = clipped.visualize(
    min=6000,
    max=18000,
    bands=['B4', 'B3', 'B2'],
)

# Finally generate the png
print visualized.getDownloadUrl({
    'region': rectangle.toGeoJSONString(),
    'scale': 100,
    'format': 'png',
})
