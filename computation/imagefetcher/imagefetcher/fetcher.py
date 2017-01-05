#!/usr/bin/env python2

"""Image fetcher, reaching the Google Earth Engine to get images from it."""

import ee
import requests
import time
import threading

from collections import deque
from datetime import datetime

from utils import Error

# We cannot use a flag here, because of how the application is designed.
DEFAULT_QUERY_PER_SECONDS = 3
OPENSTREETMAP_URL = 'http://nominatim.openstreetmap.org/search'

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

    def _GetRGBImage(self, start_date, end_date, geometry, scale):
        """Generates a RGB satellite image of an area within two dates.

        See :meth:`GetRGBImage` for information about the parameters.
        """
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

    @staticmethod
    def PlaceToGeometry(place_name, place_type=None):
        """Converts a place name to a polygon representation.

        Uses the OpenStreetMap public database to convert a city to a GeoJSON
        representation.

        Parameters:
            place_name: name of the place.
            place_type: type of the place (city, country...).
        Returns:
            A Geometry object representing area of the place.
        """
        params = {
            'format': 'json',
            'polygon_geojson': 1,
            'limit': 1,
        }

        if place_type is None:
            params['q'] = place_name
        else:
            params['place_type'] = place_name

        result = requests.get(OPENSTREETMAP_URL, params=params)

        if not result.ok:
            raise Error('Unable to fetch city name. OpenStreetMap status '
                'code: %s' % result.status_code, 500)

        result_json = result.json()
        if len(result_json) == 0:
            raise Error('Empty result received from the OpenStreetMap.', 500)

        return ee.Geometry(result_json[0].get("geojson", []))

    @staticmethod
    def CityToGeometry(city_name):
        """Converts a city name to a polygon representation.

        Uses the OpenStreetMap public database to convert a city to a GeoJSON
        representation.

        Parameters:
            place_name: name of the city.
        Returns:
            A Geometry object representing area of the city.
        """
        return ImageFetcher.PlaceToGeometry(city_name, place_type='city')

    @staticmethod
    def CountryToGeometry(country_name):
        """Converts a country name to a polygon representation.

        Parameters:
            country_name: name of the country.
        Returns:
            A Geometry object representing area of the country.
        """
        feature_id = 'ft:1tdSwUL7MVpOauSgRzqVTOwdfy17KDbw-1d9omPw'
        countries = ee.FeatureCollection(feature_id)
        name = country_name.capitalize()
        server_geo = countries.filter(ee.Filter.eq('Country', name)).geometry()

        # At this point, the geometry is still server side. As we need to
        # generate the geo json object in order to specify a region to fetch,
        # we will dump the object data and put it in a new, client side
        # geometry.
        return ee.Geometry(server_geo.getInfo())

    @staticmethod
    def VerticesToGeometry(vertices):
        """Converts a list of vertices to an Earth Engine geometry.

        Parameters:
            vertices: A list of vertices representing a polygon.
        Returns:
            The Geometry object corresponding to these vertices.
        """
        return ee.Geometry.Polygon(vertices)

    @staticmethod
    def GeometryToRectangle(geometry):
        """Converts a polygon geometry to the minimal rectangle containing it.

        Parameters:
            geometry: Computed geometry to convert.
        Returns:
            The minimal Geometry.Rectangle object containing the input.
        """

        def get_rectangle_bounds(parts):
            """Returns the minimal rectangle containing all parts of the
            polygon.
            """
            x_min, y_min = float('inf'), float('inf')
            x_max, y_max = -x_min, -y_min

            for part in parts:
                for x, y in part:
                    x_min, y_min = min(x, x_min), min(y, y_min)
                    x_max, y_max = max(x, x_max), max(y, y_max)

            return x_min, y_min, x_max, y_max

        geo_json = geometry.toGeoJSON()

        # For Polygon, simply return the minimal rectangle containing the
        # polygon.
        if geo_json['type'] == 'Polygon':
            return ee.Geometry.Rectangle(*get_rectangle_bounds(
                geo_json['coordinates']))

        if geo_json['type'] != 'MultiPolygon':
            raise Error('Unsupported polygon type: %s' % geo_json['type'], 500)

        # At this point, all geo JSON are of type MultiPolygon. Since some
        # requests may contain multiple points on the earth (such as France's
        # DOM-TOM), we cannot generate a rectangle containing all this point
        # (the Earth Engine API will not appreciate).
        # We simply get the largest rectangle generated from the polygons. This
        # may not be accurate, but at least it works!
        def distance(x_min, y_min, x_max, y_max):
            """Manhattan distance within two 2D points."""
            return x_max - x_min + y_max - y_min

        max_distance, max_bounds = 0, None
        for parts in geo_json['coordinates']:
            bounds = get_rectangle_bounds(parts)
            bounds_distance = distance(*bounds)
            if bounds_distance > max_distance:
                max_distance, max_bounds = bounds_distance, bounds

        return ee.Geometry.Rectangle(*max_bounds)

    def _GetForestIndicesImage(self, start_year, end_year, geometry, scale):
        """Generates a RGB image representing forestation within two years

        See :meth:`GetForestIndicesImage` for information about the parameters.
        """
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
        clipped = rgb_image.clip(geometry)
        return clipped.visualize(min=0, max=2000).getDownloadURL({
            'region': geometry.toGeoJSONString(),
            'scale': scale,
            'format': 'png',
        })

    def GetRGBImage(self, start_date, end_date, geometry, scale=100):
        """Generates a RGB satellite image of an area within two dates.

        Parameters:
            start_date: images in the collection generating the final picture
                must have a later date than this one.
            end_date: images in the collection generating the final picture
                must have a earlier date than this one.
            geometry: area to fetch. Earth Enging Geometry object.
            scale: image resolution, in meters per pixels.
        Returns:
            An URL to the generated image.
        """
        with self.rate_limiter:
            return self._GetRGBImage(start_date, end_date, geometry, scale)

    def GetForestIndicesImage(self, start_year, end_year, geometry, scale):
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
                greater than or equal to 2000.
            end_year: integer representing the year on which we will subtract
                the data generated from the start_year. Must be greater than
                start_year, and lower than or equal to the current year.
            geometry: area to fetch; Earth Engin Geometry object.
            scale: image resolution, in meters per pixels.
        Returns:
            An URL to the generated image.
        """
        with self.rate_limiter:
            return self._GetForestIndicesImage(start_year, end_year, geometry,
                scale)
