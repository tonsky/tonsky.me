#!/bin/bash
set -o errexit -o nounset -o pipefail
cd "`dirname $0`/.."

exec clojure $(./script/java_opts.sh) -M:java -m site.server "$@"
