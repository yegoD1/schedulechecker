#!/usr/bin/env bash
DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
cd "${DIR}"
keytool -importcert -alias ClassScheduler -cacerts -file ssbstureg-gmu-edu.pem -storepass changeit
