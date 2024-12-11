#!/usr/bin/env bash
DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
cd "${DIR}"
keytool -delete -alias ClassScheduler -cacerts -storepass changeit