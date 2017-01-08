#!/usr/bin/env python2

"""Application definition for the Image Fetcher API.

The Image Fetcher creates a Flask server handling several routes sending
requests to the Google Earth Engine that generates a link to download
images. All routes returns metadata containing notably the download link.

This application requires a Google Earth Engine token to work. If you do not
own one, please ask one on the official website [1]. If you do own one, you
can initialize it by running the following command:
    python2 -c "import ee; ee.Initialize()"

Defined routes are:
    /rgb
    /forestDiff
"""

import ee
import gflags
import json

from datetime import date
from dateutil.relativedelta import relativedelta
from flask import Flask
from flask import jsonify

from fetcher import ImageFetcher
from utils import Error
from utils import Parser
from utils import get_param
from utils import get_geometry
from utils import scale_from_geometry


app = Flask(__name__)

FLAGS = gflags.FLAGS
gflags.DEFINE_string("host", "0.0.0.0", "Server listening host.")
gflags.DEFINE_integer("port", 5000, "Server listening port.")
gflags.DEFINE_boolean("debug", False, "Run in debug mode.")

# Initialize the Google Earth Engine
ee.Initialize()


fetcher = ImageFetcher()


@app.errorhandler(Error)
def handle_error(error):
    """Handler triggered when the Error exception is raised."""
    response = jsonify(error.to_dict())
    response.status_code = error.status_code
    return response


@app.route('/rgb')
@get_param("date", parser=Parser.date, required=True)
@get_param("polygon", parser=Parser.polygon, default=None)
@get_param('place', parser=str, default=None)
@get_param("country", parser=str, default=None)
@get_param('city', parser=str, default=None)
@get_param("scale", parser=float, default=None)
@get_param("delta", parser=Parser.date_delta, default=relativedelta(months=6))
def rgb_handler(date, polygon, place, country, city, scale, delta):
    """Generates a RGB image of an area. Images are in PNG (in a zip).

    GET query parameters:
        date (yyyy-mm-dd):
            Average date of the image to fetch. Required.
        polygon (list[list[int]]):
            Area to visualize. Required, or city/country must be specified.
        place (str):
            Place to visualize. This is automatically converted to GeoJSON by
            the OpenStreetMap API. Required, or another position must be
            specified.
        country (str):
            Country to visualize. Required, or city/polygon must be specified.
        city (str):
            City to visualize. Required, or country/polygon must be specified.
        scale (float):
            Precision of the picture. Unit is meter per pixels so lower is
            better. Attempts to automatically generate it if not specified.
        delta (yyyy-mm-dd):
            Delta within images are considered valid.
    Returns:
        A JSON containing metadata about the image:
            href (link):
                Link to download the image.
            error (str):
                In case of error, displays the error message.
    """
    geometry = get_geometry({
        'country': (country, fetcher.CountryToGeometry),
        'polygon': (polygon, fetcher.VerticesToGeometry),
        'place': (place, fetcher.PlaceToGeometry),
        'city': (city, fetcher.CityToGeometry),
    })

    rectangle = fetcher.GeometryToRectangle(geometry)
    if scale is None:
        scale = scale_from_geometry(rectangle)

    start_date = date - delta
    end_date = date + delta
    url = fetcher.GetRGBImage(start_date, end_date, rectangle, scale)
    return jsonify(href=url, geojson=geometry.toGeoJSON(),
        image_geojson=rectangle.toGeoJSON())


@app.route('/forestDiff')
@get_param('polygon', parser=Parser.polygon, default=None)
@get_param('place', parser=str, default=None)
@get_param('country', parser=str, default=None)
@get_param('city', parser=str, default=None)
@get_param('start', parser=int, default=2000)
@get_param('stop', parser=int, default=date.today().year)
@get_param('scale', parser=float, default=None)
def forest_diff_handler(polygon, place, country, city, start, stop, scale):
    """Generates a RGB image of an are representing {de,re}forestation.

    Generates a RGB image where red green and blue channels correspond
    respectively to deforestation, reforestation and non land values. Non
    land values (blue channel) is set to 255 if the pixel is over non-land
    field (such as ocean, rivers...) and 0 elsewhere.

    See :meth:`ImageFetcher.GetForestIndicesImage` for more informations.

    GET Parameters:
        polygon (list[list[int]]):
            Area to visualize. Required, or another position must be specified.
        place (str):
            Place to visualize. This is automatically converted to GeoJSON by
            the OpenStreetMap API. Required, or another position must be
            specified.
        country (str):
            Country to visualize. Required, or other position must be specified.
        city (str):
            City to visualize. Required, or another position must be specified.
        start (int):
            Reference year. Must be greater than or equal to 2000.
        stop (int):
            Year on which we will subtract the data generated from start year.
            Must be greater than start year, and lower than current year.
        scale (float):
            Precision of the picture. Unit is meter per pixels so lower is
            better. Attempts to automatically generate it if not specified.
    Returns:
        A JSON containing metadata about the image:
            href (link):
                Link to download the image.
            error (str):
                In case of error, displays the error message.
    """
    geometry = get_geometry({
        'country': (country, fetcher.CountryToGeometry),
        'place': (place, fetcher.PlaceToGeometry),
        'polygon': (polygon, fetcher.VerticesToGeometry),
        'city': (city, fetcher.CityToGeometry),
    })

    rectangle = fetcher.GeometryToRectangle(geometry)
    if scale is None:
        scale = scale_from_geometry(rectangle)

    current_year = date.today().year
    try:
        assert 2000 <= start <= current_year, ("Start year must be within 2000 "
            "and %s" % current_year)
        assert start < stop <= current_year, ("Stop year must be within start "
            "and %s" % current_year)
    except AssertionError as e:
        raise Error(str(e))

    stop = min(current_year - 1, stop)
    start = min(stop - 1, start)

    url = fetcher.GetForestIndicesImage(start, stop, rectangle, scale)
    return jsonify(href=url, geojson=geometry.toGeoJSON(),
        image_geojson=rectangle.toGeoJSON())


@app.route("/")
def main_route():
    """Simple route useful for checking if the server is alive."""
    return ""
