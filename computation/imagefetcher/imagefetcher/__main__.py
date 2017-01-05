#!/usr/bin/env python2


"""Image fetcher module entry point.

This file contains is the entry point to the following command:
    python2 -m imagefetcher

This will startup the Flask application.
"""

import sys
import gflags

from app import app

FLAGS = gflags.FLAGS

if __name__ == "__main__":
    FLAGS(sys.argv)
    app.run(host=FLAGS.host, port=FLAGS.port, debug=FLAGS.debug)
