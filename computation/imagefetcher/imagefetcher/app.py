#!/usr/bin/env python2

"""Application definition for the Image Fetcher API.

The Image Fetcher creates a Flask server handling several routes sending
requests to the Google Earth Engine, that generates a link to download
images. All routes returns matadatas containing notably the download link.

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

from datetime import datetime
from datetime import timedelta
from flask import Flask
from flask import jsonify
from flask import request

from fetcher import ImageFetcher


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


class Error(Exception):
    """Exception raised when an error occurs in the API."""

    status_code = 400

    def __init__(self, message, status_code=400):
        Exception.__init__(self)
        self.message = message
        self.status_code = status_code

    def to_dict(self):
        return {"error": self.message}


@app.errorhandler(Error)
def handle_error(error):
    """Handler triggered when the Error exception is raised."""
    response = jsonify(error.to_dict())
    response.status_code = error.status_code
    return response


@app.route('/rgb')
def GetRGBImage():
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
    raw_date = request.args.get('date')
    if raw_date is None:
        raise Error("Missing required argument 'date'.")
    raw_polygon = request.args.get('polygon')
    if raw_polygon is None:
        raise Error("Missing required argument 'polygon'.")
    raw_delta = request.args.get('delta', FLAGS.default_delta)
    raw_scale = request.args.get('scale', FLAGS.default_scale)

    try:
        date = datetime.strptime(raw_date, "%Y-%m-%d")
    except ValueError as e:
        raise Error(str(e))

    try:
        polygon = json.loads(raw_polygon)
        assert type(polygon) == list, "Not a list"
        assert all(type(p) == list for p in polygon), "Not a list of list."
        assert all(len(p) == 2 for p in polygon), ("Some points have "
            "does not have 2 coords.")
        assert all(all(type(c) in (int, float) for c in p)
                for p in polygon), "Some points have invalid types."
        assert polygon[0] == polygon[-1], "Last point must equal first point."
    except ValueError:
        raise Error("Unreadable JSON sent in 'polygon' argument.")
    except AssertionError as e:
        raise Error("Invalid JSON. Error was: " + str(e))

    try:
        year, month, day = [int(t) for t in raw_delta.split("-")]
        start_date = datetime(date.year - year, date.month - month, date.day - day)
        end_date = datetime(date.year + year, date.month + month, date.day + day)
    except ValueError:
        raise Error("Malformed delta date.")

    try:
        scale = int(raw_scale)
    except ValueError:
        raise Error("Scale is not an integer.")

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
