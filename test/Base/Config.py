# Copyright (c) 2017 Public Library of Science
#
# Permission is hereby granted, free of charge, to any person obtaining a
# copy of this software and associated documentation files (the "Software"),
# to deal in the Software without restriction, including without limitation
# the rights to use, copy, modify, merge, publish, distribute, sublicense,
# and/or sell copies of the Software, and to permit persons to whom the
# Software is furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in
# all copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
# THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
# FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
# DEALINGS IN THE SOFTWARE.

#/usr/bin/env python2

import os
from os import getenv
from selenium.webdriver import DesiredCapabilities

# Set API_BASE_URL environment variable to desired URL in order to run suite against it
API_BASE_URL = os.getenv('API_BASE_URL', 'http://rhino-201.sfo.plos.org:8006/v2')
INGESTION_HOST = os.getenv('INGESTION_HOST', 'rhino-201.sfo.plos.org')
INGEST_USER = os.getenv('INGEST_USER', 'rinotest')
RHINO_INGEST_PATH = os.getenv('RHINO_INGEST_PATH', '/var/spool/ambra/ingestion-queue')
SSH_PASSWORD = os.getenv('SSH_PASSWORD', 'Shoh1yar')


PRINT_DEBUG = False
TIMEOUT = 60         # API call timeout, in seconds

# === WebDriver specific section ===

# WebDriver's implicit timeout (in seconds)
wait_timeout = 60

# WebDriver's Page Load timeout (in seconds)
page_load_timeout = 60

# Framework's link verification timeout (in seconds)
verify_link_timeout = 3

# Framework's link verification retries
verify_link_retries = 5

# Framework's link verification wait time between retries (in seconds)
wait_between_retries = 2

"""
Set **WEBDRIVER_ENVIRONMENT** env variable/default value to one of these:

1. **prod** will run tests against **PRODUCTION** site (each POM should know it prod site)
2. **dev** will run tests against the site defined by `base_url` variable.

When **WEBDRIVER_ENVIRONMENT** is set to "prod" then **WEBDRIVER_TARGET_URL** is ignored

Set **WEBDRIVER_TARGET_URL** env variable/default value to desired URL to run suite against it
when **WEBDRIVER_ENVIRONMENT** is `dev`
"""

environment = getenv('WEBDRIVER_ENVIRONMENT', 'dev')
base_url = getenv('WEBDRIVER_TARGET_URL', 'http://one-dpro.plosjournals.org/wombat')


"""
Create a DB Configuration for use in MySQL.py
"""

dbconfig = {'user': 'root',
            'password': '',
            'host': 'db-ambra-201.sfo.plos.org',
            'port': 3306,
            'database': 'ambra',
            'connection_timeout': 10,
            }

repo_config = {'transport': 'http',
               'host': 'contentrepo-201.sfo.plos.org',
               'port': 8016,
               'path': '/v1',
               }

# === Appium (stand-alone) module configuration section ===


"""
Since a Macintosh box can run both Android SDK & VMs *and* IOS Simulator it would be wise to
have Appium deployed on Macintosh hardware.

But in case it is not possible, you can still point to an Appium node for Android and a different
one for IOS, separately.
"""

run_against_appium = getenv('USE_APPIUM_SERVER', False)
appium_android_node_url = getenv('APPIUM_NODE_URL', 'http://10.136.100.186:4723/wd/hub')
appium_ios_node_url = getenv('APPIUM_NODE_URL', 'http://10.136.100.186:4723/wd/hub')

# **Android** capabilities definition
ANDROID = {'browserName': 'Browser',
           'platformName': 'Android',
           'platformVersion': '4.4',
           'deviceName': 'Android Emulator'
           }

# **iOS** capabilities definition
IOS = {'browserName': 'Safari',
       'platformName': 'iOS',
       'platformVersion': '7.1',
       'deviceName': 'iPhone Simulator'
       }

# List of Appium (stand-alone mode) enabled browsers
appium_enabled_browsers = [ANDROID,
                           IOS
                           ]


# === Selenium Grid configuration section ===


"""
Set **USE_SELENIUM_GRID** env variable/default value to desired one of these:

1. **True** will run tests against ALL browsers in PLoS's Selenium Grid
2. **False** will run tests against your local **Firefox** browser

Set **SELENIUM_GRID_URL** env variable/default value to point to PLoS's Grid hub node.

Running through Grid **takes precedence** among all other configurations.

Ex: If you have both `USE_APPIUM_SERVER` **AND** `USE_SELENIUM_GRID` options set to **True**,
then tests will be run against the **Grid**.

You can *still* include IOS and ANDROID capabilities as *enabled browsers* in the Grid and will be run against Appium.

"""

run_against_grid = getenv('USE_SELENIUM_GRID', False)
selenium_grid_url = getenv('SELENIUM_GRID_URL', 'http://10.136.104.99:4444/wd/hub')

"""

List of all PLoS's browsers enabled in the **Selenium Grid**.
Ignored when **run_against_grid** is set to **False**.

"""

grid_enabled_browsers = [DesiredCapabilities.FIREFOX,
                         DesiredCapabilities.INTERNETEXPLORER,
                         DesiredCapabilities.CHROME,
                         DesiredCapabilities.SAFARI,
                         IOS,
                         ANDROID
                         ]


# === Performance metric gathering configuration section ===


"""

Set **browsermob_proxy_enabled** to either *True* or *False* to enable or disable the use of
**BrowserMob Proxy** to gather performance metrics while the tests navigate the web sites.

Set **browsermob_proxy_path** variable to the path of the **BrowserMob Proxy** *binary* in your
machine.

"""
browsermob_proxy_enabled = getenv('USE_BROWSERMOB_PROXY', False)
browsermob_proxy_path = '/opt/browsermob/bin/browsermob-proxy'
