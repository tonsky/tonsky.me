#!/bin/bash
set -o errexit -o nounset -o pipefail -o xtrace
cd "`dirname $0`/.."

rsync --verbose --compress --recursive --times --delete _site site@tonsky.me:
rsync --verbose --compress --recursive --times --delete site site@tonsky.me: