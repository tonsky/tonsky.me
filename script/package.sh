#!/bin/bash
set -o errexit -o nounset -o pipefail
cd "`dirname $0`/.."

clojure -M:uberdeps -m uberdeps.uberjar --target target/site.jar