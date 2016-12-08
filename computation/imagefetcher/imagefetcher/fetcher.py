#!/usr/bin/env python2

"""Image fetcher, reaching the Google Earth Engine to get images from it."""

import ee
import time
import threading

from collections import deque

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
        """Constructor. Initialize a rate limit.

        Parameters:
            query_per_seconds: number of query per seconds on the backend.
        """
        self.rate_limiter = RateLimit(query_per_seconds, 1)

    def _GetRGBImage(self, start_date, end_date, polygon, scale):
        """Generates a RGB satellite image of an area within two dates.

        See :meth:`GetRGBImage` for information about the parameters.
        """
        geometry = ee.Geometry.Polygon([polygon])

        print(start_date, end_date, polygon, scale)

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
