#!/usr/bin/env python2

"""Request handler utilities.

Defines a set of parsers and utilities for easier request handling.
"""

import functools
import json

from datetime import datetime
from dateutil.relativedelta import relativedelta
from flask import request


class Error(Exception):
    """Exception raised when an error occurs in the API."""

    status_code = 400

    def __init__(self, message, status_code=400):
        Exception.__init__(self)
        self.message = message
        self.status_code = status_code

    def to_dict(self):
        return {"error": self.message}


def get_param(param_name, parser=str, required=False, default=None):
    """Decorator used on route handler to pass a get parameter to the function.

    Parameters:
        param_name: name of the parameter, used in the request.
        parser: eventual function parsing the parameter value.
        required: boolean indicating if the request should be drop if the
            parameter is missing.
        default: default value if the parameter is unspecified.
    Returns:
        The wrapped function.
    """
    if required and default is not None:
        raise ValueError("A required parameter cannot have a default value.")

    def decorator(func):
        """Parametrized decorator."""

        @functools.wraps(func)
        def wrapper(*args, **kwargs):
            """Wrapper on the function, parsing GET parameter."""
            param_value = request.args.get(param_name)

            if required and param_value is None:
                raise Error("Expected GET parameter '%s' missing." %
                    param_name)

            if param_value is not None:
                try:
                    param_value = parser(param_value)
                except Error:
                    raise
                except Exception as e:
                    raise Error(str(e))
            else:
                param_value = default

            kwargs.update({param_name: param_value})
            return func(*args, **kwargs)

        return wrapper
    return decorator


def get_geometry(get_parameters):
    """Pick one geometry from the request parameters.

    Parameters:
        get_parameters: parameter names associated to their value and
            converter.
    Returns:
        The geometry associated to the request.
    Raises:
        Error: if strictly less or more than 1 geometry parameter is not None.
    """
    non_null_parameters = [key for key, value in get_parameters.items()
        if value[0] is not None]

    if len(non_null_parameters) > 1:
        raise Error("Only one of %s GET parameters can be specified." %
            "/".join(non_null_parameters))
    if len(non_null_parameters) == 0:
        raise Error("At least one of %s GET parameters must be specified." %
            "/".join(get_parameters.keys()))

    value, parser = get_parameters[non_null_parameters[0]]
    return parser(value)


def scale_from_geometry(rectangle):
    """Attempts to compute a best scale based on the Image geometry.

    Parameters:
        rectangle: Rectangle generated from the client query.
    Returns:
        A 'best scale' based on the rectangle values.
    """
    # Get the rectangle bounds
    x_0, y_0 = rectangle.toGeoJSON()['coordinates'][0][0]
    x_1, y_1 = rectangle.toGeoJSON()['coordinates'][0][2]

    return int((abs(x_0 - x_1) + abs(y_0 - y_1)) * 80)


class Parser:
    """Set of utilities used to parse query parameters."""

    @staticmethod
    def date(entry):
        """Parse an entry as a date formated as yyyy-mm-dd.

        Parameters:
            entry: a string supposed to contain a date.
        Returns:
            A datetime object representing a date.
        Raises:
            ValueError: if the entry is invalid.
        """
        return datetime.strptime(entry, "%Y-%m-%d")

    @staticmethod
    def polygon(entry):
        """Parse an entry as a polygon.

        A polygon is a list of coordinates (list of two numbers) representing
        points of the polygon. The start element and end element must match.

        Examples of valid polygons:
            [[1, 2], [2, 3], [4, 9], [1, 2]]
            [[10.9, -23.3, [8, -20], [10, -10], [10.9, -23.3]]]

        Parameters:
            entry: a string supposed to contain a polygon
        Returns:
            A list corresponding to valid polygon
        Raises:
            ValueError: if the json is unreadable.
            AssertionError: if the polygon is invalid.
        """
        try:
            polygon = json.loads(entry)
        except ValueError:
            raise ValueError("Unreadable JSON sent.")

        assert type(polygon) == list, "Not a list"
        assert all(type(p) == list for p in polygon), "Not a list of list."
        assert all(len(p) == 2 for p in polygon), ("Some points do not have 2 "
                "coords.")
        assert all(all(type(c) in (int, float) for c in p)
                for p in polygon), "Some points have invalid types."
        assert polygon[0] == polygon[-1], "Last point must equal first point."

        return polygon

    @staticmethod
    def date_delta(entry):
        """Parse an entry as a date delta formated as yyyy-mm-dd.

        Parameters:
            entry: a string supposed to contain a date delta.
        Returns:
            A relativedelta object generated from the entry.
        Raises:
            ValueError: if the date delta is invalid.
        """
        year, month, day = [int(t) for t in entry.split("-")]
        return relativedelta(years=year, months=month, days=day)
