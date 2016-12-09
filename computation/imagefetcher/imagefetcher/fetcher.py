#!/usr/bin/env python2

"""Image fetcher, reaching the Google Earth Engine to get images from it."""

import ee
import time
import threading

from collections import deque
from datetime import datetime

# We cannot use a flag here, because of how the application is designed.
DEFAULT_QUERY_PER_SECONDS = 3

class RateLimit:
    """Implementation of a rate limiter.

    This class is highly inspired from the Riot Watcher project, with some
    cool functionalities added.
    """

    def __init__(self, allowed_requests, seconds):
        """Constructor.

        Parameters:
            allowed_requests: number of allowed requests during the time frame.
            seconds: time frame, in seconds.
        """
        self.allowed_requests = allowed_requests
        self.seconds = seconds
        self.made_requests = deque()
        self.lock = threading.Lock()

    def _reload(self):
        """Remove old requests."""
        t = time.time()
        while len(self.made_requests) > 0 and self.made_requests[0] < t:
            self.made_requests.popleft()

    def add_request(self):
        """Add a request to the counter."""
        self.made_requests.append(time.time() + self.seconds)

    def requests_available(self):
        """Check if a request is available.

        Returns:
            False if the rate limit is reached.
        """
        self._reload()
        return len(self.made_requests) < self.allowed_requests

    def __enter__(self):
        """Context management: blocking requests in a threaded context."""
        self.lock.acquire()
        while not self.requests_available():
            time.sleep(0.1)

    def __exit__(self, *args):
        """Context management: release the lock in threaded context."""
        self.lock.release()


class ImageFetcher:
    """Implementation of the image fetcher."""

    def __init__(self, query_per_seconds=DEFAULT_QUERY_PER_SECONDS):
        """Constructor. Initializes a rate limit.

        Parameters:
            query_per_seconds: number of query per seconds on the backend.
        """
        self.rate_limiter = RateLimit(query_per_seconds, 1)

    def _load_land_mask(self):
        """Load a mask of lands and rivers.

        This mask has 0 values on non-lands part of the image (e.g. oceans or
        rivers) and 1 values else. This should be used as a mask to
        manipulate lands and non lands part of the image
        """
        return (ee.Image('MODIS/051/MCD12Q1/2001_01_01')
                .select(['Land_Cover_Type_1'])
                .neq(0))

    def _GetRGBImage(self, start_date, end_date, polygon, scale):
        """Generates a RGB satellite image of an area within two dates.

        See :meth:`GetRGBImage` for information about the parameters.
        """
        geometry = ee.Geometry.Polygon([polygon])

        # Get the Landsat 8 collection.
        # TODO(funkysayu) might be good taking a look at other ones.
        raw_collection = (ee.ImageCollection('LANDSAT/LC8_L1T')
                .filterDate(start_date, end_date)
                .filterBounds(geometry))

        # Reduce the collection to one image, and clip it to the bounds.
        image = raw_collection.median().clip(geometry)

        # Create a visualization of the image
        visualization = image.visualize(
            min=6000,
            max=18000,
            bands=['B4', 'B3', 'B2'],
        )

        # Finally generate the png
        return visualization.getDownloadUrl({
            'region': geometry.toGeoJSONString(),
            'scale': scale,
            'format': 'png',
        })

    def _GetForestIndicesImage(self, start_year, end_year, polygon, scale):
        """Generates a RGB image representing forestation within two years

        See :meth:`GetForestIndicesImage` for information about the parameters.
        """
        geometry = ee.Geometry.Polygon([polygon])
        mask = self._load_land_mask()

        # Within many datasets, the MODIS/MOD13A1 is the most accurate on the
        # amazon rainforest. Other datasets contains noise on some part of the
        # image. Also select EVI, which is more accurate than NDVI here.
        collection = ee.ImageCollection('MODIS/MOD13A1').select(['EVI'])

        # Do the difference between EVI on one year.
        older_evi = collection.filterDate(datetime(start_year, 1, 1),
            datetime(start_year, 12, 31)).median()
        newest_evi = collection.filterDate(datetime(end_year, 1, 1),
            datetime(end_year, 12, 31)).median()
        difference = newest_evi.subtract(older_evi)

        # Set to 0 masked parts, and remove the mask. Thanks to this, image
        # will still be generated on masked parts.
        difference = difference.where(mask.eq(0), 0).unmask()

        # Get negatives (deforestation) and positives (reforestation) parts.
        positives = difference.where(difference.lt(0), 0)
        negatives = difference.where(difference.gte(0), 0).abs()

        # The estimated scale is from 0 to 2000 values. Set the mask to 0 where
        # there is lands and 2000 elsewhere.
        scaled_mask = mask.where(mask.eq(0), 2000).where(mask.eq(1), 0)

        rgb_image = ee.Image.rgb(negatives, positives, scaled_mask)
        return rgb_image.visualize(min=0, max=2000).getDownloadURL({
            'region': geometry.toGeoJSONString(),
            'scale': scale,
            'format': 'png',
        })

    def GetRGBImage(self, start_date, end_date, polygon, scale=100):
        """Generates a RGB satellite image of an area within two dates.

        Parameters:
            start_date: images in the collection generating the final picture
                must have a later date than this one.
            end_date: images in the collection generating the final picture
                must have a earlier date than this one.
            polygon: area to fetch; a list of latitude and longitude making a
                polygon.
            scale: image resolution, in meters per pixels.
        Returns:
            An URL to the generated image.
        """
        with self.rate_limiter:
            return self._GetRGBImage(start_date, end_date, polygon, scale)

    def GetForestIndicesImage(self, start_year, end_year, polygon, scale):
        """Generates a RGB image representing forestation within two years.

        Generates a RGB image where red green and blue channels correspond
        respectively to deforestation, reforestation and non land values. Non
        land values (blue channel) is set to 255 if the pixel is over non-land
        field (such as ocean, rivers...) and 0 elsewhere.

        Analysis are done over one year to ensure weather metrics will not
        polute the result. We also use the Enhanced Vegetation Index (EVI)
        instead of the Normalized Difference Vegetation Index (NDVI) because
        the accuracy is better on this dataset.

        The dataset used to generate this image is the MOD13A1.005 Vegetation
        Indices 16-Day L3 Global 500m [1], provided publicly by the NASA. It is
        updated every 16 days, with a maximum scale of 500 meter per pixels.
        This is the most accurate on the amazon rainforest.

        Parameters:
            start_year: integer representing the reference year. Must be
                greater or equal than 2000.
            end_year: integer representing the year on which we will subtract
                the data generated from the start_year. Must be greater than
                start_year, and lesser or equal to 2016.
            polygon: area to fetch; a list of latitude and longitude making a
                polygon.
            scale: image resolution, in meters per pixels.
        Returns:
            An URL to the generated image.
        """
        with self.rate_limiter:
            return self._GetForestIndicesImage(start_year, end_year, polygon,
                scale)
