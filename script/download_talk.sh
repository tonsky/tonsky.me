#!/bin/bash
set -o errexit -o nounset -o pipefail
cd "`dirname $0`/../files/talks/content"

# Check if both arguments are provided
if [ "$#" -ne 2 ]; then
    echo "Usage: $0 <YouTube_URL> <Output_Name>"
    echo "Example: $0 'https://www.youtube.com/watch?v=ZJW3wAo5nxI' 'MyVideo'"
    exit 1
fi

URL="$1"
NAME="$2"

# Run yt-dlp with the specified format
yt-dlp \
    --output "${NAME}.%(ext)s" \
    --write-thumbnail \
    --convert-thumbnails webp \
    --format "bestvideo[ext=mp4][vcodec^=avc1]+bestaudio[ext=m4a]/best[ext=mp4][vcodec^=avc1]" \
    --merge-output-format mp4 \
    "$URL"

echo "Download complete! Files saved as:"
echo "- Video: ${NAME}.mp4"
echo "- Cover: ${NAME}.webp"