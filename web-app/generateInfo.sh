#!/usr/bin/env bash

if ! command -v git &> /dev/null
then
    echo "git could not be found, setting variables to empty strings"
    revision=""
    tags=""
    branch=""
else
    revision=$(git rev-parse HEAD)
    tags=$(git tag --points-at HEAD)
    branch=$(git rev-parse --abbrev-ref HEAD)
fi

date=$(date +'%Y-%m-%d %H:%M:%S%z')

echo "const object = {revision: '$revision', tags: '$tags', branch: '$branch', buildTime: '$date'}; export default object;" > src/BuildInfo.ts