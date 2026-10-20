#!/bin/bash
set -o errexit -o nounset -o pipefail
cd "`dirname $0`/.."

# Houston builds from `git archive`, so there's no .git here and every file
# has the extraction time as mtime. Posts' mtimes become <updated> in atom.xml,
# so restore them from git history (Houston keeps a bare clone next to data/)
export TZ=UTC
GIT_DIR="${GIT_DIR:-${DATA_DIR:-.}/../repo.git}"
REV="${RELEASE:-HEAD}"
REV="${REV##*-}" # 20261020-1830-abc1234 → abc1234

for file in $(find site/blog -name index.md); do
    time="$(git --git-dir "$GIT_DIR" log --pretty=format:%cd -n 1 --date=format-local:%Y%m%d%H%M.%S --date-order "$REV" -- "$file")"
    if [ -n "$time" ]; then
        touch -m -t "$time" "$file"
    fi
done

touch -m -t 202207261436.26 site/blog/dice-out/index.md
touch -m -t 202303092055.37 site/blog/clojure-sublimed-3/index.md
touch -m -t 202305031142.50 site/blog/humble-state/index.md
touch -m -t 202305191428.55 site/blog/humble-signals/index.md
touch -m -t 202306291434.24 site/blog/clojurescript-2/index.md

# prefetch deps
clojure -P -M:java
