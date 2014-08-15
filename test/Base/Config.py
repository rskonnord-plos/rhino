#!/usr/bin/env python2

import os


# Set API_BASE_URL environment variable to desired URL in order to run suite against it
API_BASE_URL = os.getenv('API_BASE_URL', 'http://localhost:8080/rhino')

PRINT_DEBUG = False

TIMEOUT = 30         # API call timeout, in seconds
