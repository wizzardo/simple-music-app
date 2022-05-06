#!/bin/sh
set -e

APP=simple-streaming-app

docker buildx build --platform linux/amd64 -t $APP -f Dockerfile.heroku .
docker tag $APP registry.heroku.com/$APP/web
docker push registry.heroku.com/$APP/web
heroku container:release  web -a $APP
heroku logs --tail -a $APP