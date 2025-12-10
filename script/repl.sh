#!/bin/bash
set -o errexit -o nounset -o pipefail
cd "`dirname $0`/.."

clj $(./script/java_opts.sh) -M:java:dev -m user --ip 0.0.0.0 --port 8080 --dev true