import flask
import gflags
import json
import mock
import requests
import threading
import time
import unittest

from itertools import combinations

import app
from imagefetcher.utils import Parser

FLAGS = gflags.FLAGS

VALID_DATE = "2015-04-01"
VALID_POLYGON = json.dumps([[0, 0], [10, 0], [10, 10], [0, 10], [0, 0]])

@app.app.route('/shutdown')
def shutdown():
    """Shutdown the flask application."""
    flask.request.environ.get('werkzeug.server.shutdown')()
    return ""


class FlaskApplicationTest(unittest.TestCase):
    """Test the Flask application routes is correctly handled."""

    @classmethod
    def setUpClass(cls):
        """Set up the Flask server and start listening for requests."""
        # Generate server address, based on port flag's default value.
        gflags.FLAGS([])
        cls.server_address = ("127.0.0.1", FLAGS.port)
        cls.base_url = "http://%s:%s" % (cls.server_address)

        cls.thread = threading.Thread(target=app.app.run,
            args=cls.server_address)
        cls.thread.start()
        time.sleep(1)  # Wait the server starts

        # Save the fetcher previously generated in the app module. We are going
        # to override it. Everytime.
        cls._base_fetcher = app.fetcher

    @classmethod
    def tearDownClass(cls):
        """Shutdown the Flask server."""
        requests.get("http://%s:%s/shutdown" % cls.server_address)
        cls.thread.join()

        # Reset the fetcher.
        app.fetcher = cls._base_fetcher

    def setUp(self):
        """Test setup. Defines a new mock in the fetcher."""
        self.fetcher = mock.MagicMock()
        app.fetcher = self.fetcher

    def do_request(self, route="/", params=None):
        """Sends the request to the server.

        Parameters:
            route: route to check.
            params: GET parameters to send.
        Returns:
            The requests.Response object sent by the server.
        """
        if params is None:
            params = {}

        return requests.get(self.base_url + route, params=params)

    def test_server_running(self):
        """Simply test the server is running."""
        response = self.do_request()
        self.assertEquals(response.status_code, 200)

    def test_rgb_invalid_parameters(self):
        """Test if missing arguments are correctly handled."""
        required_args = {
            'polygon': VALID_POLYGON,
            'country': 'congo',
            'city': 'Pau',
        }

        for r in range(1, len(required_args)):
            for params in combinations(required_args.items(), r):
                params = dict(params)
                response = self.do_request("/rgb", params=params)
                # Date is missing, it will report 400 everytime
                self.assertEquals(response.status_code, 400, params)

                params.update({'date': '2000-01-01'})
                response = self.do_request("/rgb", params=params)
                # It will report 400 except if only one parameter is specified.
                status = 400 if r != 1 else 200
                self.assertEquals(response.status_code, status, params)

    def test_rgb_invalid_date(self):
        """Test if invalid date is correctly handled."""
        response = self.do_request("/rgb", params={
            'date': 'bad-bad',
            'polygon': VALID_POLYGON,
        })
        self.assertEquals(response.status_code, 400)

    def test_rgb_invalid_polygon(self):
        """Test if invalid polygons are correctly handled."""
        invalids = [
            {"somekey": "somevalue"},
            [1, 2],
            [[1, 2, 3], [1, 2], [1, 3]],
            [["1", "2"], ["2", "3"]],
        ]

        for invalid in invalids:
            response = self.do_request("/rgb", params={
                'date': VALID_DATE,
                'polygon': json.dumps(invalid)
            })
            self.assertEqual(response.status_code, 400)

    def test_rgb_valid_simple_polygon_query(self):
        """Test a valid query."""
        self.fetcher.GetRGBImage.return_value = "http://something.com/foo"
        response = self.do_request("/rgb", params={
            'date': VALID_DATE,
            'polygon': VALID_POLYGON,
        })
        self.assertEqual(response.status_code, 200, "Server sent error: %s" %
            response.json().get("error", "[internal error]"))
        self.assertTrue(self.fetcher.VerticesToGeometry.called)
        self.assertTrue(self.fetcher.GetRGBImage.called)

    def test_rgb_valid_simple_country_query(self):
        """Test a valid query."""
        self.fetcher.GetRGBImage.return_value = "http://something.com/foo"
        response = self.do_request("/rgb", params={
            'date': VALID_DATE,
            'country': 'congo',
        })
        self.assertEqual(response.status_code, 200, "Server sent error: %s" %
            response.json().get("error", "[internal error]"))
        self.assertTrue(self.fetcher.CountryToGeometry.called)
        self.assertTrue(self.fetcher.GetRGBImage.called)

    def test_rgb_valid_complex_query(self):
        """Test a valid query with all parameters."""
        self.fetcher.GetRGBImage.return_value = "http://something.com/foo"
        response = self.do_request("/rgb", params={
            'date': VALID_DATE,
            'polygon': VALID_POLYGON,
            'scale': 1000,
            'delta': '0000-01-00',
        })
        self.assertEqual(response.status_code, 200, "Server sent error: %s" %
            response.json().get("error", "[internal error]"))
        self.assertTrue(self.fetcher.GetRGBImage.called)

    def test_rgb_date_delta_supported(self):
        """Test if date delta is fully supported."""
        date_parameters = [
            ("2015-12-01", "0000-1-0"),
            ("2015-12-31", "0000-0-1"),
            ("2000-01-01", "0000-3-0"),
            ("2000-01-01", "0000-0-1"),
            ("2000-01-01", "0000-1-1"),
        ]

        for date, delta in date_parameters:
            response = self.do_request("/rgb", params={
                'date': date,
                'polygon': VALID_POLYGON,
                'delta': delta,
            })
            self.assertEqual(response.status_code, 200, "Server sent error: %s" %
                response.json().get("error", "[internal error]"))
            self.assertTrue(self.fetcher.GetRGBImage.called)

    def test_forest_diff_invalid_parameters(self):
        """Test if missing arguments are correctly handled."""
        required_args = {
            'polygon': VALID_POLYGON,
            'country': 'congo',
            'city': 'Pau',
        }

        for r in range(2, len(required_args)):
            for params in combinations(required_args.items(), r):
                params = dict(params)
                response = self.do_request("/forestDiff", params=params)
                self.assertEquals(response.status_code, 400, params)

    def test_forest_diff_invalid_polygon(self):
        """Test if invalid polygons are correctly handled."""
        invalids = [
            {"somekey": "somevalue"},
            [1, 2],
            [[1, 2, 3], [1, 2], [1, 3]],
            [["1", "2"], ["2", "3"]],
        ]

        for invalid in invalids:
            response = self.do_request("/forestDiff", params={
                'polygon': json.dumps(invalid)
            })
            self.assertEqual(response.status_code, 400)

    def test_forest_diff_invalid_years(self):
        """Test invalid years."""
        invalid_dates = [
            (1999, None),
            (2016, None),
            (1999, 2015),
            (None, 2017),
            (None, 2000),
            (2001, 2017),
            (2000, 2000),
            (2005, 2004),
        ]

        for start, stop in invalid_dates:
            params = {"polygon": VALID_POLYGON}
            if start is not None:
                params["start"] = start
            if stop is not None:
                params["stop"] = stop

            response = self.do_request("/forestDiff", params=params)
            self.assertEqual(response.status_code, 400)

    def test_forest_diff_valid_simple_polygon_query(self):
        """Test a valid query."""
        self.fetcher.GetForestIndicesImage.return_value = "http://foo.com/bar"
        response = self.do_request("/forestDiff", params={
            'polygon': VALID_POLYGON,
        })
        self.assertEqual(response.status_code, 200, "Server sent error: %s" %
            response.json().get("error", "[internal error]"))
        self.assertTrue(self.fetcher.VerticesToGeometry.called)
        self.assertTrue(self.fetcher.GetForestIndicesImage.called)

    def test_forest_diff_valid_simple_country_query(self):
        """Test a valid query."""
        self.fetcher.GetForestIndicesImage.return_value = "http://foo.com/bar"
        response = self.do_request("/forestDiff", params={
            'country': 'congo',
        })
        self.assertEqual(response.status_code, 200, "Server sent error: %s" %
            response.json().get("error", "[internal error]"))
        self.assertTrue(self.fetcher.CountryToGeometry.called)
        self.assertTrue(self.fetcher.GetForestIndicesImage.called)

    def test_forest_diff_valid_complex_query(self):
        """Test a valid query with all parameters."""
        self.fetcher.GetForestIndicesImage.return_value = "http://foo.com/bar"
        response = self.do_request("/forestDiff", params={
            'start': 2001,
            'stop': 2015,
            'polygon': VALID_POLYGON,
            'scale': 600,
        })
        self.assertEqual(response.status_code, 200, "Server sent error: %s" %
            response.json().get("error", "[internal error]"))
        self.assertTrue(self.fetcher.GetForestIndicesImage.called)


if __name__ == "__main__":
    unittest.main()
