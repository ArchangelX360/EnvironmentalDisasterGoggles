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
"""

import ee
import gflags
import json

from flask import Flask
from flask import jsonify

from fetcher import ImageFetcher
from utils import DateDelta
from utils import Error
from utils import Parser
from utils import get_param


app = Flask(__name__)

FLAGS = gflags.FLAGS
gflags.DEFINE_string("default_delta", "0000-03-00", "default range "
        "within images are still considered as 'close to the date'.")
gflags.DEFINE_string("default_scale", "100", "default image scale, "
        "in meter per pixels (lower is better).")
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
@get_param("polygon", parser=Parser.polygon, required=True)
@get_param("scale", parser=int, default=100)
@get_param("delta", parser=Parser.date_delta, default=DateDelta(0, 3, 0))
def GetRGBImage(date, polygon, scale, delta):
    """Generates a RGB image of an area. Images are in PNG (in a zip).

    GET query parameters:
        date (yyyy-mm-dd):
            Average date of the image to fetch. Required.
        polygon (list[list[int]]):
            Area to visualize. Required.
        scale (int):
            Precision of the picture. Unit is meter per pixels so lower is
            better.
        delta (yyyy-mm-dd):
            Delta within images are considered valid.
    Returns:
        A JSON containing metadata about the image:
            href (link):
                Link to download the image.
            error (str):
                In case of error, displays the error message.
    """
    start_date, end_date = delta.generate_start_end_date(date)
    url = fetcher.GetRGBImage(start_date, end_date, polygon, scale)
    return jsonify(href=url)


@app.route("/")
def main_route():
    """Simple route useful for checking if the server is alive."""
    return ""


if __name__ == "__main__":
    import sys
    FLAGS(sys.argv)
    app.run(host=FLAGS.host, port=FLAGS.port, debug=FLAGS.debug)
