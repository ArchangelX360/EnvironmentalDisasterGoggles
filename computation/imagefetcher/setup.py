#!/usr/bin/python2

"""
Python installation script.
"""

from setuptools import setup, find_packages

__author__ = "Axel Martin"
__copyright__ = "Copyright 2016, Axel Martin"
__credits__ = ["Titouan Bion", "Pierre-Baptiste Bouillon", "Damien Cassan",
    "Axel Martin"]
__license__ = "MIT"
__version__ = "0.1"
__maintainer__ = "Axel Martin"
__email__ = "funkysayu@gmail.com"
__status__ = "Prototype"

name = "imagefetcher"

with open("classifiers.txt") as f:
    classifiers = f.read().splitlines()

with open("requirements.txt") as f:
    requirements = f.read().splitlines()

with open("README.md") as f:
    readme = f.read()

extra_requirements = {}

setup(
    name=name,
    description="Image fetcher utility based on the Google Earth API.",
    long_description=readme,
    author=__author__,
    version=__version__,
    license=__license__,
    author_email=__email__,
    packages=find_packages(),
    namespace_packages=[],
    install_requires=requirements,
    include_package_data=True,
    zip_safe=False,
    classifiers=classifiers,
)

