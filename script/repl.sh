#!/bin/bash
set -o errexit -o nounset -o pipefail
cd "`dirname $0`/.."

./script/build.sh

clj -M:dev -m user --ip 0.0.0.0 --port 8080 --dev true