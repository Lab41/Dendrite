#!/bin/bash

cd "`dirname $0`/../../../.."

echo ""
echo "Starting Karma Server (http://karma-runner.github.io)"
echo "-------------------------------------------------------------------"

karma start src/test/javascript/config/karma-e2e.conf.js "$@"
