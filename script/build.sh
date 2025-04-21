#!/bin/bash
set -o errexit -o nounset -o pipefail
cd "`dirname $0`/../presence"

yarn install
yarn run build
