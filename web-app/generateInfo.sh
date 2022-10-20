#!/usr/bin/env bash

revision=$(git rev-parse HEAD)
tags=$(git tag --points-at HEAD)
branch=$(git rev-parse --abbrev-ref HEAD)
date=$(date +'%Y-%m-%d %H:%M:%S%z')

echo "const object = {revision: '$revision', tags: '$tags', branch: '$branch', buildTime: '$date'}; export default object;" > src/BuildInfo.ts