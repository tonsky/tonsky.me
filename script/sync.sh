#!/bin/bash
set -o errexit -o nounset -o pipefail
cd "`dirname $0`/.."

# build
./script/build.sh

# blog posts
FILES=`find site/blog -name index.md`
for file in $FILES; do
    time="$(git log --pretty=format:%cd -n 1 --date=format:%Y%m%d%H%M.%S --date-order -- "$file")"
    echo "changing modification time of" $file "to" $time
    touch -m -t "$time" "$file"
done

touch -m -t 202207261436.26 site/blog/dice-out/index.md 
touch -m -t 202303092055.37 site/blog/clojure-sublimed-3/index.md
touch -m -t 202305031142.50 site/blog/humble-state/index.md
touch -m -t 202305191428.55 site/blog/humble-signals/index.md 
touch -m -t 202306291434.24 site/blog/clojurescript-2/index.md

# rsync
rsync --verbose --compress --recursive --times --delete site site@tonsky.me:
