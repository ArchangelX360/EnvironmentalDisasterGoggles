#!/usr/bin/env python2

"""Example of server-side computations used in global forest change analysis.

In this example we will focus on server side computation using NDVI and EVI
data. This both metrics are computed bands created by third party companies
or directly taken by the satellites.

NDVI and EVI are two metrics used in global forest change analysis. They
represent the forest concentration in a specific area. We will use the
MOD13A1 vegetation indice provided by the NASA [1].

The goal is to generate an RGB image, where reds stands for deforestation,
gree for reforestation and blue for masked data (e.g. rivers, oceans...).

  [1] https://code.earthengine.google.com/dataset/MODIS/MOD13A1
"""

import ee

# Initialize the Earth Engine
ee.Initialize()

# Small rectangle used to generate the image, over the Amazonian forest.
# The location is above the Rondonia (West of Bresil).
rectangle = ee.Geometry.Rectangle(-68, -7, -65, -8)

# Get the MODIS dataset.
collection = ee.ImageCollection('MODIS/MOD13A1')

# Select the EVI, since it is more accurate on this dataset. You can also
# use the NDVI band here.
collection = collection.select(['EVI'])

# Get two dataset, one over the year 2000 and the other one over 2015
ndvi2000 = collection.filterDate('2000-01-01', '2000-12-31').median()
ndvi2015 = collection.filterDate('2015-01-01', '2015-12-31').median()

# Substract the two datasets to see the evolution between both of them.
difference = ndvi2015.subtract(ndvi2000)

# Use a mask to avoid showing data on rivers.
# TODO(funkysayu) move this mask to blue color.
classifiedImage = ee.Image('MODIS/051/MCD12Q1/2001_01_01')
mask = classifiedImage.select(['Land_Cover_Type_1'])
maskedDifference = difference.updateMask(mask)

# Convert it to RGB image.
visualized = maskedDifference.visualize(
    min=-2000,
    max=2000,
    palette='FF0000, 000000, 00FF00',
)

# Finally generate the PNG.
print visualized.getDownloadUrl({
    'region': rectangle.toGeoJSONString(),
    'scale': 500,
    'format': 'png',
})
