import flask
import gflags
import json
import mock
import requests
import threading
import unittest

import app

VALID_DATE = "2015-04-01"
VALID_POLYGON = json.dumps([[0, 0], [10, 0], [10, 10], [0, 10], [0, 0]])

@app.app.route('/shutdown')
def shutdown():
    """Shutdown the flask application."""
    flask.request.environ.get('werkzeug.server.shutdown')()
    return ""


class RGBRouteTest(unittest.TestCase):
    """Test the /rgb route is correctly handled."""

    server_address = ("127.0.0.1", 5000)
    base_url = "http://%s:%s/rgb" % (server_address)

    @classmethod
    def setUpClass(cls):
        """Setup the Flask server and start listening for requests."""
        # We need to load the flags library before running the tests.
        gflags.FLAGS([])
        cls.thread = threading.Thread(target=app.app.run,
                args=cls.server_address)
        cls.thread.start()

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

    def test_server_running(self):
        """Simply test the server is running."""
        response = requests.get("http://%s:%s/" % self.server_address)
        self.assertEquals(response.status_code, 200)

    def test_missing_arguments(self):
        """Test if missing arguments are correctly handled."""
        response = requests.get(self.base_url)
        self.assertEquals(response.status_code, 400)
        response = requests.get(self.base_url, params={'date': '2000-01-01'})
        self.assertEquals(response.status_code, 400)
        response = requests.get(self.base_url,
            params={'polygon': VALID_POLYGON})
        self.assertEquals(response.status_code, 400)

    def test_invalid_date(self):
        """Test if invalid date is correctly handled."""
        response = requests.get(self.base_url, params={
            'date': 'bad-bad',
            'polygon': VALID_POLYGON,
        })
        self.assertEquals(response.status_code, 400)

    def test_invalid_polygon(self):
        """Test if invalid polygons are correctly handled."""
        invalids = [
            {"somekey": "somevalue"},
            [1, 2],
            [[1, 2, 3], [1, 2], [1, 3]],
            [["1", "2"], ["2", "3"]],
        ]

        for invalid in invalids:
            response = requests.get(self.base_url, params={
                'date': VALID_DATE,
                'polygon': json.dumps(invalid)
            })
            self.assertEqual(response.status_code, 400)

    def test_valid_simple_query(self):
        """Test a valid query."""
        self.fetcher.GetRGBImage.return_value = "http://something.com/foo"
        response = requests.get(self.base_url, params={
            'date': VALID_DATE,
            'polygon': VALID_POLYGON,
        })
        self.assertEqual(response.status_code, 200)
        self.assertTrue(self.fetcher.GetRGBImage.called)

    def test_valid_complex_query(self):
        """Test a valid query with all parameters."""
        self.fetcher.GetRGBImage.return_value = "http://something.com/foo"
        response = requests.get(self.base_url, params={
            'date': VALID_DATE,
            'polygon': VALID_POLYGON,
            'scale': 1000,
            'delta': '0000-01-00',
        })
        self.assertEqual(response.status_code, 200)
        self.assertTrue(self.fetcher.GetRGBImage.called)


if __name__ == "__main__":
    unittest.main()
